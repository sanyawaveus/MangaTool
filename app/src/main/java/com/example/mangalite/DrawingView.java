package com.example.mangalite;

import android.content.Context;
import android.graphics.*;
import android.os.Environment;
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

    private ArrayList<Bitmap> layers = new ArrayList<>();
    private int activeLayer = 0;
    private Canvas layerCanvas;

    private Paint paint;
    private Path path;

    private Rect selectionRect = null;
    private float startX, startY;

    private Bitmap patternBitmap = null;

    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();

    private float scaleFactor = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private boolean canvasLocked = false;
    private ScaleGestureDetector scaleDetector;

    public DrawingView(Context context) {
        super(context);

        Bitmap base = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888);
        base.eraseColor(Color.WHITE);

        layers.add(base);
        layerCanvas = new Canvas(base);

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        path = new Path();
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        for (Bitmap layer : layers) {
            canvas.drawBitmap(layer, 0, 0, null);
        }

        if (currentTool == Tool.SELECT && selectionRect != null) {
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            canvas.drawRect(selectionRect, p);
        }

        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        scaleDetector.onTouchEvent(event);

        float x = (event.getX() - offsetX) / scaleFactor;
        float y = (event.getY() - offsetY) / scaleFactor;

        // === ДВА ПАЛЬЦА = ЗУМ / ПАН ===
        if (event.getPointerCount() >= 2 && !canvasLocked) {
            return true;
        }

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

        invalidate();
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!canvasLocked) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 4f));
                invalidate();
            }
            return true;
        }
    }

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
        BitmapShader shader = new BitmapShader(
                patternBitmap,
                Shader.TileMode.REPEAT,
                Shader.TileMode.REPEAT
        );

        p.setShader(shader);

        if (selectionRect != null) {
            layerCanvas.drawRect(selectionRect, p);
        } else {
            layerCanvas.drawRect(0, 0,
                    layers.get(activeLayer).getWidth(),
                    layers.get(activeLayer).getHeight(), p);
        }
    }

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

    public void addLayer() {
        Bitmap newLayer = Bitmap.createBitmap(
                layers.get(0).getWidth(),
                layers.get(0).getHeight(),
                Bitmap.Config.ARGB_8888);
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

    public void saveImage() {
        try {
            Bitmap result = Bitmap.createBitmap(
                    layers.get(0).getWidth(),
                    layers.get(0).getHeight(),
                    Bitmap.Config.ARGB_8888);

            Canvas c = new Canvas(result);
            for (Bitmap layer : layers) {
                c.drawBitmap(layer, 0, 0, null);
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

    public void setTool(Tool tool) { currentTool = tool; }
    public void toggleLock() { canvasLocked = !canvasLocked; }
}
