package david.dokholyan.aquatime.ui.training;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import david.dokholyan.aquatime.R;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TrainingFragment extends Fragment {
    private TextView tvPageMainTitle;
    private TextView tvCoachTip, tvWorkoutTitle, tvWorkoutDistance;
    private TextView tvBlockWarmup, tvBlockMain, tvBlockCooldown, tvTimer;
    private TextView tvLabelWarmup, tvLabelMain, tvLabelCooldown;
    private Button btnModeAi, btnModeConstructor;
    private Button btnActionPrimary, btnScheduleWorkout, btnCompleteNow;
    private Button btnTimerStart, btnTimerPause, btnTimerReset, btnSaveWorkoutResults;
    private Spinner spinnerStyles;

    private Handler handler = new Handler();
    private long startTime = 0L, updateTime = 0L, timeSwapBuff = 0L, timeInMilliseconds = 0L;
    private boolean running = false;

    private int currentMode = 0;
    private SharedPreferences prefs;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private ArrayList<String> aiWarmupList = new ArrayList();
    private ArrayList<String> aiMainList = new ArrayList();
    private ArrayList<String> aiCooldownList = new ArrayList();
    private int aiTotalDistance = 0;
    private ArrayList<String> constWarmupList = new ArrayList();
    private ArrayList<String> constMainList = new ArrayList();
    private ArrayList<String> constCooldownList = new ArrayList();
    private int constTotalDistance = 0;
    private int lastAiDiff = 1;
    private String lastAiStyle = "Комплексный (Все стили)";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews(view);
        setupStylesSpinner();
        showRandomTip();

        if (auth.getCurrentUser() != null) {
            loadUserDataFromFirestore();
        } else {
            restoreScreenState();
        }

        if (btnModeAi != null) btnModeAi.setOnClickListener(v -> switchMode(0));
        if (btnModeConstructor != null) btnModeConstructor.setOnClickListener(v -> switchMode(1));

        btnActionPrimary.setOnClickListener(v -> handlePrimaryAction());

        btnActionPrimary.setOnLongClickListener(v -> {
            if (currentMode == 0 && aiTotalDistance > 0) {
                showResetAiPlanDialog();
                return true;
            }
            return false;
        });

        btnCompleteNow.setOnClickListener(v -> completeTrainingNow());
        btnScheduleWorkout.setOnClickListener(v -> openCalendarScheduler());

        btnTimerStart.setOnClickListener(v -> startTimer());
        btnTimerPause.setOnClickListener(v -> stopTimer());
        btnTimerReset.setOnClickListener(v -> resetTimer());
        btnSaveWorkoutResults.setOnClickListener(v -> saveWorkoutResults());

        requestNotificationPermission();

        return view;
    }

    private void initViews(View v) {
        tvPageMainTitle = v.findViewById(R.id.tv_page_main_title);
        if (tvPageMainTitle != null) {
            tvPageMainTitle.setText(isEnglish() ? "Training" : "Тренировка");
        }

        tvCoachTip = v.findViewById(R.id.tv_coach_tip);
        tvWorkoutTitle = v.findViewById(R.id.tv_workout_title);
        tvWorkoutDistance = v.findViewById(R.id.tv_workout_distance);
        tvBlockWarmup = v.findViewById(R.id.tv_block_warmup);
        tvBlockMain = v.findViewById(R.id.tv_block_main);
        tvBlockCooldown = v.findViewById(R.id.tv_block_cooldown);
        tvTimer = v.findViewById(R.id.tv_timer);
        tvLabelWarmup = v.findViewById(R.id.tv_label_warmup);
        tvLabelMain = v.findViewById(R.id.tv_label_main);
        tvLabelCooldown = v.findViewById(R.id.tv_label_cooldown);
        btnModeAi = v.findViewById(R.id.btn_mode_ai);
        btnModeConstructor = v.findViewById(R.id.btn_mode_constructor);

        if (btnModeAi != null) btnModeAi.setText(isEnglish() ? "READY-MADE" : "ГОТОВЫЕ");
        if (btnModeConstructor != null) btnModeConstructor.setText(isEnglish() ? "CONSTRUCTOR" : "КОНСТРУКТОР");

        btnActionPrimary = v.findViewById(R.id.btn_action_primary);
        btnScheduleWorkout = v.findViewById(R.id.btn_schedule_workout);
        btnCompleteNow = v.findViewById(R.id.btn_complete_now);

        btnTimerStart = v.findViewById(R.id.btn_timer_start);
        btnTimerPause = v.findViewById(R.id.btn_timer_pause);
        btnTimerReset = v.findViewById(R.id.btn_timer_reset);
        btnSaveWorkoutResults = v.findViewById(R.id.btn_save_workout_results);

        if (btnTimerStart != null) btnTimerStart.setText(isEnglish() ? "START" : "СТАРТ");
        if (btnTimerPause != null) btnTimerPause.setText(isEnglish() ? "STOP" : "СТОП");
        if (btnTimerReset != null) btnTimerReset.setText(isEnglish() ? "RESET" : "СБРОС");
        if (btnSaveWorkoutResults != null) {
            btnSaveWorkoutResults.setText(isEnglish() ? "Save Swim to Analytics" : "Сохранить заплыв в Аналитику");
        }

        spinnerStyles = v.findViewById(R.id.spinner_styles);
    }

    private void loadUserDataFromFirestore() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        firestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            if (document.contains("total_meters")) {
                                prefs.edit().putInt("total_meters", document.getLong("total_meters").intValue()).apply();
                            }
                            if (document.contains("trainings_count")) {
                                prefs.edit().putInt("trainings_count", document.getLong("trainings_count").intValue()).apply();
                            }
                            if (document.contains("weekly_meters")) {
                                prefs.edit().putInt("weekly_meters", document.getLong("weekly_meters").intValue()).apply();
                            }
                            if (document.contains("all_res")) {
                                prefs.edit().putString("all_res", document.getString("all_res")).apply();
                            }
                            if (document.contains("stopwatch_log")) {
                                prefs.edit().putString("stopwatch_log", document.getString("stopwatch_log")).apply();
                            }
                            if (document.contains("planner")) {
                                prefs.edit().putString("planner", document.getString("planner")).apply();
                            }

                            // Load personal records
                            for (int i = 0; i < 5; i++) {
                                String recordKey = "best_style_" + i;
                                if (document.contains(recordKey)) {
                                    prefs.edit().putString(recordKey, document.getString(recordKey)).apply();
                                }
                            }
                        }
                    }

                    restoreScreenState();
                });
    }


    private void saveWorkoutResults() {
        String time = tvTimer.getText().toString();
        if (time.equals("00:00:00") || tvTimer == null) {
            Toast.makeText(getContext(), isEnglish() ? "Start the timer!" : "Запустите таймер!", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedStyle = (spinnerStyles != null) ? spinnerStyles.getSelectedItem().toString() : (isEnglish() ? "50m Freestyle" : "50м Вольный стиль");


        int sessionMeters = (currentMode == 0) ? aiTotalDistance : constTotalDistance;
        if (sessionMeters == 0) {
            sessionMeters = parseMetersFromStyleString(selectedStyle);
        }

        String[] timeParts = time.split(":");
        String formattedTime = timeParts[0] + ":" + timeParts[1];
        String fullDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        saveGlobalWorkoutData(sessionMeters, formattedTime, fullDateStr, selectedStyle);
        checkAndUpgradePersonalRecord(selectedStyle, time);
        saveToStopwatchLog(selectedStyle, time, fullDateStr);

        Toast.makeText(getContext(), isEnglish() ? "Result saved! 🌊" : "Результат сохранен! 🌊", Toast.LENGTH_SHORT).show();
        resetTimer();
        switchMode(currentMode);
    }


    private int parseMetersFromStyleString(String styleName) {
        try {

            String numericOnly = styleName.replaceAll("[^0-9]", "").trim();
            if (!numericOnly.isEmpty()) {
                return Integer.parseInt(numericOnly);
            }
        } catch (Exception e) {
            Log.e("AquaTime", "Ошибка парсинга метров из спиннера", e);
        }
        return 50;
    }

    private void saveToStopwatchLog(String style, String time, String date) {
        String entry = style + "|" + time + "|" + date;
        String currentLog = prefs.getString("stopwatch_log", "");
        String updatedLog;

        if (currentLog.isEmpty()) {
            updatedLog = entry;
        } else {
            updatedLog = entry + ";" + currentLog;
        }

        prefs.edit().putString("stopwatch_log", updatedLog).apply();

        // Save to Firestore
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("stopwatch_log", updatedLog);

            firestore.collection("users").document(userId)
                    .set(updates, SetOptions.merge())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to sync: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }


    private void checkAndUpgradePersonalRecord(String fullStyleName, String currentTimeStr) {
        int styleIdx = -1;
        String lowerStyle = fullStyleName.toLowerCase();

        if (lowerStyle.contains("50м вольный") || lowerStyle.contains("50m freestyle")) styleIdx = 0;
        else if (lowerStyle.contains("50м брасс") || lowerStyle.contains("50m breaststroke")) styleIdx = 1;
        else if (lowerStyle.contains("50м на спине") || lowerStyle.contains("50m backstroke")) styleIdx = 2;
        else if (lowerStyle.contains("50м баттерфляй") || lowerStyle.contains("50m butterfly")) styleIdx = 3;
        else if (lowerStyle.contains("100м комплекс") || lowerStyle.contains("100m individual medley")) styleIdx = 4;

        if (styleIdx == -1) return;

        String prefKey = "best_style_" + styleIdx;
        String savedBestTime = prefs.getString(prefKey, "99:99:99");

        double currentSec = parseTimeToSecondsInternal(currentTimeStr);
        double bestSec = parseTimeToSecondsInternal(savedBestTime);

        if (currentSec > 0 && currentSec < bestSec) {
            prefs.edit().putString(prefKey, currentTimeStr).apply();


            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put(prefKey, currentTimeStr);
                firestore.collection("users").document(user.getUid())
                        .set(updates, SetOptions.merge());
            }

            String message = isEnglish() ?
                    "🎉 New Personal Record! 🎉\n" + fullStyleName + ": " + currentTimeStr :
                    "🎉 Новый личный рекорд! 🎉\n" + fullStyleName + ": " + currentTimeStr;
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }


    private void saveGlobalWorkoutData(int distance, String timeStr, String dateStr, String style) {
        int currentTotalMeters = prefs.getInt("total_meters", 0);
        int currentTrainingsCount = prefs.getInt("trainings_count", 0);
        int currentWeeklyMeters = prefs.getInt("weekly_meters", 0);

        String historyEntry = distance + " | " + timeStr + " | " + dateStr + " | " + style;
        String oldHistory = prefs.getString("all_res", "");
        String updatedHistory = oldHistory.isEmpty() ? historyEntry : historyEntry + ";" + oldHistory;

        int newTotalMeters = currentTotalMeters + distance;
        int newTrainingsCount = currentTrainingsCount + 1;
        int newWeeklyMeters = currentWeeklyMeters + distance;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("all_res", updatedHistory);
        editor.putInt("total_meters", newTotalMeters);
        editor.putInt("trainings_count", newTrainingsCount);
        editor.putInt("weekly_meters", newWeeklyMeters);
        editor.putInt("last_completed_distance", distance);
        editor.putString("last_completed_time", timeStr);
        editor.putString("last_completed_date", dateStr);
        editor.putString("last_completed_style", style);
        editor.apply();


        if (getContext() != null) {
            Intent intent = new Intent("david.dokholyan.aquatime.ACTION_WORKOUT_SAVED");
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            Map<String, Object> workoutData = new HashMap<>();
            workoutData.put("distance", distance);
            workoutData.put("time", timeStr);
            workoutData.put("date", dateStr);
            workoutData.put("style", style);
            workoutData.put("timestamp", System.currentTimeMillis());

            firestore.collection("users")
                    .document(userId)
                    .collection("workouts")
                    .add(workoutData);

            Map<String, Object> statsUpdate = new HashMap<>();
            statsUpdate.put("total_meters", newTotalMeters);
            statsUpdate.put("trainings_count", newTrainingsCount);
            statsUpdate.put("weekly_meters", newWeeklyMeters);
            statsUpdate.put("all_res", updatedHistory);

            statsUpdate.put("last_completed_distance", distance);
            statsUpdate.put("last_completed_time", timeStr);
            statsUpdate.put("last_completed_date", dateStr);
            statsUpdate.put("last_completed_style", style);
            statsUpdate.put("last_workout_date", dateStr);

            firestore.collection("users").document(userId)
                    .set(statsUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), isEnglish() ? "Cloud Synchronized ☁️" : "Облако синхронизировано ☁️", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Sync failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    private void addToPlanner(String planGoalText) {
        String currentPlannerData = prefs.getString("planner", "").trim();
        String emptyGoalsText = isEnglish() ? "No goals planned" : "Нет запланированных целей";

        String updatedPlannerData = (currentPlannerData.isEmpty() || currentPlannerData.equals(emptyGoalsText))
                ? "• " + planGoalText : currentPlannerData + "\n• " + planGoalText;

        prefs.edit().putString("planner", updatedPlannerData).apply();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> plannerUpdate = new HashMap<>();
            plannerUpdate.put("planner", updatedPlannerData);
            firestore.collection("users").document(user.getUid())
                    .set(plannerUpdate, SetOptions.merge());
        }
    }

    private void removeCalendarPlanAfterExecution(int distance) {
        String currentPlannerData = prefs.getString("planner", "").trim();
        String emptyGoalsText = isEnglish() ? "No goals planned" : "Нет запланированных целей";
        if (currentPlannerData.isEmpty() || currentPlannerData.equals(emptyGoalsText)) return;

        String[] lines = currentPlannerData.split("\n");
        StringBuilder updatedPlanner = new StringBuilder();
        boolean removed = false;

        String targetSearchRu = "Заплыв на " + distance + " м";
        String targetSearchEn = "Swim for " + distance + " m";

        for (String line : lines) {
            if (!removed && (line.contains(targetSearchRu) || line.contains(targetSearchEn))) {
                removed = true;
                continue;
            }
            if (updatedPlanner.length() > 0) updatedPlanner.append("\n");
            updatedPlanner.append(line);
        }

        String result = updatedPlanner.toString().trim();
        if (result.isEmpty()) result = emptyGoalsText;
        prefs.edit().putString("planner", result).apply();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> plannerUpdate = new HashMap<>();
            plannerUpdate.put("planner", result);
            firestore.collection("users").document(user.getUid())
                    .set(plannerUpdate, SetOptions.merge());
        }
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

        FirebaseUser user = auth.getCurrentUser();
        if (user != null && aiTotalDistance > 0) {
            Map<String, Object> planData = new HashMap<>();
            planData.put("ai_total_dist", aiTotalDistance);
            planData.put("ai_wu", aiWarmupList);
            planData.put("ai_main", aiMainList);
            planData.put("ai_cd", aiCooldownList);
            planData.put("last_ai_diff", lastAiDiff);
            planData.put("last_ai_style", lastAiStyle);
            planData.put("last_updated", System.currentTimeMillis());

            firestore.collection("users").document(user.getUid())
                    .collection("training_plans")
                    .document("current_plan")
                    .set(planData, SetOptions.merge());
        }
    }

    private void setupAlarms(Calendar targetTime, int minutesBefore, int distance) {
        Context context = getContext();
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                return;
            }
        }


        String formattedDate = String.format(Locale.getDefault(), "%02d.%02d.%04d",
                targetTime.get(Calendar.DAY_OF_MONTH), targetTime.get(Calendar.MONTH) + 1, targetTime.get(Calendar.YEAR));

        String planGoalText = isEnglish() ?
                "Swim for " + distance + " m (" + formattedDate + ")" :
                "Заплыв на " + distance + " м (" + formattedDate + ")";

        addToPlanner(planGoalText);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Map<String, Object> scheduledWorkout = new HashMap<>();
            scheduledWorkout.put("distance", distance);
            scheduledWorkout.put("date", formattedDate);
            scheduledWorkout.put("time", String.format("%02d:%02d", targetTime.get(Calendar.HOUR_OF_DAY), targetTime.get(Calendar.MINUTE)));
            scheduledWorkout.put("minutes_before", minutesBefore);
            scheduledWorkout.put("timestamp", targetTime.getTimeInMillis());

            firestore.collection("users")
                    .document(user.getUid())
                    .collection("scheduled_workouts")
                    .add(scheduledWorkout);
        }

        Toast.makeText(context, isEnglish() ? "Workout successfully scheduled! 📅" : "Тренировка успешно запланирована! 📅", Toast.LENGTH_LONG).show();
    }

    private double parseTimeToSecondsInternal(String timeStr) {
        try {
            if (timeStr.equals("00:00:00") || timeStr.equals("99:99:99")) return 9999.0;
            String[] parts = timeStr.split(":");
            int min = Integer.parseInt(parts[0]);
            int sec = Integer.parseInt(parts[1]);
            int ms = Integer.parseInt(parts[2]);
            return min * 60 + sec + ms / 100.0;
        } catch (Exception e) { return 9999.0; }
    }


    private boolean isEnglish() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("en");
    }

    private void showRandomTip() {
        if (tvCoachTip == null) return;
        String[] tips;
        if (isEnglish()) {
            tips = new String[]{
                    "Tip: Don't forget to warm up on dry land before jumping into the water!",
                    "Tip: Keep your head lower when swimming freestyle for better streamlining.",
                    "Tip: A high elbow during the stroke significantly saves your energy.",
                    "Tip: A strong and effective kick comes from the hip, not the knee.",
                    "Tip: Focus on technique first — speed comes naturally with good form.",
                    "Tip: Keep your body as horizontal as possible to reduce water resistance.",
                    "Tip: Relax your shoulders during recovery to avoid unnecessary tension.",
                    "Tip: Consistent training is more important than occasional hard workouts.",
                    "Tip: Push off the wall explosively to gain speed after every turn.",
                    "Tip: Use your core muscles to maintain balance and stability in the water.",
            };
        } else {
            tips = new String[]{
                    "Совет: Не забывайте делать разминку на суше перед прыжком в воду!",
                    "Совет: Держите голову ниже при плавании кролем для лучшей обтекаемости.",
                    "Совет: Высокий локоть при гребке существенно экономит ваши силы.",
                    "Совет: Сильный и эффективный удар ногами идет от бедра, а не от колена.",
                    "Совет: Сначала сосредоточьтесь на технике — скорость придет с хорошей формой.",
                    "Совет: Держите тело максимально горизонтально, чтобы уменьшить сопротивление воды.",
                    "Совет: Расслабляйте плечи во время проноса рук, чтобы избежать лишнего напряжения.",
                    "Совет: Регулярные тренировки важнее редких тяжелых нагрузок.",
                    "Совет: Мощно отталкивайтесь от бортика, чтобы набирать скорость после каждого поворота.",
                    "Совет: Используйте мышцы кора для поддержания баланса и стабильности в воде.",
            };
        }
        Random random = new Random();
        int randomIndex = random.nextInt(tips.length);
        tvCoachTip.setText(tips[randomIndex]);
    }

    private void setupStylesSpinner() {
        String[] list;
        if (isEnglish()) {
            list = new String[]{
                    "50m Freestyle", "100m Freestyle", "200m Freestyle",
                    "50m Breaststroke", "100m Breaststroke", "200m Breaststroke",
                    "50m Backstroke", "100m Backstroke", "200m Backstroke",
                    "50m Butterfly", "100m Butterfly", "200m Butterfly",
                    "100m Individual Medley", "200m Individual Medley"
            };
        } else {
            list = new String[]{
                    "50м Вольный стиль", "100м Вольный стиль", "200м Вольный стиль",
                    "50м Брасс", "100м Брасс", "200м Брасс",
                    "50м На спине", "100м На спине", "200м На спине",
                    "50м Баттерфляй", "100м Баттерфляй", "200м Баттерфляй",
                    "100м Комплекс", "200м Комплекс"
            };
        }
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

        boolean en = isEnglish();

        if (tvLabelWarmup != null) tvLabelWarmup.setText(en ? "🏁 WARM-UP" : "🏁 РАЗМИНКА (WARM-UP)");
        if (tvLabelMain != null) tvLabelMain.setText(en ? "🏊‍♂️ MAIN SET" : "🏊‍♂️ ОСНОВНАЯ СЕРИЯ (MAIN SET)");
        if (tvLabelCooldown != null) tvLabelCooldown.setText(en ? "🛑 COOL-DOWN" : "🛑 ЗАМИНКА (COOL-DOWN)");

        if (btnScheduleWorkout != null) btnScheduleWorkout.setText(en ? "Schedule 📅" : "Запланировать 📅");
        if (btnCompleteNow != null) btnCompleteNow.setText(en ? "Complete Now! ✅" : "Выполнить сейчас! ✅");

        if (mode == 0) {
            if (tvWorkoutTitle != null) tvWorkoutTitle.setText(en ? "Ready-made Workouts" : "Готовые тренировки");
            btnActionPrimary.setText(aiTotalDistance > 0 ? (en ? "Reset Plan ❌" : "Сбросить план ❌") : (en ? "Select ready plan" : "Выбрать готовый план"));
            btnActionPrimary.setVisibility(View.VISIBLE);
            updateScreenFields(aiTotalDistance, aiWarmupList, aiMainList, aiCooldownList);
            toggleActionButtons(aiTotalDistance > 0);
        } else {
            if (tvWorkoutTitle != null) tvWorkoutTitle.setText(en ? "Workout Constructor" : "Конструктор тренировки");
            btnActionPrimary.setText(en ? "Assemble Custom Plan" : "Собрать кастомный план");
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
                CharSequence[] options = isEnglish() ?
                        new CharSequence[]{"Change Plan", "Completely Reset Plan ❌"} :
                        new CharSequence[]{"Поменять план", "Полностью сбросить план ❌"};

                new AlertDialog.Builder(requireContext())
                        .setTitle(isEnglish() ? "Plan Management" : "Управление планом")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                showAiPreferencesDialog();
                            } else {
                                resetAiPlanData();
                            }
                        }).show();
            } else {
                showAiPreferencesDialog();
            }
        } else {
            showFullConstructorDialog();
        }
    }

    private void showResetAiPlanDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(isEnglish() ? "Reset Workout?" : "Сбросить тренировку?")
                .setMessage(isEnglish() ? "Are you sure you want to delete the current swim plan?" : "Вы действительно хотите удалить текущий план заплыва?")
                .setPositiveButton(isEnglish() ? "Yes, reset" : "Да, сбросить", (d, w) -> resetAiPlanData())
                .setNegativeButton(isEnglish() ? "Cancel" : "Отмена", null).show();
    }

    private void resetAiPlanData() {
        aiWarmupList.clear(); aiMainList.clear(); aiCooldownList.clear(); aiTotalDistance = 0;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("ai_total_dist", 0).putString("ai_wu", "").putString("ai_main", "").putString("ai_cd", "");
        editor.apply();

        Toast.makeText(getContext(), isEnglish() ? "Plan successfully cleared" : "План успешно очищен", Toast.LENGTH_SHORT).show();
        switchMode(0);
    }

    private void showFullConstructorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(isEnglish() ? "Workout Constructor" : "Конструктор тренировки");

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(44, 30, 44, 30);

        TextView labelWu = new TextView(getContext());
        labelWu.setText(isEnglish() ? "🏁 WARM-UP" : "🏁 РАЗМИНКА (WARM-UP)");
        labelWu.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        labelWu.setPaintFlags(labelWu.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        labelWu.setTextColor(0xFFE65100);

        final EditText etWuTask = new EditText(getContext());
        etWuTask.setHint(isEnglish() ? "Task (e.g., 200m Freestyle)" : "Задание (напр: 200м Кроль)");
        final EditText etWuMeters = new EditText(getContext());
        etWuMeters.setHint(isEnglish() ? "Meters (number)" : "Метры (число)");
        etWuMeters.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        TextView labelMain = new TextView(getContext());
        labelMain.setText(isEnglish() ? "🏊‍♂️ MAIN SET" : "🏊‍♂️ ОСНОВНАЯ СЕРИЯ (MAIN SET)");
        labelMain.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        labelMain.setPaintFlags(labelMain.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        labelMain.setTextColor(0xFF1565C0);
        labelMain.setPadding(0, 30, 0, 0);

        final EditText etMainTask = new EditText(getContext());
        etMainTask.setHint(isEnglish() ? "Task (e.g., 4x50m Breaststroke)" : "Задание (напр: 4х50м Брасс)");
        final EditText etMainMeters = new EditText(getContext());
        etMainMeters.setHint(isEnglish() ? "Meters (number)" : "Метры (число)");
        etMainMeters.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        TextView labelCd = new TextView(getContext());
        labelCd.setText(isEnglish() ? "🛑 COOL-DOWN" : "🛑 ЗАМИНКА (COOL-DOWN)");
        labelCd.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        labelCd.setPaintFlags(labelCd.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        labelCd.setTextColor(0xFF006064);
        labelCd.setPadding(0, 30, 0, 0);

        final EditText etCdTask = new EditText(getContext());
        etCdTask.setHint(isEnglish() ? "Task (e.g., 100m Relaxed swim)" : "Задание (напр: 100м Расслабленный откуп)");
        final EditText etCdMeters = new EditText(getContext());
        etCdMeters.setHint(isEnglish() ? "Meters (number)" : "Метры (число)");
        etCdMeters.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        layout.addView(labelWu);
        layout.addView(etWuTask);
        layout.addView(etWuMeters);
        layout.addView(labelMain);
        layout.addView(etMainTask);
        layout.addView(etMainMeters);
        layout.addView(labelCd);
        layout.addView(etCdTask);
        layout.addView(etCdMeters);

        scrollView.addView(layout);
        builder.setView(scrollView);

        String saveText = isEnglish() ? "Save All" : "Сохранить всё";
        String cancelText = isEnglish() ? "Cancel" : "Отмена";

        builder.setPositiveButton(saveText, (dialog, which) -> {
            constWarmupList.clear();
            constMainList.clear();
            constCooldownList.clear();
            constTotalDistance = 0;

            String wuTask = etWuTask.getText().toString().trim();
            String wuMetersStr = etWuMeters.getText().toString().trim();
            String mUnit = isEnglish() ? "m" : "м";

            if (!wuTask.isEmpty()) {
                int meters = wuMetersStr.isEmpty() ? 0 : Integer.parseInt(wuMetersStr);
                constWarmupList.add(wuTask + " (" + meters + mUnit + ")");
                constTotalDistance += meters;
            }

            String mainTask = etMainTask.getText().toString().trim();
            String mainMetersStr = etMainMeters.getText().toString().trim();
            if (!mainTask.isEmpty()) {
                int meters = mainMetersStr.isEmpty() ? 0 : Integer.parseInt(mainMetersStr);
                constMainList.add(mainTask + " (" + meters + mUnit + ")");
                constTotalDistance += meters;
            }

            String cdTask = etCdTask.getText().toString().trim();
            String cdMetersStr = etCdMeters.getText().toString().trim();
            if (!cdTask.isEmpty()) {
                int meters = cdMetersStr.isEmpty() ? 0 : Integer.parseInt(cdMetersStr);
                constCooldownList.add(cdTask + " (" + meters + mUnit + ")");
                constTotalDistance += meters;
            }

            updateScreenFields(constTotalDistance, constWarmupList, constMainList, constCooldownList);
            saveScreenState();
            toggleActionButtons(constTotalDistance > 0);
            Toast.makeText(getContext(), isEnglish() ? "Custom plan successfully assembled! 📝" : "Кастомный план успешно собран! 📝", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(cancelText, null);
        builder.show();
    }

    private void completeTrainingNow() {
        int activeDistance = (currentMode == 0) ? aiTotalDistance : constTotalDistance;
        if (activeDistance == 0) return;

        int calcTotalSeconds = Math.max(60, (activeDistance / 50) * 45);
        int mins = calcTotalSeconds / 60;
        int secs = calcTotalSeconds % 60;
        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        String fullDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        saveGlobalWorkoutData(activeDistance, formattedTime, fullDateStr, "");
        removeCalendarPlanAfterExecution(activeDistance);

        SharedPreferences.Editor editor = prefs.edit();
        if (currentMode == 0) {
            aiWarmupList.clear(); aiMainList.clear(); aiCooldownList.clear(); aiTotalDistance = 0;
            editor.putInt("ai_total_dist", 0).putString("ai_wu", "").putString("ai_main", "").putString("ai_cd", "");
        } else {
            constWarmupList.clear(); constMainList.clear(); constCooldownList.clear(); constTotalDistance = 0;
            editor.putInt("const_total_dist", 0).putString("const_wu", "").putString("const_main", "").putString("const_cd", "");
        }
        editor.apply();

        Toast.makeText(getContext(), isEnglish() ? "Plan completed! 🏆" : "План выполнен! 🏆", Toast.LENGTH_SHORT).show();
        switchMode(currentMode);
    }

    private void showAiPreferencesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(isEnglish() ? "Workout Settings" : "Настройка тренировки");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);

        final Spinner spinnerDiff = new Spinner(getContext());
        String[] diffs = isEnglish() ?
                new String[]{"Easy (Recovery)", "Medium (Normal)", "Heavy (Hardcore)"} :
                new String[]{"Легкая (Восстановление)", "Средняя (Обычная)", "Тяжелая (Хардкор)"};
        spinnerDiff.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, diffs));

        final Spinner spinnerStylePref = new Spinner(getContext());
        String[] stylePrefs = isEnglish() ?
                new String[]{"Medley (All Styles)", "Freestyle (Crawl)", "Breaststroke", "Butterfly", "Backstroke"} :
                new String[]{"Комплексный (Все стили)", "Вольный стиль (Кроль)", "Брасс", "Баттерфляй", "На спине"};
        spinnerStylePref.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, stylePrefs));

        layout.addView(spinnerDiff);
        layout.addView(spinnerStylePref);
        builder.setView(layout);

        builder.setPositiveButton(isEnglish() ? "Create Plan" : "Создать план", (dialog, which) -> {
            lastAiDiff = spinnerDiff.getSelectedItemPosition();
            lastAiStyle = spinnerStylePref.getSelectedItem().toString();
            generateCustomAiPlan(lastAiDiff, lastAiStyle);
        });
        builder.setNegativeButton(isEnglish() ? "Cancel" : "Отмена", null);
        builder.show();
    }

    private void generateCustomAiPlan(int difficulty, String swimStyle) {
        aiWarmupList.clear(); aiMainList.clear(); aiCooldownList.clear();
        int baseDistance = (difficulty + 1) * 500;
        int randomVariation = new Random().nextInt(2);
        boolean en = isEnglish();

        aiWarmupList.add(en ? "200m Warm-up (easy pace)" : "200м Разминка (легкий темп)");

        if (swimStyle.contains("Комплексный") || swimStyle.contains("Medley")) {
            if (randomVariation == 0) {
                aiMainList.add(en ? "4 x 50m Medley (change style every 50m)" : "4 x 50м Комплекс (смена стилей каждые 50м)");
                aiMainList.add(en ? "4 x 100m Freestyle/Breaststroke alternately" : "4 x 100м Кроль/Брасс поочередно");
            } else {
                aiMainList.add(en ? "200m Medley swimming for technique" : "200м Комплексное плавание на технику");
                aiMainList.add(en ? "8 x 50m Kick with board (all styles x2)" : "8 x 50м Ноги с доской (все стили по 2 раза)");
            }
        } else {
            if (randomVariation == 0) {
                aiMainList.add(en ? "4 x 100m Set: " + swimStyle + " (focus on water catch)" : "4 x 100м Серия: " + swimStyle + " (акцент на захват воды)");
                if (difficulty > 0) {
                    aiMainList.add(en ? "6 x 50m Sprint: " + swimStyle + " (max pace)" : "6 x 50м Спринт: " + swimStyle + " (макс. темп)");
                }
            } else {
                aiMainList.add(en ? "8 x 50m Main Set: " + swimStyle + " (coordination)" : "8 x 50м Основной стиль: " + swimStyle + " (координация)");
                if (difficulty > 0) {
                    aiMainList.add(en ? "2 x 200m " + swimStyle + " maintaining stroke length" : "2 x 200м " + swimStyle + " на удержание шага гребка");
                }
            }
        }

        aiCooldownList.add(en ? "100m Cool-down relaxed on back" : "100м Откупка расслабленным на спине");
        aiTotalDistance = baseDistance + (randomVariation * 100) + 200;

        updateScreenFields(aiTotalDistance, aiWarmupList, aiMainList, aiCooldownList);
        saveScreenState();
        switchMode(0);
    }

    private void updateScreenFields(int distance, ArrayList<String> wu, ArrayList<String> main, ArrayList<String> cd) {
        if (tvWorkoutDistance != null) {
            tvWorkoutDistance.setText(isEnglish() ? "Total Distance: " + distance + " m" : "Общая дистанция: " + distance + " м");
        }
        updateBlockTextView(tvBlockWarmup, wu);
        updateBlockTextView(tvBlockMain, main);
        updateBlockTextView(tvBlockCooldown, cd);
    }

    private void updateBlockTextView(TextView tv, ArrayList<String> list) {
        if (tv == null) return;
        if (list.isEmpty()) {
            if (currentMode == 1) {
                tv.setText(isEnglish() ? "• Block is empty. Press build plan button." : "• Блок пуст. Нажмите кнопку создания плана.");
            } else {
                tv.setText(isEnglish() ? "Click the button below to choose a ready plan" : "Нажми кнопку ниже, чтобы выбрать готовый план");
            }
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append("• ").append(s).append("\n");
        tv.setText(sb.toString().trim());
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

    private void openCalendarScheduler() {
        if (getContext() == null) return;

        final int activeDistance = (currentMode == 0) ? aiTotalDistance : constTotalDistance;
        if (activeDistance == 0) {
            Toast.makeText(getContext(), isEnglish() ? "Select or build a workout plan first!" : "Сначала выберите или соберите план тренировки!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
            calendar.set(Calendar.YEAR, selectedYear);
            calendar.set(Calendar.MONTH, selectedMonth);
            calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                final String[] options = isEnglish() ?
                        new String[]{"15 minutes before", "30 minutes before", "45 minutes before", "At start time"} :
                        new String[]{"За 15 минут", "За 30 минут", "За 45 минут", "В момент начала"};
                final int[] minutesValues = {15, 30, 45, 0};

                new AlertDialog.Builder(requireContext())
                        .setTitle(isEnglish() ? "How many minutes before to remind?" : "За сколько минут напомнить?")
                        .setItems(options, (dialog, which) -> {
                            int minutesBefore = minutesValues[which];
                            setupAlarms(calendar, minutesBefore, activeDistance);
                        }).show();

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePickerDialog.show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void startTimer() { if (!running) { startTime = SystemClock.uptimeMillis(); handler.postDelayed(timerRunnable, 0); running = true; } }
    private void stopTimer() { if (running) { timeSwapBuff += timeInMilliseconds; handler.removeCallbacks(timerRunnable); running = false; } }
    private void resetTimer() { stopTimer(); startTime = 0L; updateTime = 0L; timeSwapBuff = 0L; timeInMilliseconds = 0L; if (tvTimer != null) tvTimer.setText("00:00:00"); }

    private final Runnable timerRunnable = new Runnable() {
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