package com.example.mangalite;

import android.content.Context;
import android.graphics.*;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Stack;

public class DrawingView extends View {

    public enum Tool {
        BRUSH,
        ERASER,
        SELECT,
        PATTERN_FILL
    }

    private Tool currentTool = Tool.BRUSH;

    // ===== Layers =====
    private ArrayList<Bitmap> layers = new ArrayList<>();
    private ArrayList<Boolean> layerVisibility = new ArrayList<>();
    private int activeLayer = 0;
    private Canvas layerCanvas;

    // ===== Drawing =====
    private Paint paint;
    private Path path;

    // ===== Selection =====
    private Rect selectionRect = null;
    private float startX, startY;

    // ===== Pattern =====
    private Bitmap patternBitmap = null;

    // ===== Undo / Redo =====
    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();

    // ===== Zoom & Pan =====
    private float scaleFactor = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private boolean canvasLocked = false;
    private ScaleGestureDetector scaleDetector;

    public DrawingView(Context context) {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);

        Bitmap base = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888);
        base.eraseColor(Color.WHITE);

        layers.add(base);
        layerVisibility.add(true);
        layerCanvas = new Canvas(base);

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

        for (int i = 0; i < layers.size(); i++) {
            if (layerVisibility.get(i)) {
                canvas.drawBitmap(layers.get(i), 0, 0, null);
            }
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

        if (event.getPointerCount() == 1) {

            switch (currentTool) {

                case BRUSH:
                case ERASER:
                    handleBrush(event, x, y);
                    break;

                case SELECT:
                    handleSelect(event, x, y);
                    break;

                case PATTERN_FILL:
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        saveState();
                        fillPattern();
                    }
                    break;
            }
        }

        invalidate();
        return true;
    }

    // ===== PINCH =====
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!canvasLocked) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 4f));
            }
            invalidate();
            return true;
        }
    }

    // ===== BRUSH / ERASER =====
    private void handleBrush(MotionEvent event, float x, float y) {

        if (currentTool == Tool.ERASER) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            paint.setXfermode(null);
        }

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

        Bitmap src = layers.get(activeLayer);

        patternBitmap = Bitmap.createBitmap(
                src,
                selectionRect.left,
                selectionRect.top,
                selectionRect.width(),
                selectionRect.height()
        );
    }

    private void fillPattern() {

        if (patternBitmap == null) return;

        Paint p = new Paint();
        BitmapShader shader = new BitmapShader(patternBitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT);

        p.setShader(shader);

        if (selectionRect != null) {
            layerCanvas.drawRect(selectionRect, p);
        } else {
            layerCanvas.drawRect(
                    0,
                    0,
                    layers.get(activeLayer).getWidth(),
                    layers.get(activeLayer).getHeight(),
                    p
            );
        }
    }

    // ===== UNDO REDO =====
    private void saveState() {
        undoStack.push(layers.get(activeLayer)
                .copy(Bitmap.Config.ARGB_8888, true));
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(layers.get(activeLayer)
                    .copy(Bitmap.Config.ARGB_8888, true));
            Bitmap prev = undoStack.pop();
            layers.set(activeLayer, prev);
            layerCanvas = new Canvas(prev);
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(layers.get(activeLayer)
                    .copy(Bitmap.Config.ARGB_8888, true));
            Bitmap next = redoStack.pop();
            layers.set(activeLayer, next);
            layerCanvas = new Canvas(next);
            invalidate();
        }
    }

    // ===== LAYERS =====
    public void addLayer() {
        Bitmap newLayer = Bitmap.createBitmap(
                layers.get(0).getWidth(),
                layers.get(0).getHeight(),
                Bitmap.Config.ARGB_8888);
        layers.add(newLayer);
        layerVisibility.add(true);
        activeLayer = layers.size() - 1;
        layerCanvas = new Canvas(newLayer);
    }

    public void toggleLayerVisibility(int index) {
        if (index < layerVisibility.size()) {
            layerVisibility.set(index,
                    !layerVisibility.get(index));
            invalidate();
        }
    }

    // ===== SAVE PNG =====
    public void saveImage() {
        try {
            Bitmap result = Bitmap.createBitmap(
                    layers.get(0).getWidth(),
                    layers.get(0).getHeight(),
                    Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(result);

            for (int i = 0; i < layers.size(); i++) {
                if (layerVisibility.get(i)) {
                    c.drawBitmap(layers.get(i), 0, 0, null);
                }
            }

            File file = new File(
                    Environment.getExternalStorageDirectory(),
                    "manga_edit.png");

            FileOutputStream out = new FileOutputStream(file);
            result.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== PUBLIC =====
    public void setTool(Tool tool) { currentTool = tool; }
    public void toggleLock() { canvasLocked = !canvasLocked; }

}
