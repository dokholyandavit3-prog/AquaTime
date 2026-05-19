package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import david.dokholyan.aquatime.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AnalyticsFragment extends Fragment {

    private SharedPreferences prefs;
    private TextView tvTotalDist, tvTotalTrainingsCount, tvHistory, tvPlanner;
    private EditText etGoal;
    private LinearLayout ranksContainer;
    private ProgressChartView chartDist;
    private boolean isExpanded = false;

    private final String[] styles = {"Вольный стиль 50м", "Брасс 50м", "На спине 50м", "Баттерфляй 50м", "Комплекс 100м"};
    private final String[] rankNames = {"III юн", "II юн", "I юн", "III", "II", "I", "КМС", "МС", "МСМК"};

    private final double[][] norms = {
            {55.25, 45.25, 35.25, 29.25, 27.05, 24.85, 23.40, 22.50, 21.20}, // Вольный
            {66.25, 56.25, 46.25, 37.25, 34.05, 31.85, 29.40, 28.10, 26.60}, // Брасс
            {62.25, 52.25, 42.25, 34.25, 31.05, 28.85, 26.90, 25.50, 24.00}, // Спина
            {58.25, 48.25, 38.25, 31.25, 29.05, 26.85, 25.40, 24.20, 22.80}, // Батт
            {135.0, 115.0, 102.0, 74.50, 69.50, 64.20, 59.50, 56.40, 53.60}  // Комплекс
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_analytics, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(v);
        calculateWeeklyStats();
        showRanks(0, 3);

        v.findViewById(R.id.btn_toggle_ranks).setOnClickListener(view -> toggleRanks((Button) view));
        v.findViewById(R.id.btn_add_goal).setOnClickListener(view -> addGoal());

        v.findViewById(R.id.btn_open_history).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.historyFragment));

        v.findViewById(R.id.btn_open_champions).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.championsFragment));

        return v;
    }

    private void initViews(View v) {
        tvTotalDist = v.findViewById(R.id.tv_total_distance);
        tvTotalTrainingsCount = v.findViewById(R.id.tv_total_trainings_count);
        tvHistory = v.findViewById(R.id.tv_history_summary);
        tvPlanner = v.findViewById(R.id.tv_planner_list);
        etGoal = v.findViewById(R.id.et_training_goal);
        ranksContainer = v.findViewById(R.id.ranks_container);
        chartDist = v.findViewById(R.id.chart_dist_weekly);

        tvPlanner.setText(prefs.getString("planner", "Нет запланированных целей"));
    }

    private void calculateWeeklyStats() {
        String data = prefs.getString("all_res", "");
        if (data.isEmpty()) {
            tvTotalTrainingsCount.setText("0");
            return;
        }

        String[] entries = data.split(";");


        int totalTrainingsCount = entries.length;

        float[] weeklyDist = new float[7];
        int totalWeeklyDist = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        Calendar now = Calendar.getInstance();

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                Date date = sdf.parse(p[2].trim());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                if (cal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)) {
                    int d = Integer.parseInt(p[0].trim());

                    int day = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                    weeklyDist[day] += d;

                    totalWeeklyDist += d;
                }
            } catch (Exception ignored) {}
        }

        chartDist.setData(weeklyDist, 1);

        tvTotalDist.setText(totalWeeklyDist + " м");
        // Выводим сумму всех тренировок в карточку
        tvTotalTrainingsCount.setText(String.valueOf(totalTrainingsCount));

        String lastEntry = entries[0].replace("|", " — ");
        tvHistory.setText("Последняя: " + lastEntry);
    }

    private void showRanks(int start, int end) {
        for (int i = start; i < end; i++) {
            final int styleIdx = i;
            View card = getLayoutInflater().inflate(R.layout.item_rank_card, ranksContainer, false);
            ((TextView) card.findViewById(R.id.tv_rank_style)).setText(styles[i]);

            String bestTime;
            if (i == 0) bestTime = getMinTime("best_style_0", "best_style_4");
            else if (i == 1) bestTime = getMinTime("best_style_1", "best_style_5");
            else if (i == 2) bestTime = getMinTime("best_style_2", "best_style_6");
            else if (i == 3) bestTime = getMinTime("best_style_3", "best_style_7");
            else bestTime = prefs.getString("best_style_8", "99:99:99");

            if (bestTime.equals("99:99:99")) bestTime = "00:00:00";

            ((TextView) card.findViewById(R.id.tv_rank_best)).setText("Рекорд: " + bestTime);

            Button btnMore = card.findViewById(R.id.btn_rank_more);
            LinearLayout details = card.findViewById(R.id.rank_detail_list);

            String finalBestTime = bestTime;
            btnMore.setOnClickListener(view -> {
                if (details.getVisibility() == View.GONE) {
                    details.setVisibility(View.VISIBLE);
                    btnMore.setText("Скрыть");
                    fillRankDetails(details, norms[styleIdx], parseTimeToSeconds(finalBestTime));
                } else {
                    details.setVisibility(View.GONE);
                    btnMore.setText("Подробнее");
                }
            });
            ranksContainer.addView(card);
        }
    }

    private String getMinTime(String key1, String key2) {
        String t1 = prefs.getString(key1, "99:99:99");
        String t2 = prefs.getString(key2, "99:99:99");
        return (parseTimeToSeconds(t1) < parseTimeToSeconds(t2)) ? t1 : t2;
    }

    private void fillRankDetails(LinearLayout container, double[] normArr, double userSec) {
        container.removeAllViews();
        for (int i = 0; i < normArr.length; i++) {
            TextView tv = new TextView(getContext());
            boolean isOk = (userSec > 0 && userSec <= normArr[i]);
            String status = isOk ? " ✅" : "";
            tv.setText(rankNames[i] + ": " + normArr[i] + "с" + status);
            tv.setPadding(0, 4, 0, 4);

            tv.setTextColor(isOk ? Color.parseColor("#1976D2") : Color.GRAY);
            container.addView(tv);
        }
    }

    private void toggleRanks(Button btn) {
        if (!isExpanded) {
            showRanks(3, styles.length);
            btn.setText("СКРЫТЬ");
            isExpanded = true;
        } else {
            ranksContainer.removeViews(3, ranksContainer.getChildCount() - 3);
            btn.setText("ПОКАЗАТЬ ВСЕ СТИЛИ");
            isExpanded = false;
        }
    }

    private void addGoal() {
        String goal = etGoal.getText().toString().trim();
        if (!goal.isEmpty()) {
            String current = prefs.getString("planner", "");
            String updated = "• " + goal + "\n" + current;
            prefs.edit().putString("planner", updated).apply();
            tvPlanner.setText(updated);
            etGoal.setText("");
            Toast.makeText(getContext(), "Цель добавлена", Toast.LENGTH_SHORT).show();
        }
    }

    private double parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr.equals("00:00:00") || timeStr.equals("99:99:99")) return 9999.0;
            String[] parts = timeStr.split(":");
            int min = Integer.parseInt(parts[0]);
            int sec = Integer.parseInt(parts[1]);
            int ms = Integer.parseInt(parts[2]);
            return min * 60 + sec + ms / 100.0;
        } catch (Exception e) { return 9999.0; }
    }
}