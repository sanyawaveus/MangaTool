package com.example.mangalite;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
        Button importBtn = new Button(this);
        importBtn.setText("Импорт");

        Button brushBtn = new Button(this);
        brushBtn.setText("Кисть");

        Button eraserBtn = new Button(this);
        eraserBtn.setText("Ластик");

        Button selectBtn = new Button(this);
        selectBtn.setText("Выделение");

        Button patternBtn = new Button(this);
        patternBtn.setText("Define Pattern");

        Button fillBtn = new Button(this);
        fillBtn.setText("Заливка");

        Button layerBtn = new Button(this);
        layerBtn.setText("Новый слой");

        Button undoBtn = new Button(this);
        undoBtn.setText("Undo");

        Button redoBtn = new Button(this);
        redoBtn.setText("Redo");

        Button lockBtn = new Button(this);
        lockBtn.setText("🔒");

        Button saveBtn = new Button(this);
        saveBtn.setText("Сохранить");

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

        undoBtn.setOnClickListener(v ->
                drawingView.undo());

        redoBtn.setOnClickListener(v ->
                drawingView.redo());

        lockBtn.setOnClickListener(v ->
                drawingView.toggleLock());

        saveBtn.setOnClickListener(v ->
                drawingView.saveImage());

        // ===== ВЕРХНЯЯ ПАНЕЛЬ =====

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);

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

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        mainLayout.addView(scroll);
        mainLayout.addView(drawingView);

        setContentView(mainLayout);
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
