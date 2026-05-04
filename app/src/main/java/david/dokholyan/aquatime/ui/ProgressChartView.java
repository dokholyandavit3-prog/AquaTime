package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class ProgressChartView extends View {
    private float[] data = new float[0];
    private Paint linePaint, barPaint;
    private int mode = 0;

    public ProgressChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#009688"));
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#80009688"));
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(float[] newData, int chartMode) {
        this.data = newData;
        this.mode = chartMode;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.length == 0) return;

        float width = getWidth();
        float height = getHeight();
        float max = getMaxValue(data);
        float stepX = width / (data.length > 1 ? data.length - 1 : 1);
        if (mode == 1) stepX = width / data.length;

        if (mode == 0) {
            Path path = new Path();
            for (int i = 0; i < data.length; i++) {
                float x = i * stepX;
                float y = height - (data[i] / max * height * 0.8f) - 20;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            canvas.drawPath(path, linePaint);
        } else {
            for (int i = 0; i < data.length; i++) {
                float barW = stepX * 0.6f;
                float x = i * stepX + (stepX - barW) / 2;
                float y = height - (data[i] / max * height * 0.8f) - 20;
                canvas.drawRect(x, y, x + barW, height, barPaint);
            }
        }
    }

    private float getMaxValue(float[] array) {
        float max = 1;
        for (float f : array) if (f > max) max = f;
        return max;
    }
}