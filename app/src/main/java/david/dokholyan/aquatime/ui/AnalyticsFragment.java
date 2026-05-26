package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
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
import androidx.core.content.ContextCompat;
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
    private TextView tvArchivePreview, tvStopwatchSummary;
    private EditText etGoal;
    private LinearLayout ranksContainer;
    private ProgressChartView chartDist;
    private boolean isExpanded = false;

    // Ссылки на текстовые метки дней недели для динамической локализации
    private TextView tvDayMon, tvDayTue, tvDayWed, tvDayThu, tvDayFri, tvDaySat, tvDaySun;

    private final String[] styles = {"Вольный стиль 50м", "Брасс 50м", "На спине 50м", "Баттерфляй 50м", "Комплекс 100м"};
    private final double[][] norms = {
            {55.25, 45.25, 35.25, 29.25, 27.05, 24.85, 23.40, 22.50, 21.20},
            {66.25, 56.25, 46.25, 37.25, 34.05, 31.85, 29.40, 28.10, 26.60},
            {62.25, 52.25, 42.25, 34.25, 31.05, 28.85, 26.90, 25.50, 24.00},
            {58.25, 48.25, 38.25, 31.25, 29.05, 26.85, 25.40, 24.20, 22.80},
            {135.0, 115.0, 102.0, 74.50, 69.50, 64.20, 59.50, 56.40, 53.60}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_analytics, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(v);
        calculateWeeklyAndArchiveStats();
        updateStopwatchPreview();
        showRanks(0, 3);
        fetchFreshDataFromFirebase();

        updateStaticButtonsLocalization(v);
        updateChartDaysLocalization();

        v.findViewById(R.id.btn_toggle_ranks).setOnClickListener(view -> toggleRanks((Button) view));
        v.findViewById(R.id.btn_add_goal).setOnClickListener(view -> addGoal());

        v.findViewById(R.id.btn_open_history).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.historyFragment));

        v.findViewById(R.id.btn_open_weeks_archive).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.weeksArchiveFragment));

        v.findViewById(R.id.btn_open_stopwatch_history).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.stopwatchHistoryFragment));

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

        tvArchivePreview = v.findViewById(R.id.tv_archive_preview);
        tvStopwatchSummary = v.findViewById(R.id.tv_stopwatch_summary);


        tvDayMon = v.findViewById(R.id.tv_day_mon);
        tvDayTue = v.findViewById(R.id.tv_day_tue);
        tvDayWed = v.findViewById(R.id.tv_day_wed);
        tvDayThu = v.findViewById(R.id.tv_day_thu);
        tvDayFri = v.findViewById(R.id.tv_day_fri);
        tvDaySat = v.findViewById(R.id.tv_day_sat);
        tvDaySun = v.findViewById(R.id.tv_day_sun);

        TextView tvStopwatchTitle = v.findViewById(R.id.tv_stopwatch_history_title);
        if (tvStopwatchTitle != null) {
            tvStopwatchTitle.setText(isEnglish() ? "Stopwatch History" : "История секундомера");
        }

        if (tvPlanner != null) {
            tvPlanner.setText(prefs.getString("planner", getString(R.string.planner_empty)));
        }
    }

    private boolean isEnglish() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("en");
    }

    private void updateStaticButtonsLocalization(View v) {
        TextView btnArchive = v.findViewById(R.id.btn_open_weeks_archive);
        TextView btnHistory = v.findViewById(R.id.btn_open_history);
        TextView btnStopwatch = v.findViewById(R.id.btn_open_stopwatch_history);

        boolean en = isEnglish();
        if (btnArchive != null) btnArchive.setText(en ? "VIEW ARCHIVE" : "ВЕСЬ АРХИВ");
        if (btnHistory != null) btnHistory.setText(en ? "VIEW HISTORY" : "ВСЯ ИСТОРИЯ");
        if (btnStopwatch != null) btnStopwatch.setText(en ? "VIEW HISTORY" : "ВСЯ ИСТОРИЯ");
    }

    // Метод динамического изменения формата подписей дней недели под графиком
    private void updateChartDaysLocalization() {
        boolean en = isEnglish();
        if (tvDayMon != null) tvDayMon.setText(en ? "M\n(Mo)" : "M\n(Пн)");
        if (tvDayTue != null) tvDayTue.setText(en ? "T\n(Tu)" : "T\n(Вт)");
        if (tvDayWed != null) tvDayWed.setText(en ? "W\n(We)" : "W\n(Ср)");
        if (tvDayThu != null) tvDayThu.setText(en ? "T\n(Th)" : "T\n(Чт)");
        if (tvDayFri != null) tvDayFri.setText(en ? "F\n(Fr)" : "F\n(Пт)");
        if (tvDaySat != null) tvDaySat.setText(en ? "S\n(Sa)" : "S\n(Сб)");
        if (tvDaySun != null) tvDaySun.setText(en ? "S\n(Su)" : "S\n(Вс)");
    }

    private void updateStopwatchPreview() {
        if (tvStopwatchSummary == null) return;
        String rawLog = prefs.getString("stopwatch_log", "");
        if (rawLog.isEmpty()) {
            tvStopwatchSummary.setText(isEnglish() ? "No records yet." : "Нет сохранённых замеров.");
        } else {
            String[] logs = rawLog.split(";");
            String[] pieces = logs[0].split("\\|");
            if (pieces.length >= 2) {
                String styleAndDist = pieces[0];

                if (isEnglish()) {
                    styleAndDist = styleAndDist.replace("Брасс", "Breaststroke")
                            .replace("Вольный стиль", "Freestyle")
                            .replace("На спине", "Backstroke")
                            .replace("Баттерфляй", "Butterfly")
                            .replace("Комплекс", "Medley");
                }
                tvStopwatchSummary.setText((isEnglish() ? "Latest: " : "Последний замер: ") + styleAndDist + " — " + pieces[1]);
            }
        }
    }

    private void fetchFreshDataFromFirebase() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            com.google.firebase.database.DataSnapshot snapshot = task.getResult();

                            String allRes = snapshot.child("all_res").getValue(String.class);
                            Long totalMeters = snapshot.child("total_meters").getValue(Long.class);
                            Long trainingsCount = snapshot.child("trainings_count").getValue(Long.class);
                            Long weeklyMeters = snapshot.child("weekly_meters").getValue(Long.class);
                            String cloudPlanner = snapshot.child("planner").getValue(String.class);
                            String stopwatchLog = snapshot.child("stopwatch_log").getValue(String.class);

                            SharedPreferences.Editor editor = prefs.edit();
                            if (allRes != null) editor.putString("all_res", allRes);
                            if (totalMeters != null) editor.putInt("total_meters", totalMeters.intValue());
                            if (trainingsCount != null) editor.putInt("trainings_count", trainingsCount.intValue());
                            if (weeklyMeters != null) editor.putInt("weekly_meters", weeklyMeters.intValue());
                            if (cloudPlanner != null) editor.putString("planner", cloudPlanner);
                            if (stopwatchLog != null) editor.putString("stopwatch_log", stopwatchLog);
                            editor.apply();

                            if (isAdded() && getContext() != null) {
                                calculateWeeklyAndArchiveStats();
                                updateStopwatchPreview();
                                updateChartDaysLocalization();
                                if (tvPlanner != null) {
                                    tvPlanner.setText(prefs.getString("planner", getString(R.string.planner_empty)));
                                }
                                ranksContainer.removeAllViews();
                                showRanks(0, isExpanded ? styles.length : 3);
                            }
                        }
                    });
        }
    }

    private void calculateWeeklyAndArchiveStats() {
        String data = prefs.getString("all_res", "");
        if (data.isEmpty()) {
            tvTotalTrainingsCount.setText("0");
            tvTotalDist.setText(getString(R.string.meters_format, 0));
            if (tvArchivePreview != null) tvArchivePreview.setText(getString(R.string.empty_archive_msg));
            return;
        }

        String[] entries = data.split(";");
        int totalTrainingsCount = entries.length;

        float[] weeklyDist = new float[7];
        int totalWeeklyDist = 0;
        int archiveTotalWeeks = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(Calendar.YEAR);

        int[] archiveWeeksMeters = new int[55];

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                Date date = sdf.parse(p[2].trim());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                int d = Integer.parseInt(p[0].trim());
                int wYear = cal.get(Calendar.YEAR);
                int wWeek = cal.get(Calendar.WEEK_OF_YEAR);

                if (wWeek == currentWeek && wYear == currentYear) {
                    int day = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                    weeklyDist[day] += d;
                    totalWeeklyDist += d;
                } else if (wYear == currentYear && wWeek < currentWeek) {
                    if (archiveWeeksMeters[wWeek] == 0) archiveTotalWeeks++;
                    archiveWeeksMeters[wWeek] += d;
                }
            } catch (Exception ignored) {}
        }

        chartDist.setData(weeklyDist, 1);
        tvTotalDist.setText(getString(R.string.meters_format, totalWeeklyDist));
        tvTotalTrainingsCount.setText(String.valueOf(totalTrainingsCount));

        if (entries.length > 0) {
            try {
                String[] parts = entries[0].split("\\|");
                String prefix = isEnglish() ? "Last: " : getString(R.string.last_workout_prefix);
                String metersUnit = isEnglish() ? "m" : "м";
                tvHistory.setText(prefix + parts[0].trim() + metersUnit + " (" + parts[2].trim() + ")");
            } catch (Exception e) {
                String prefix = isEnglish() ? "Last: " : getString(R.string.last_workout_prefix);
                tvHistory.setText(prefix + entries[0].replace("|", " — "));
            }
        }

        if (tvArchivePreview != null) {
            tvArchivePreview.setText(isEnglish() ? "Archived weeks available: " + archiveTotalWeeks : "Доступно недель в архиве: " + archiveTotalWeeks);
        }
    }

    private String getTrainingWordForm(int count) {
        if (count % 10 == 1 && count % 100 != 11) {
            return getString(R.string.word_training_1);
        } else if ((count % 10 >= 2 && count % 10 <= 4) && (count % 100 < 10 || count % 100 >= 20)) {
            return getString(R.string.word_training_2);
        } else {
            return getString(R.string.word_training_3);
        }
    }

    private void showRanks(int start, int end) {
        if (getContext() == null) return;
        String[] localizedStyles = getResources().getStringArray(R.array.styles_array);

        for (int i = start; i < end; i++) {
            final int styleIdx = i;
            View card = getLayoutInflater().inflate(R.layout.item_rank_card, ranksContainer, false);
            ((TextView) card.findViewById(R.id.tv_rank_style)).setText(localizedStyles[i]);

            String bestTime;
            if (i == 0) bestTime = getMinTime("best_style_0", "best_style_4");
            else if (i == 1) bestTime = getMinTime("best_style_1", "best_style_5");
            else if (i == 2) bestTime = getMinTime("best_style_2", "best_style_6");
            else if (i == 3) bestTime = getMinTime("best_style_3", "best_style_7");
            else bestTime = prefs.getString("best_style_8", "99:99:99");

            if (bestTime.equals("99:99:99")) bestTime = "00:00:00";

            ((TextView) card.findViewById(R.id.tv_rank_best)).setText(getString(R.string.rank_best_label, bestTime));

            Button btnMore = card.findViewById(R.id.btn_rank_more);
            LinearLayout details = card.findViewById(R.id.rank_detail_list);

            String finalBestTime = bestTime;
            btnMore.setOnClickListener(view -> {
                if (details.getVisibility() == View.GONE) {
                    details.setVisibility(View.VISIBLE);
                    btnMore.setText(getString(R.string.btn_hide));
                    fillRankDetails(details, norms[styleIdx], parseTimeToSeconds(finalBestTime));
                } else {
                    details.setVisibility(View.GONE);
                    btnMore.setText(getString(R.string.btn_details));
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
        if (getContext() == null) return;
        container.removeAllViews();
        String[] localizedRanks = getResources().getStringArray(R.array.ranks_array);

        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        int secondaryTextColor = ContextCompat.getColor(getContext(), typedValue.resourceId != 0 ? typedValue.resourceId : android.R.color.darker_gray);

        int activeAquaColor = ContextCompat.getColor(getContext(), R.color.aqua_primary);

        for (int i = 0; i < normArr.length; i++) {
            TextView tv = new TextView(getContext());
            boolean isOk = (userSec > 0 && userSec <= normArr[i]);
            String status = isOk ? " ✅" : "";
            tv.setText(localizedRanks[i] + ": " + normArr[i] + getString(R.string.rank_status_sec) + status);
            tv.setPadding(0, 4, 0, 4);

            tv.setTextColor(isOk ? activeAquaColor : secondaryTextColor);
            container.addView(tv);
        }
    }

    private void toggleRanks(Button btn) {
        if (!isExpanded) {
            showRanks(3, styles.length);
            btn.setText(getString(R.string.btn_hide).toUpperCase());
            isExpanded = true;
        } else {
            ranksContainer.removeViews(3, ranksContainer.getChildCount() - 3);
            btn.setText(getString(R.string.btn_show_all_styles));
            isExpanded = false;
        }
    }

    private void addGoal() {
        String goal = etGoal.getText().toString().trim();
        if (!goal.isEmpty()) {
            String current = prefs.getString("planner", "").trim();
            String updated = (current.isEmpty() || current.equals(getString(R.string.planner_empty))) ? "• " + goal : "• " + goal + "\n" + current;

            prefs.edit().putString("planner", updated).apply();
            tvPlanner.setText(updated);
            etGoal.setText("");
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.goal_added_msg), Toast.LENGTH_SHORT).show();
            }

            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                        .child(user.getUid()).child("planner").setValue(updated);
            }
        }
    }

    private double parseTimeToSeconds(String timeStr) {
        try {
            if (timeStr.equals("00:00:00") || timeStr.equals("99:99:99")) return 9999.0;
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]) + Integer.parseInt(parts[2]) / 100.0;
        } catch (Exception e) { return 9999.0; }
    }
}
