package david.dokholyan.aquatime.ui.training;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import david.dokholyan.aquatime.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

interface OnResultClickListener { void onItemClick(int position); }

public class TrainingFragment extends Fragment {

    private TextView tvTimer, tvTodayTraining, tvStatsCount, tvSwimmingTip;
    private Spinner spinnerDistance;
    private EditText etManualDist;
    private RecyclerView rvResults;
    private Button btnStartTimer, btnStopTimer, btnResetTimer, btnSaveMeasure, btnSaveManual, btnStartTraining, btnOpenCalendar, btnDeleteMeasure;

    private Handler handler = new Handler();
    private long startTime = 0L, updateTime = 0L, timeSwapBuff = 0L, timeInMilliseconds = 0L;
    private boolean running = false;

    private ArrayList<String> allResults = new ArrayList<>();
    private ResultsAdapter resultsAdapter;
    private SharedPreferences prefs;
    private int selectedPosition = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);
        prefs = getActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(view);
        setupRecyclerViews();
        setupDistances();
        loadData();
        showRandomTip();

        // Таймер
        btnStartTimer.setOnClickListener(v -> startTimer());
        btnStopTimer.setOnClickListener(v -> stopTimer());
        btnResetTimer.setOnClickListener(v -> resetTimer());

        // Замеры (для нормативов)
        btnSaveMeasure.setOnClickListener(v -> saveNewMeasure());

        // Твой Конструктор (добавление в план на сегодня)
        btnSaveManual.setOnClickListener(v -> saveManualTrainingToPlan());

        // Робот и Календарь
        btnStartTraining.setOnClickListener(v -> getAIPlan());
        btnOpenCalendar.setOnClickListener(v -> showDateTimePicker());

        // Удаление
        btnDeleteMeasure.setOnClickListener(v -> deleteSelectedResult());

        return view;
    }

    private void initViews(View v) {
        tvTimer = v.findViewById(R.id.tv_timer);
        tvTodayTraining = v.findViewById(R.id.tv_today_training);
        tvStatsCount = v.findViewById(R.id.tv_stats_count);
        tvSwimmingTip = v.findViewById(R.id.tv_swimming_tip);
        spinnerDistance = v.findViewById(R.id.spinner_distance);
        etManualDist = v.findViewById(R.id.et_manual_dist);
        rvResults = v.findViewById(R.id.rv_results);

        btnStartTimer = v.findViewById(R.id.btn_start_timer);
        btnStopTimer = v.findViewById(R.id.btn_stop_timer);
        btnResetTimer = v.findViewById(R.id.btn_reset_timer);
        btnSaveMeasure = v.findViewById(R.id.btn_save_measure);
        btnSaveManual = v.findViewById(R.id.btn_save_manual_training);
        btnStartTraining = v.findViewById(R.id.btn_start_training);
        btnOpenCalendar = v.findViewById(R.id.btn_open_calendar);
        btnDeleteMeasure = v.findViewById(R.id.btn_delete_measure);
    }



    private void saveManualTrainingToPlan() {
        String exercise = etManualDist.getText().toString().trim();
        if (exercise.isEmpty()) {
            Toast.makeText(getContext(), "Напишите упражнение", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentPlan = tvTodayTraining.getText().toString();

        if (currentPlan.contains("Нажми на робота")) currentPlan = "";

        String updatedPlan = currentPlan + (currentPlan.isEmpty() ? "" : "\n") + "• " + exercise;
        tvTodayTraining.setText(updatedPlan);
        tvTodayTraining.setTextColor(Color.parseColor("#102A43"));

        etManualDist.setText("");
        Toast.makeText(getContext(), "Добавлено в план", Toast.LENGTH_SHORT).show();
    }

    private void saveNewMeasure() {
        String time = tvTimer.getText().toString();
        if (time.equals("00:00:00")) return;

        int styleIdx = spinnerDistance.getSelectedItemPosition();
        updateBestTime(styleIdx, time);

        String date = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(new Date());
        String entry = spinnerDistance.getSelectedItem().toString() + " | " + time + " (" + date + ")";

        allResults.add(0, entry);
        saveData();
        resultsAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Результат сохранен!", Toast.LENGTH_SHORT).show();
    }

    private void updateBestTime(int styleIdx, String newTime) {
        String key = "best_style_" + styleIdx;
        String current = prefs.getString(key, "99:99:99");
        if (parseToSec(newTime) < parseToSec(current)) {
            prefs.edit().putString(key, newTime).apply();
        }
    }

    private double parseToSec(String t) {
        try {
            String[] p = t.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]) + Integer.parseInt(p[2]) / 100.0;
        } catch (Exception e) { return 9999; }
    }

    // --- СЕКУНДОМЕР ---

    private void startTimer() {
        if (!running) {
            startTime = SystemClock.uptimeMillis();
            handler.postDelayed(timerRunnable, 0);
            running = true;
        }
    }
    private void stopTimer() {
        if (running) {
            timeSwapBuff += timeInMilliseconds;
            handler.removeCallbacks(timerRunnable);
            running = false;
        }
    }
    private void resetTimer() {
        stopTimer();
        startTime = 0L; updateTime = 0L; timeSwapBuff = 0L; timeInMilliseconds = 0L;
        tvTimer.setText("00:00:00");
    }
    private Runnable timerRunnable = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updateTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updateTime / 1000);
            int mins = secs / 60; secs %= 60;
            int ms = (int) (updateTime % 1000) / 10;
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", mins, secs, ms));
            handler.postDelayed(this, 30);
        }
    };



    private void showRandomTip() {
        String[] tips = {
                "Совет: Не забывайте делать разминку на суше перед прыжком в воду!",
                "Совет: Держите голову ниже для лучшей обтекаемости.",
                "Совет: Высокий локоть при гребке экономит силы.",
                "Совет: Сильный удар ногами идет от бедра, а не от колена."
        };
        tvSwimmingTip.setText(tips[new Random().nextInt(tips.length)]);
    }

    private void getAIPlan() {
        tvTodayTraining.setText("• 400м разминка кролем\n• 8x50м ускорение (отдых 30с)\n• 200м заминка");
        tvTodayTraining.setTextColor(Color.parseColor("#102A43"));
    }

    private void showDateTimePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(), (v, y, m, d) -> {
            new TimePickerDialog(getContext(), (v2, hh, mm) -> {
                Toast.makeText(getContext(), "Тренировка запланирована!", Toast.LENGTH_SHORT).show();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void deleteSelectedResult() {
        if (selectedPosition != -1 && selectedPosition < allResults.size()) {
            allResults.remove(selectedPosition);
            saveData();
            resultsAdapter.notifyDataSetChanged();
            selectedPosition = -1;
            Toast.makeText(getContext(), "Удалено", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Выберите заплыв для удаления", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDistances() {
        String[] list = {"Кроль 50м", "Брасс 50м", "Спина 50м", "Батт 50м","Кроль 100м", "Брасс 100м", "Спина 100м", "Батт 100м","Комплекс 100м"};
        spinnerDistance.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, list));
    }

    private void saveData() {
        prefs.edit().putString("measure_history_full", String.join(";", allResults)).apply();
        if (tvStatsCount != null) tvStatsCount.setText("Всего заплывов: " + allResults.size());
    }

    private void loadData() {
        String data = prefs.getString("measure_history_full", "");
        if (!data.isEmpty()) {
            allResults.clear();
            allResults.addAll(Arrays.asList(data.split(";")));
        }
        if (tvStatsCount != null) tvStatsCount.setText("Всего заплывов: " + allResults.size());
        resultsAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerViews() {
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        resultsAdapter = new ResultsAdapter(allResults, pos -> {
            selectedPosition = pos;
            resultsAdapter.notifyDataSetChanged();
        });
        rvResults.setAdapter(resultsAdapter);
    }


    private class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {
        private final ArrayList<String> mData;
        private final OnResultClickListener listener;

        ResultsAdapter(ArrayList<String> data, OnResultClickListener listener) {
            this.mData = data;
            this.listener = listener;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            h.textView.setText(mData.get(pos));
            h.textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            h.itemView.setBackgroundColor(pos == selectedPosition ? Color.parseColor("#E3F2FD") : Color.TRANSPARENT);
            h.itemView.setOnClickListener(v -> listener.onItemClick(pos));
        }

        @Override public int getItemCount() { return mData.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View v) { super(v); textView = v.findViewById(android.R.id.text1); }
        }
    }
}