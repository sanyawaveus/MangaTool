package com.example.mangalite;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drawingView = new DrawingView(this);

        // ===== КНОПКИ =====
        Button eraserBtn = new Button(this);
        eraserBtn.setText("Ластик");

        Button saveBtn = new Button(this);
        saveBtn.setText("Сохранить");

        Button undoBtn = new Button(this);
        undoBtn.setText("Undo");

        Button redoBtn = new Button(this);
        redoBtn.setText("Redo");

        Button lockBtn = new Button(this);
        lockBtn.setText("🔒");

        // ===== ОБРАБОТЧИКИ =====
        eraserBtn.setOnClickListener(v ->
                drawingView.setTool(DrawingView.Tool.ERASER));

        saveBtn.setOnClickListener(v ->
                drawingView.saveImage());

        undoBtn.setOnClickListener(v ->
                drawingView.undo());

        redoBtn.setOnClickListener(v ->
                drawingView.redo());

        lockBtn.setOnClickListener(v ->
                drawingView.toggleLock());

        // ===== LAYOUT =====
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(eraserBtn);
        layout.addView(saveBtn);
        layout.addView(undoBtn);
        layout.addView(redoBtn);
        layout.addView(lockBtn);
        layout.addView(drawingView);

        setContentView(layout);
    }
}
