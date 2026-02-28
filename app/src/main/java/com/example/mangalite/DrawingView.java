package com.example.mangalite;

import android.content.Context;
import android.graphics.*;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Stack;

public class DrawingView extends View {

    public enum Tool {
        BRUSH,
        SELECT,
        PATTERN_FILL
    }

    private Tool currentTool = Tool.BRUSH;

    private ArrayList<Bitmap> layers = new ArrayList<>();
    private int activeLayer = 0;
    private Canvas layerCanvas;

    private Paint paint;
    private Path path;

    private Rect selectionRect = null;
    private float startX, startY;

    private Bitmap patternBitmap = null;

    // ====== ZOOM & MOVE ======
    private float scaleFactor = 1.0f;
    private float minScale = 0.5f;
    private float maxScale = 4.0f;

    private float offsetX = 0f;
    private float offsetY = 0f;

    private boolean canvasLocked = false;

    private ScaleGestureDetector scaleDetector;
    private float lastTouchX, lastTouchY;

    // ====== UNDO REDO ======
    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();

    public DrawingView(Context context) {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);

        Bitmap base = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888);
        base.eraseColor(Color.WHITE);
        layers.add(base);

        layerCanvas = new Canvas(layers.get(activeLayer));

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        path = new Path();

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    // ===== DRAW =====
    @Override
    protected void onDraw(Canvas canvas) {

        canvas.save();

        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        for (Bitmap layer : layers) {
            canvas.drawBitmap(layer, 0, 0, null);
        }

        if (currentTool == Tool.SELECT && selectionRect != null) {
            Paint selectPaint = new Paint();
            selectPaint.setColor(Color.RED);
            selectPaint.setStyle(Paint.Style.STROKE);
            selectPaint.setStrokeWidth(3);
            canvas.drawRect(selectionRect, selectPaint);
        }

        canvas.drawPath(path, paint);

        canvas.restore();
    }

    // ===== TOUCH =====
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!canvasLocked) {
            scaleDetector.onTouchEvent(event);
        }

        float x = (event.getX() - offsetX) / scaleFactor;
        float y = (event.getY() - offsetY) / scaleFactor;

        if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {

            switch (currentTool) {

                case BRUSH:
                    handleBrush(event, x, y);
                    break;

                case SELECT:
                    handleSelect(event, x, y);
                    break;

                case PATTERN_FILL:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        saveState();
                        fillWithPattern();
                    }
                    break;
            }

            // Перемещение холста
            if (!canvasLocked) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    offsetX += event.getX() - lastTouchX;
                    offsetY += event.getY() - lastTouchY;
                }
            }

            lastTouchX = event.getX();
            lastTouchY = event.getY();
        }

        invalidate();
        return true;
    }

    // ===== PINCH ZOOM =====
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!canvasLocked) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));
            }
            invalidate();
            return true;
        }
    }

    // ===== BRUSH =====
    private void handleBrush(MotionEvent event, float x, float y) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                saveState();
                path.moveTo(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;

            case MotionEvent.ACTION_UP:
                layerCanvas.drawPath(path, paint);
                path.reset();
                break;
        }
    }

    // ===== SELECT =====
    private void handleSelect(MotionEvent event, float x, float y) {

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startX = x;
                startY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                selectionRect = new Rect(
                        (int) Math.min(startX, x),
                        (int) Math.min(startY, y),
                        (int) Math.max(startX, x),
                        (int) Math.max(startY, y)
                );
                break;
        }
    }

    // ===== PATTERN =====
    public void definePattern() {

        if (selectionRect == null) return;

        Bitmap source = layers.get(activeLayer);

        patternBitmap = Bitmap.createBitmap(
                source,
                selectionRect.left,
                selectionRect.top,
                selectionRect.width(),
                selectionRect.height()
        );
    }

    private void fillWithPattern() {

        if (patternBitmap == null) return;

        Paint patternPaint = new Paint();
        BitmapShader shader = new BitmapShader(
                patternBitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
        );

        patternPaint.setShader(shader);

        layerCanvas.drawRect(
                0,
                0,
                layers.get(activeLayer).getWidth(),
                layers.get(activeLayer).getHeight(),
                patternPaint
        );
    }

    // ===== UNDO REDO =====
    private void saveState() {
        Bitmap current = layers.get(activeLayer);
        undoStack.push(current.copy(Bitmap.Config.ARGB_8888, true));
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Bitmap current = layers.get(activeLayer);
            redoStack.push(current.copy(Bitmap.Config.ARGB_8888, true));

            Bitmap previous = undoStack.pop();
            layers.set(activeLayer, previous);
            layerCanvas = new Canvas(previous);
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Bitmap current = layers.get(activeLayer);
            undoStack.push(current.copy(Bitmap.Config.ARGB_8888, true));

            Bitmap next = redoStack.pop();
            layers.set(activeLayer, next);
            layerCanvas = new Canvas(next);
            invalidate();
        }
    }

    // ===== LOCK =====
    public void toggleCanvasLock() {
        canvasLocked = !canvasLocked;
    }

    // ===== PUBLIC =====
    public void setTool(Tool tool) {
        currentTool = tool;
    }

    public void addLayer() {
        Bitmap newLayer = Bitmap.createBitmap(
                layers.get(0).getWidth(),
                layers.get(0).getHeight(),
                Bitmap.Config.ARGB_8888
        );
        layers.add(newLayer);
        activeLayer = layers.size() - 1;
        layerCanvas = new Canvas(newLayer);
    }

    public void setBaseImage(Bitmap bitmap) {
        layers.clear();
        Bitmap base = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        layers.add(base);
        activeLayer = 0;
        layerCanvas = new Canvas(base);
        invalidate();
    }

    // ===== KEYBOARD =====
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {

            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_EQUALS:
                scaleFactor *= 1.1f;
                break;

            case KeyEvent.KEYCODE_MINUS:
                scaleFactor *= 0.9f;
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                offsetY += 50;
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                offsetY -= 50;
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                offsetX += 50;
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                offsetX -= 50;
                break;

            case KeyEvent.KEYCODE_Z:
                if (event.isCtrlPressed()) undo();
                break;

            case KeyEvent.KEYCODE_Y:
                if (event.isCtrlPressed()) redo();
                break;

            default:
                return super.onKeyDown(keyCode, event);
        }

        invalidate();
        return true;
    }
}
