package david.dokholyan.aquatime.ui.training;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import david.dokholyan.aquatime.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class TrainingFragment extends Fragment {

    private TextView tvCoachTip, tvWorkoutTitle, tvWorkoutDistance;
    private TextView tvBlockWarmup, tvBlockMain, tvBlockCooldown, tvTimer;
    private Button btnModeAi, btnModeConstructor;
    private Button btnActionPrimary, btnScheduleWorkout, btnCompleteNow;
    private Button btnTimerStart, btnTimerPause, btnTimerReset, btnSaveWorkoutResults;
    private Spinner spinnerStyles;


    private Handler handler = new Handler();
    private long startTime = 0L, updateTime = 0L, timeSwapBuff = 0L, timeInMilliseconds = 0L;
    private boolean running = false;

    private int currentMode = 0;
    private SharedPreferences prefs;


    private ArrayList<String> aiWarmupList = new ArrayList<>();
    private ArrayList<String> aiMainList = new ArrayList<>();
    private ArrayList<String> aiCooldownList = new ArrayList<>();
    private int aiTotalDistance = 0;

    private ArrayList<String> constWarmupList = new ArrayList<>();
    private ArrayList<String> constMainList = new ArrayList<>();
    private ArrayList<String> constCooldownList = new ArrayList<>();
    private int constTotalDistance = 0;

    private int lastAiDiff = 1;
    private String lastAiStyle = "Комплексный (Все стили)";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(view);
        setupStylesSpinner();
        showRandomTip();

        if (btnModeAi != null) btnModeAi.setOnClickListener(v -> switchMode(0));
        if (btnModeConstructor != null) btnModeConstructor.setOnClickListener(v -> switchMode(1));

        btnActionPrimary.setOnClickListener(v -> handlePrimaryAction());
        btnCompleteNow.setOnClickListener(v -> completeTrainingNow());
        btnScheduleWorkout.setOnClickListener(v -> openCalendarScheduler());

        btnTimerStart.setOnClickListener(v -> startTimer());
        btnTimerPause.setOnClickListener(v -> stopTimer());
        btnTimerReset.setOnClickListener(v -> resetTimer());
        btnSaveWorkoutResults.setOnClickListener(v -> saveWorkoutResults());

        restoreScreenState();

        return view;
    }

    private void initViews(View v) {
        tvCoachTip = v.findViewById(R.id.tv_coach_tip);
        tvWorkoutTitle = v.findViewById(R.id.tv_workout_title);
        tvWorkoutDistance = v.findViewById(R.id.tv_workout_distance);
        tvBlockWarmup = v.findViewById(R.id.tv_block_warmup);
        tvBlockMain = v.findViewById(R.id.tv_block_main);
        tvBlockCooldown = v.findViewById(R.id.tv_block_cooldown);
        tvTimer = v.findViewById(R.id.tv_timer);

        btnModeAi = v.findViewById(R.id.btn_mode_ai);
        btnModeConstructor = v.findViewById(R.id.btn_mode_constructor);

        View btnManual = v.findViewById(R.id.btn_mode_manual);
        if (btnManual != null) btnManual.setVisibility(View.GONE);

        btnActionPrimary = v.findViewById(R.id.btn_action_primary);
        btnScheduleWorkout = v.findViewById(R.id.btn_schedule_workout);
        btnCompleteNow = v.findViewById(R.id.btn_complete_now);

        btnTimerStart = v.findViewById(R.id.btn_timer_start);
        btnTimerPause = v.findViewById(R.id.btn_timer_pause);
        btnTimerReset = v.findViewById(R.id.btn_timer_reset);
        btnSaveWorkoutResults = v.findViewById(R.id.btn_save_workout_results);

        spinnerStyles = v.findViewById(R.id.spinner_styles);
    }

    private void showRandomTip() {
        if (tvCoachTip == null) return;

        String[] tips = {
                "Совет: Не забывайте делать разминку на суше перед прыжком в воду!",
                "Совет: Держите голову ниже при плавании кролем для лучшей обтекаемости.",
                "Совет: Высокий локоть при гребке существенно экономит ваши силы.",
                "Совет: Сильный и эффективный удар ногами идет от бедра, а не от колена."
        };

        Random random = new Random();
        int randomIndex = random.nextInt(tips.length);
        tvCoachTip.setText(tips[randomIndex]);
    }

    private void setupStylesSpinner() {
        String[] list = {
                "50м Вольный стиль", "100м Вольный стиль", "200м Вольный стиль",
                "50м Брасс", "100м Брасс", "200м Брасс",
                "50м На спине", "100м На спине", "200м На спине",
                "50м Баттерфляй", "100м Баттерфляй", "200м Баттерфляй",
                "100м Комплекс", "200м Комплекс"
        };
        if (getContext() != null && spinnerStyles != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, list);
            spinnerStyles.setAdapter(adapter);
        }
    }

    private void switchMode(int mode) {
        currentMode = mode;
        prefs.edit().putInt("saved_mode", mode).apply();

        if (btnModeAi != null) btnModeAi.setBackgroundTintList(android.content.res.ColorStateList.valueOf(mode == 0 ? 0xFFE3F2FD : Color.TRANSPARENT));
        if (btnModeConstructor != null) btnModeConstructor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(mode == 1 ? 0xFFE3F2FD : Color.TRANSPARENT));

        if (mode == 0) {
            if (tvWorkoutTitle != null) tvWorkoutTitle.setText("Интеллектуальный план от ИИ");
            btnActionPrimary.setText(aiTotalDistance > 0 ? "Поменять план 🔄" : "Сгенерировать ИИ-план");
            btnActionPrimary.setVisibility(View.VISIBLE);
            updateScreenFields(aiTotalDistance, aiWarmupList, aiMainList, aiCooldownList);
            toggleActionButtons(aiTotalDistance > 0);
        } else {
            if (tvWorkoutTitle != null) tvWorkoutTitle.setText("Кастомный конструктор");
            btnActionPrimary.setText("Добавить упражнение в блок");
            btnActionPrimary.setVisibility(View.VISIBLE);
            updateScreenFields(constTotalDistance, constWarmupList, constMainList, constCooldownList);
            toggleActionButtons(constTotalDistance > 0);
        }
    }

    private void toggleActionButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (btnScheduleWorkout != null) btnScheduleWorkout.setVisibility(visibility);
        if (btnCompleteNow != null) btnCompleteNow.setVisibility(visibility);
    }

    private void handlePrimaryAction() {
        if (currentMode == 0) {
            if (aiTotalDistance > 0) {
                generateCustomAiPlan(lastAiDiff, lastAiStyle);
                Toast.makeText(getContext(), "ИИ подобрал новые упражнения! 🔄", Toast.LENGTH_SHORT).show();
            } else {
                showAiPreferencesDialog();
            }
        } else {
            showBuildBlockDialog();
        }
    }

    private void completeTrainingNow() {
        int activeDistance = (currentMode == 0) ? aiTotalDistance : constTotalDistance;
        if (activeDistance == 0) return;

        int calcTotalSeconds = Math.max(60, (activeDistance / 50) * 45);
        int mins = calcTotalSeconds / 60;
        int secs = calcTotalSeconds % 60;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs);

        String fullDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        String historyEntry = activeDistance + " | " + formattedTime + " | " + fullDateStr;

        String oldHistory = prefs.getString("all_res", "");
        String updatedHistory = oldHistory.isEmpty() ? historyEntry : historyEntry + ";" + oldHistory;

        int currentTotalMeters = prefs.getInt("total_meters", 0);
        int currentTrainingsCount = prefs.getInt("trainings_count", 0);
        int currentWeeklyMeters = prefs.getInt("weekly_meters", 0);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("all_res", updatedHistory);
        editor.putInt("total_meters", currentTotalMeters + activeDistance);
        editor.putInt("trainings_count", currentTrainingsCount + 1);
        editor.putInt("weekly_meters", currentWeeklyMeters + activeDistance);

        if (currentMode == 0) {
            aiWarmupList.clear(); aiMainList.clear(); aiCooldownList.clear(); aiTotalDistance = 0;
            editor.putInt("ai_total_dist", 0).putString("ai_wu", "").putString("ai_main", "").putString("ai_cd", "");
        } else {
            constWarmupList.clear(); constMainList.clear(); constCooldownList.clear(); constTotalDistance = 0;
            editor.putInt("const_total_dist", 0).putString("const_wu", "").putString("const_main", "").putString("const_cd", "");
        }
        editor.apply();

        Toast.makeText(getContext(), "План выполнен! Рейтинг обновлен 🏆", Toast.LENGTH_LONG).show();
        switchMode(currentMode);
    }

    private void showAiPreferencesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Настройка ИИ Тренировки");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);

        final Spinner spinnerDiff = new Spinner(getContext());
        String[] diffs = {"Легкая (Восстановление)", "Средняя (Обычная)", "Тяжелая (Хардкор)"};
        spinnerDiff.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, diffs));

        final Spinner spinnerStylePref = new Spinner(getContext());
        String[] stylePrefs = {"Комплексный (Все стили)", "Вольный стиль (Кроль)", "Брасс", "Баттерфляй", "На спине"};
        spinnerStylePref.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, stylePrefs));

        layout.addView(spinnerDiff);
        layout.addView(spinnerStylePref);
        builder.setView(layout);

        builder.setPositiveButton("Создать план", (dialog, which) -> {
            lastAiDiff = spinnerDiff.getSelectedItemPosition();
            lastAiStyle = spinnerStylePref.getSelectedItem().toString();
            generateCustomAiPlan(lastAiDiff, lastAiStyle);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void generateCustomAiPlan(int difficulty, String swimStyle) {
        aiWarmupList.clear(); aiMainList.clear(); aiCooldownList.clear();
        int baseDistance = (difficulty + 1) * 500;

        aiWarmupList.add("200м Разминка (легкий темп)");
        int randomVariation = new Random().nextInt(2);

        if (swimStyle.contains("Комплексный")) {
            if (randomVariation == 0) {
                aiMainList.add("4 x 50м Комплекс (смена стилей каждые 50м)");
                aiMainList.add("4 x 100м Кроль/Брасс поочередно");
            } else {
                aiMainList.add("200м Комплексное плавание на технику");
                aiMainList.add("8 x 50м Ноги с доской (все стили по 2 раза)");
            }
        } else {
            if (randomVariation == 0) {
                aiMainList.add("4 x 100м Серия: " + swimStyle + " (акцент на захват воды)");
                if (difficulty > 0) aiMainList.add("6 x 50м Спринт: " + swimStyle + " (макс. темп)");
            } else {
                aiMainList.add("8 x 50м Основной стиль: " + swimStyle + " (координация)");
                if (difficulty > 0) aiMainList.add("2 x 200м " + swimStyle + " на удержание шага гребка");
            }
        }

        aiCooldownList.add("100м Откупка расслабленным на спине");
        aiTotalDistance = baseDistance + (randomVariation * 100) + 200;

        updateScreenFields(aiTotalDistance, aiWarmupList, aiMainList, aiCooldownList);
        saveScreenState();

        if (btnActionPrimary != null) btnActionPrimary.setText("Поменять план 🔄");
        toggleActionButtons(true);
    }

    private void showBuildBlockDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Конструктор упражнений");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final Spinner spinnerBlock = new Spinner(getContext());
        String[] blocks = {"Разминка (Warm-up)", "Основная серия (Main)", "Заминка (Cool-down)"};
        spinnerBlock.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, blocks));

        final EditText etTask = new EditText(getContext());
        etTask.setHint("Что делать (напр: 4х50м Кроль)");

        final EditText etMeters = new EditText(getContext());
        etMeters.setHint("Метры (число)");
        etMeters.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        layout.addView(spinnerBlock);
        layout.addView(etTask);
        layout.addView(etMeters);
        builder.setView(layout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String task = etTask.getText().toString().trim();
            String metersStr = etMeters.getText().toString().trim();
            int meters = metersStr.isEmpty() ? 0 : Integer.parseInt(metersStr);
            int selectedBlock = spinnerBlock.getSelectedItemPosition();

            if (!task.isEmpty()) {
                String entry = task + " (" + meters + "м)";
                if (selectedBlock == 0) constWarmupList.add(entry);
                else if (selectedBlock == 1) constMainList.add(entry);
                else constCooldownList.add(entry);

                constTotalDistance += meters;
                updateScreenFields(constTotalDistance, constWarmupList, constMainList, constCooldownList);
                saveScreenState();

                toggleActionButtons(true);
            }
        });
        builder.show();
    }

    private void updateScreenFields(int distance, ArrayList<String> wu, ArrayList<String> main, ArrayList<String> cd) {
        if (tvWorkoutDistance != null) tvWorkoutDistance.setText("Общая дистанция: " + distance + " м");
        updateBlockTextView(tvBlockWarmup, wu);
        updateBlockTextView(tvBlockMain, main);
        updateBlockTextView(tvBlockCooldown, cd);
    }

    private void updateBlockTextView(TextView tv, ArrayList<String> list) {
        if (tv == null) return;
        if (list.isEmpty()) {
            tv.setText(currentMode == 1 ? "• Блок пуст. Нажмите кнопку добавления." : "Нажми кнопку ниже, чтобы сгенерировать план");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append("• ").append(s).append("\n");
        tv.setText(sb.toString().trim());
    }

    private void saveScreenState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("ai_total_dist", aiTotalDistance);
        editor.putString("ai_wu", TextUtils.join(";", aiWarmupList));
        editor.putString("ai_main", TextUtils.join(";", aiMainList));
        editor.putString("ai_cd", TextUtils.join(";", aiCooldownList));

        editor.putInt("const_total_dist", constTotalDistance);
        editor.putString("const_wu", TextUtils.join(";", constWarmupList));
        editor.putString("const_main", TextUtils.join(";", constMainList));
        editor.putString("const_cd", TextUtils.join(";", constCooldownList));

        editor.putInt("last_ai_diff", lastAiDiff);
        editor.putString("last_ai_style", lastAiStyle);
        editor.apply();
    }

    private void restoreScreenState() {
        int mode = prefs.getInt("saved_mode", 0);
        lastAiDiff = prefs.getInt("last_ai_diff", 1);
        lastAiStyle = prefs.getString("last_ai_style", "Комплексный (Все стили)");

        aiTotalDistance = prefs.getInt("ai_total_dist", 0);
        String aiWu = prefs.getString("ai_wu", "");
        String aiMn = prefs.getString("ai_main", "");
        String aiCd = prefs.getString("ai_cd", "");
        if (!aiWu.isEmpty()) aiWarmupList = new ArrayList<>(Arrays.asList(aiWu.split(";")));
        if (!aiMn.isEmpty()) aiMainList = new ArrayList<>(Arrays.asList(aiMn.split(";")));
        if (!aiCd.isEmpty()) aiCooldownList = new ArrayList<>(Arrays.asList(aiCd.split(";")));

        constTotalDistance = prefs.getInt("const_total_dist", 0);
        String cWu = prefs.getString("const_wu", "");
        String cMn = prefs.getString("const_main", "");
        String cCd = prefs.getString("const_cd", "");
        if (!cWu.isEmpty()) constWarmupList = new ArrayList<>(Arrays.asList(cWu.split(";")));
        if (!cMn.isEmpty()) constMainList = new ArrayList<>(Arrays.asList(cMn.split(";")));
        if (!cCd.isEmpty()) constCooldownList = new ArrayList<>(Arrays.asList(cCd.split(";")));

        switchMode(mode);
    }

    private void saveWorkoutResults() {
        String time = tvTimer.getText().toString();
        if (time.equals("00:00:00") || tvTimer == null) return;

        int sessionMeters = (currentMode == 0) ? (aiTotalDistance > 0 ? aiTotalDistance : 500) : (constTotalDistance > 0 ? constTotalDistance : 500);
        int currentTotalMeters = prefs.getInt("total_meters", 0);
        int currentTrainingsCount = prefs.getInt("trainings_count", 0);
        int currentWeeklyMeters = prefs.getInt("weekly_meters", 0);

        String[] timeParts = time.split(":");
        String formattedTime = timeParts[0] + ":" + timeParts[1];

        String fullDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        String historyEntry = sessionMeters + " | " + formattedTime + " | " + fullDateStr;

        String oldHistory = prefs.getString("all_res", "");
        String updatedHistory = oldHistory.isEmpty() ? historyEntry : historyEntry + ";" + oldHistory;

        prefs.edit()
                .putString("all_res", updatedHistory)
                .putInt("total_meters", currentTotalMeters + sessionMeters)
                .putInt("trainings_count", currentTrainingsCount + 1)
                .putInt("weekly_meters", currentWeeklyMeters + sessionMeters)
                .apply();

        Toast.makeText(getContext(), "Результат сохранен в Аналитику и Профиль! 🌊", Toast.LENGTH_SHORT).show();
        resetTimer();
        switchMode(currentMode);
    }


    private void openCalendarScheduler() {
        if (getContext() == null) return;

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, selectedYear, selectedMonth, selectedDay) -> {

            String formattedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear);


            int activeDistance = (currentMode == 0) ? aiTotalDistance : constTotalDistance;


            String planGoalText = "Заплыв на " + activeDistance + " м (" + formattedDate + ")";


            String currentPlannerData = prefs.getString("planner", "");
            String updatedPlannerData;
            if (currentPlannerData.isEmpty() || currentPlannerData.equals("Нет запланированных целей")) {
                updatedPlannerData = "• " + planGoalText;
            } else {
                updatedPlannerData = "• " + planGoalText + "\n" + currentPlannerData;
            }

            prefs.edit().putString("planner", updatedPlannerData).apply();

            Toast.makeText(getContext(), "Тренировка запланирована на " + formattedDate + "! 📅", Toast.LENGTH_LONG).show();
        }, year, month, day);

        datePickerDialog.show();
    }

    private void startTimer() { if (!running) { startTime = SystemClock.uptimeMillis(); handler.postDelayed(timerRunnable, 0); running = true; } }
    private void stopTimer() { if (running) { timeSwapBuff += timeInMilliseconds; handler.removeCallbacks(timerRunnable); running = false; } }
    private void resetTimer() { stopTimer(); startTime = 0L; updateTime = 0L; timeSwapBuff = 0L; timeInMilliseconds = 0L; if (tvTimer != null) tvTimer.setText("00:00:00"); }

    private Runnable timerRunnable = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updateTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updateTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updateTime % 1000) / 10;
            if (tvTimer != null) {
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", mins, secs, milliseconds));
            }
            handler.postDelayed(this, 10);
        }
    };
}