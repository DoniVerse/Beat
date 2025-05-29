package com.example.beat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class CircularProfileView extends View {
    private Paint backgroundPaint;
    private Paint textPaint;
    private String letter = "?";
    private int size = 0;

    public CircularProfileView(Context context) {
        super(context);
        init();
    }

    public CircularProfileView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(getResources().getColor(R.color.accent));
        backgroundPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24); // Default text size
    }

    public void setLetter(String name) {
        if (name != null && !name.isEmpty()) {
            this.letter = name.substring(0, 1).toUpperCase();
            textPaint.setTextSize(getWidth() * 0.4f); // Adjust text size based on view size
        } else {
            this.letter = "?";
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw circle
        float radius = getWidth() / 2f;
        canvas.drawCircle(radius, radius, radius, backgroundPaint);

        // Draw text
        if (!letter.equals("?")) {
            textPaint.setTextSize(getWidth() * 0.4f);
        }
        Rect textBounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), textBounds);
        float textHeight = textBounds.height();
        canvas.drawText(letter, radius, radius + (textHeight / 2), textPaint);
    }
}