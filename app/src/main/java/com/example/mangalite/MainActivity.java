package com.example.mangalite;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DrawingView drawingView = new DrawingView(this);
        setContentView(drawingView);
        protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    drawingView = new DrawingView(this);

    // кнопки
    Button eraserBtn = new Button(this);
    Button saveBtn = new Button(this);
    Button undoBtn = new Button(this);
    Button redoBtn = new Button(this);
    Button lockBtn = new Button(this);

    eraserBtn.setText("Ластик");
    saveBtn.setText("Сохранить");
    undoBtn.setText("Undo");
    redoBtn.setText("Redo");
    lockBtn.setText("🔒");

    // обработчики
    eraserBtn.setOnClickListener(v -> drawingView.setTool(DrawingView.Tool.ERASER));
    saveBtn.setOnClickListener(v -> drawingView.saveImage());
    undoBtn.setOnClickListener(v -> drawingView.undo());
    redoBtn.setOnClickListener(v -> drawingView.redo());
    lockBtn.setOnClickListener(v -> drawingView.toggleLock());

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
}
