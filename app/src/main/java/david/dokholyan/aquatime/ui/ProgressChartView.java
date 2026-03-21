package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ProgressChartView extends View {

    private final Paint linePaint = new Paint();
    private final Paint pointPaint = new Paint();
    private final Paint textPaint = new Paint();

    // Тестовые данные (дистанции за 7 дней)
    private final float[] distances = {800, 1000, 1200, 1100, 1400, 1500, 1600};
    private final String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    public ProgressChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#00AEEF"));
        linePaint.setStrokeWidth(6f);
        linePaint.setAntiAlias(true);

        pointPaint.setColor(Color.parseColor("#0093E9"));
        pointPaint.setAntiAlias(true);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        float padding = 80f;
        float maxDistance = getMaxDistance();
        float stepX = (width - 2 * padding) / (distances.length - 1);

        float prevX = 0, prevY = 0;

        for (int i = 0; i < distances.length; i++) {
            float x = padding + i * stepX;
            float y = height - (distances[i] / maxDistance) * (height - 2 * padding);

            // Рисуем линию
            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, linePaint);
            }

            // Точка
            canvas.drawCircle(x, y, 10, pointPaint);

            // Подпись под графиком
            canvas.drawText(days[i], x - 20, height - 20, textPaint);

            prevX = x;
            prevY = y;
        }
    }

    private float getMaxDistance() {
        float max = 0;
        for (float d : distances) if (d > max) max = d;
        return max;
    }
}
