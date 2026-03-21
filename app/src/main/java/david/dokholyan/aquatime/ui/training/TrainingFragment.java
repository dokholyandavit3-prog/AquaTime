package david.dokholyan.aquatime.ui.training;

import david.dokholyan.aquatime.R;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class TrainingFragment extends Fragment {

    TextView tvTimer, tvTipText, tvTodayTraining, tvWeekProgress;
    Spinner spinnerMood, spinnerDistance;
    ListView listResults, listBestResults;
    CalendarView calendarTraining;
    Button btnStartTimer, btnStopTimer, btnResetTimer, btnSaveMeasure, btnDeleteMeasure, btnStartTraining, btnTrainingHistory;

    Handler handler = new Handler();
    long startTime = 0;
    boolean running = false;

    ArrayList<String> results = new ArrayList<>();
    ArrayList<String> bestResults = new ArrayList<>();

    ArrayAdapter<String> resultsAdapter;
    ArrayAdapter<String> bestAdapter;
    SharedPreferences prefs;

    int weeklyGoal = 3000;
    int weeklyVolume = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_training, container, false);

        tvTimer = view.findViewById(R.id.tv_timer);
        tvTipText = view.findViewById(R.id.tv_tip_text);
        tvTodayTraining = view.findViewById(R.id.tv_today_training);
        tvWeekProgress = view.findViewById(R.id.tv_week_progress);

        spinnerMood = view.findViewById(R.id.spinner_mood);
        spinnerDistance = view.findViewById(R.id.spinner_distance);

        listResults = view.findViewById(R.id.list_results);
        listBestResults = view.findViewById(R.id.list_best_results);

        calendarTraining = view.findViewById(R.id.calendar_training);

        btnStartTimer = view.findViewById(R.id.btn_start_timer);
        btnStopTimer = view.findViewById(R.id.btn_stop_timer);
        btnResetTimer = view.findViewById(R.id.btn_reset_timer);
        btnSaveMeasure = view.findViewById(R.id.btn_save_measure);
        btnDeleteMeasure = view.findViewById(R.id.btn_delete_measure);
        btnStartTraining = view.findViewById(R.id.btn_start_training);
        btnTrainingHistory = view.findViewById(R.id.btn_training_history);

        prefs = getActivity().getSharedPreferences("AquaTime", getContext().MODE_PRIVATE);

        setupMoodSpinner();
        setupDistanceSpinner();
        loadResults();
        setupCalendar();

        btnStartTimer.setOnClickListener(v -> startTimer());
        btnStopTimer.setOnClickListener(v -> stopTimer());
        btnResetTimer.setOnClickListener(v -> resetTimer());
        btnSaveMeasure.setOnClickListener(v -> saveMeasurement());
        btnDeleteMeasure.setOnClickListener(v -> deleteMeasurement());

        btnStartTraining.setOnClickListener(v -> generateAITraining());
        btnTrainingHistory.setOnClickListener(v -> showTrainingHistory());

        return view;
    }

    // =======================
    // TIMER
    // =======================
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.uptimeMillis() - startTime;
            int ms = (int) (millis % 1000);
            int seconds = (int) (millis / 1000) % 60;
            int minutes = (int) (millis / (1000 * 60));

            tvTimer.setText(String.format("%02d:%02d:%03d", minutes, seconds, ms));
            handler.postDelayed(this, 30);
        }
    };

    private void startTimer() {
        if (!running) {
            startTime = SystemClock.uptimeMillis();
            handler.postDelayed(timerRunnable, 0);
            running = true;
        }
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
        running = false;
    }

    private void resetTimer() {
        handler.removeCallbacks(timerRunnable);
        tvTimer.setText("00:00:00.000");
        running = false;
    }

    // =======================
    // MOOD & TIP
    // =======================
    private void setupMoodSpinner() {
        String[] moods = {"Отличное", "Нормальное", "Устал"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, moods);
        spinnerMood.setAdapter(adapter);

        spinnerMood.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: tvTipText.setText("Сегодня можно сделать интенсивную тренировку"); break;
                    case 1: tvTipText.setText("Работай над техникой"); break;
                    case 2: tvTipText.setText("Сделай лёгкую восстановительную тренировку"); break;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // =======================
    // DISTANCES
    // =======================
    private void setupDistanceSpinner() {
        String[] distances = {
                "50м Кроль", "100м Кроль", "200м Кроль", "400м Кроль",
                "50м Брасс", "100м Брасс", "200м Брасс",
                "50м Баттерфляй", "100м Баттерфляй",
                "50м Спина", "100м Спина", "200м Спина"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, distances);
        spinnerDistance.setAdapter(adapter);
    }

    // =======================
    // SAVE & DELETE RESULTS
    // =======================
    private void saveMeasurement() {
        String result = spinnerDistance.getSelectedItem() + " — " + tvTimer.getText();
        results.add(result);
        updateVolume();
        saveResults();
        updateAdapters();
    }

    private void deleteMeasurement() {
        int pos = listResults.getCheckedItemPosition();
        if (pos != ListView.INVALID_POSITION) {
            results.remove(pos);
            saveResults();
            updateAdapters();
        }
    }

    private void updateVolume() {
        String distance = spinnerDistance.getSelectedItem().toString();
        if (distance.contains("50")) weeklyVolume += 50;
        else if (distance.contains("100")) weeklyVolume += 100;
        else if (distance.contains("200")) weeklyVolume += 200;
        else if (distance.contains("400")) weeklyVolume += 400;

        int progress = (weeklyVolume * 100) / weeklyGoal;
        tvWeekProgress.setText("Прогресс недели: " + progress + "%");
    }

    private void saveResults() {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder builder = new StringBuilder();
        for (String r : results) builder.append(r).append(";");
        editor.putString("results", builder.toString());
        editor.apply();
    }

    private void loadResults() {
        String data = prefs.getString("results", "");
        if (!data.equals("")) {
            String[] items = data.split(";");
            for (String i : items) results.add(i);
        }
        updateAdapters();
    }

    private void updateAdapters() {
        resultsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_single_choice, results);
        listResults.setAdapter(resultsAdapter);

        bestAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, results);
        listBestResults.setAdapter(bestAdapter);
    }

    // =======================
    // AI TRAINER
    // =======================
    private void generateAITraining() {
        String style = spinnerDistance.getSelectedItem().toString();
        String training = "";

        switch (style) {
            case "50м Кроль": training = "Разминка 50м\n2x25 кроль\n50м расслабленно"; break;
            case "100м Кроль": training = "Разминка 100м\n4x25 кроль\n100м техника"; break;
            case "200м Кроль": training = "Разминка 200м\n4x50 кроль\n200м ноги"; break;
            case "400м Кроль": training = "Разминка 400м\n8x50 кроль\n200м расслабленно"; break;
            case "50м Брасс": training = "Разминка 50м\n2x25 брасс\n50м легко"; break;
            case "100м Брасс": training = "Разминка 100м\n4x25 брасс\n100м техника"; break;
            case "200м Брасс": training = "Разминка 200м\n4x50 брасс\n200м ноги"; break;
            case "50м Баттерфляй": training = "Разминка 50м\n2x25 баттерфляй\n50м легко"; break;
            case "100м Баттерфляй": training = "Разминка 100м\n4x25 баттерфляй\n100м техника"; break;
            case "50м Спина": training = "Разминка 50м\n2x25 спина\n50м легко"; break;
            case "100м Спина": training = "Разминка 100м\n4x25 спина\n100м техника"; break;
            case "200м Спина": training = "Разминка 200м\n4x50 спина\n200м ноги"; break;
            default: training = "Разминка 200м\n4x50 кроль\n100м легко";
        }

        tvTodayTraining.setText("AI Тренировка:\n" + training);
    }

    // =======================
    // TRAINING HISTORY
    // =======================
    private void showTrainingHistory() {
        StringBuilder sb = new StringBuilder("История тренировок:\n");
        for (String r : results) sb.append(r).append("\n");
        Toast.makeText(getContext(), sb.toString(), Toast.LENGTH_LONG).show();
    }

    // =======================
    // CALENDAR
    // =======================
    private void setupCalendar() {
        calendarTraining.setOnDateChangeListener((view, year, month, day) -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(
                    getContext(),
                    (timePicker, hour, minute) -> {
                        String date = day + "/" + (month + 1) + "/" + year + " " + hour + ":" + minute;
                        tvTodayTraining.setText("Тренировка: " + date);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
            );
            dialog.show();
        });
    }
}