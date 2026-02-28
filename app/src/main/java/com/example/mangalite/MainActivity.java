package com.example.mangalite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 1;
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drawingView = new DrawingView(this);

        // ===== КНОПКИ =====
        Button importBtn = makeButton("Импорт");
        Button brushBtn = makeButton("Кисть");
        Button eraserBtn = makeButton("Ластик");
        Button selectBtn = makeButton("Выделение");
        Button patternBtn = makeButton("Define");
        Button fillBtn = makeButton("Заливка");
        Button layerBtn = makeButton("Слой+");
        Button undoBtn = makeButton("Undo");
        Button redoBtn = makeButton("Redo");
        Button lockBtn = makeButton("🔒");
        Button saveBtn = makeButton("Сохранить");

        // ===== ДЕЙСТВИЯ =====
        importBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        brushBtn.setOnClickListener(v ->
                drawingView.setTool(DrawingView.Tool.BRUSH));

        eraserBtn.setOnClickListener(v ->
                drawingView.setTool(DrawingView.Tool.ERASER));

        selectBtn.setOnClickListener(v ->
                drawingView.setTool(DrawingView.Tool.SELECT));

        patternBtn.setOnClickListener(v ->
                drawingView.definePattern());

        fillBtn.setOnClickListener(v ->
                drawingView.setTool(DrawingView.Tool.PATTERN_FILL));

        layerBtn.setOnClickListener(v ->
                drawingView.addLayer());

        undoBtn.setOnClickListener(v -> drawingView.undo());
        redoBtn.setOnClickListener(v -> drawingView.redo());
        lockBtn.setOnClickListener(v -> drawingView.toggleLock());
        saveBtn.setOnClickListener(v -> drawingView.saveImage());

        // ===== TOOLBAR =====
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        toolbar.addView(importBtn);
        toolbar.addView(brushBtn);
        toolbar.addView(eraserBtn);
        toolbar.addView(selectBtn);
        toolbar.addView(patternBtn);
        toolbar.addView(fillBtn);
        toolbar.addView(layerBtn);
        toolbar.addView(undoBtn);
        toolbar.addView(redoBtn);
        toolbar.addView(lockBtn);
        toolbar.addView(saveBtn);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.addView(toolbar);

        // ===== ГЛАВНЫЙ LAYOUT =====
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Toolbar фиксированной высоты
        mainLayout.addView(scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        // Canvas занимает ВСЁ остальное место
        mainLayout.addView(drawingView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1));  // <-- ВАЖНО

        setContentView(mainLayout);
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                Uri uri = data.getData();
                InputStream stream = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                drawingView.setBaseImage(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
