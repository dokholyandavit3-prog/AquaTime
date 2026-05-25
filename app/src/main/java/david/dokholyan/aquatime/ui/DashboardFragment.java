package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import david.dokholyan.aquatime.R;

public class DashboardFragment extends Fragment {

    private SharedPreferences prefs;
    private TextView tvGreeting, tvStreakText, tvBestStreakText, tvDailyTip;
    private TextView tvLastDist, tvLastDate, tvWeeklyStatus;
    private TextView tvFavoritePool;


    private CardView cardMyPool, cardLastWorkout;

    private ProgressBar ringDist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(v);
        setupGreeting();
        updateStreakLogic();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void initViews(View v) {
        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvStreakText = v.findViewById(R.id.tv_streak_count_text);
        tvBestStreakText = v.findViewById(R.id.tv_best_streak_text);
        tvFavoritePool = v.findViewById(R.id.tv_favorite_pool_dashboard);

        ringDist = v.findViewById(R.id.ring_dist);
        tvDailyTip = v.findViewById(R.id.tv_daily_tip_text);
        tvWeeklyStatus = v.findViewById(R.id.tv_weekly_status_text);
        tvLastDist = v.findViewById(R.id.tv_last_distance);
        tvLastDate = v.findViewById(R.id.tv_last_date);


        cardMyPool = v.findViewById(R.id.card_my_pool);
        cardLastWorkout = v.findViewById(R.id.card_last_workout);

        Button btnToWorkout = v.findViewById(R.id.btn_to_workout);
        Button btnToAnalytics = v.findViewById(R.id.btn_to_analytics);
        FloatingActionButton btnOpenMap = v.findViewById(R.id.btn_open_map);

        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(view -> {
                try {
                    Navigation.findNavController(view).navigate(R.id.poolsMapFragment);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            });
        }


        if (cardMyPool != null) {
            cardMyPool.setOnLongClickListener(view -> {
                try {
                    Navigation.findNavController(view).navigate(R.id.poolsMapFragment);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                return true;
            });
        }


        if (cardLastWorkout != null) {
            cardLastWorkout.setOnLongClickListener(view -> {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                    if (nav != null) {

                        nav.setSelectedItemId(R.id.analyticsFragment);
                    }
                }
                return true;
            });
        }



        if (btnToWorkout != null) {
            btnToWorkout.setOnClickListener(view -> {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                    if (nav != null) {
                        nav.setSelectedItemId(R.id.trainingFragment);
                    }
                }
            });
        }

        if (btnToAnalytics != null) {
            btnToAnalytics.setOnClickListener(view -> {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                    if (nav != null) {
                        nav.setSelectedItemId(R.id.analyticsFragment);
                    }
                }
            });
        }
    }

    private void updateStreakLogic() {
        long lastVisitDay = prefs.getLong("last_visit_day", 0);
        int currentStreak = prefs.getInt("current_streak", 0);
        int bestStreak = prefs.getInt("best_streak", 0);

        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        if (lastVisitDay == 0) {
            currentStreak = 1;
        } else if (today == lastVisitDay) {

        } else if (today == lastVisitDay + 1) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }

        prefs.edit()
                .putLong("last_visit_day", today)
                .putInt("current_streak", currentStreak)
                .putInt("best_streak", bestStreak)
                .apply();
    }

    private void updateUI() {
        int streak = prefs.getInt("current_streak", 0);
        int bestStreak = prefs.getInt("best_streak", 0);

        if (tvStreakText != null) {
            tvStreakText.setText(getString(R.string.streak_days_count, streak));
        }

        if (tvBestStreakText != null) {
            tvBestStreakText.setText(getString(R.string.streak_record, bestStreak));
        }

        if (tvFavoritePool != null) {
            String defaultText = getString(R.string.dashboard_no_pool_selected);
            String favoritePoolName = prefs.getString("FavoritePool", defaultText);
            tvFavoritePool.setText(favoritePoolName);
        }

        String data = prefs.getString("all_res", "");
        int weeklyMeters = 0;

        if (!data.isEmpty()) {
            String[] entries = data.split(";");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Calendar now = Calendar.getInstance();
            int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
            int currentYear = now.get(Calendar.YEAR);

            for (String entry : entries) {
                try {
                    String[] p = entry.split("\\|");
                    int distance = Integer.parseInt(p[0].trim());
                    Date date = sdf.parse(p[2].trim());

                    Calendar cal = Calendar.getInstance();
                    if (date != null) {
                        cal.setTime(date);
                        if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(Calendar.YEAR) == currentYear) {
                            weeklyMeters += distance;
                        }
                    }
                } catch (Exception ignored) {}
            }
            prefs.edit().putInt("weekly_meters", weeklyMeters).apply();
        } else {
            weeklyMeters = prefs.getInt("weekly_meters", 0);
        }

        final int distGoal = 5000;
        final int finalWeeklyMeters = weeklyMeters;


        if (ringDist != null && distGoal > 0) {
            int progressValue = (finalWeeklyMeters * 100) / distGoal;
            if (progressValue > 100) {
                progressValue = 100;
            }
            ringDist.setProgress(progressValue);

            ringDist.setOnLongClickListener(view -> {
                showWeeklyOverflowDialog(finalWeeklyMeters, distGoal);
                return true;
            });
        }

        if (tvWeeklyStatus != null) {
            try {
                tvWeeklyStatus.setText(getString(R.string.weekly_goals_progress, finalWeeklyMeters, distGoal));
            } catch (Exception e) {
                tvWeeklyStatus.setText(finalWeeklyMeters + " из " + distGoal + " м пройдено");
            }
        }

        int lastDistance = prefs.getInt("last_completed_distance", 0);
        String lastDate = prefs.getString("last_completed_date", "00.00.2026");

        if (tvLastDist != null) {
            tvLastDist.setText(lastDistance + " м");
        }
        if (tvLastDate != null) {
            tvLastDate.setText(lastDate);
        }

        if (tvDailyTip != null) {
            tvDailyTip.setText(getString(R.string.default_coach_advice));
        }
    }

    private void showWeeklyOverflowDialog(int totalMeters, int goal) {
        if (getContext() == null) return;

        boolean isEnglish = getString(R.string.nav_home).equals("Home");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_weekly_overflow, null);

        ProgressBar dialogRingBase = dialogView.findViewById(R.id.dialog_ring_base);
        ProgressBar dialogRingOverflow = dialogView.findViewById(R.id.dialog_ring_overflow);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvDialogBaseTitle = dialogView.findViewById(R.id.tv_dialog_base_title);
        TextView tvDialogOverflowTitle = dialogView.findViewById(R.id.tv_dialog_overflow_title);

        TextView tvBaseStatus = dialogView.findViewById(R.id.tv_dialog_base_status);
        TextView tvOverflowStatus = dialogView.findViewById(R.id.tv_dialog_overflow_status);
        View btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        if (tvDialogTitle != null) {
            tvDialogTitle.setText(isEnglish ? "Goal Overflow!" : "Перевыполнение цели!");
        }
        if (tvDialogBaseTitle != null) {
            tvDialogBaseTitle.setText(isEnglish ? "Main Goal" : "Основная цель");
        }

        if (totalMeters >= goal) {
            if (dialogRingBase != null) dialogRingBase.setProgress(100);

            if (tvBaseStatus != null) {
                tvBaseStatus.setText(isEnglish ?
                        goal + " of " + goal + " m (Completed!)" :
                        goal + " из " + goal + " м (Выполнено!)");
            }

            int extraMeters = totalMeters - goal;
            int currentBonusLevel = (extraMeters / goal) + 1;
            int metersInCurrentLevel = extraMeters % goal;

            if (metersInCurrentLevel == 0 && extraMeters > 0) {
                metersInCurrentLevel = goal;
                currentBonusLevel--;
            }

            int overflowPercent = (metersInCurrentLevel * 100) / goal;
            if (dialogRingOverflow != null) dialogRingOverflow.setProgress(overflowPercent);

            if (tvDialogOverflowTitle != null) {
                tvDialogOverflowTitle.setText(isEnglish ?
                        "Bonus Level " + currentBonusLevel :
                        "Дополнительный уровень " + currentBonusLevel);
            }

            if (tvOverflowStatus != null) {
                tvOverflowStatus.setText(isEnglish ?
                        metersInCurrentLevel + " of " + goal + " m" :
                        metersInCurrentLevel + " из " + goal + " м");
            }
        } else {
            int basePercent = (totalMeters * 100) / goal;
            if (dialogRingBase != null) dialogRingBase.setProgress(basePercent);

            if (tvBaseStatus != null) {
                tvBaseStatus.setText(isEnglish ?
                        totalMeters + " of " + goal + " m" :
                        totalMeters + " из " + goal + " м");
            }

            if (dialogRingOverflow != null) dialogRingOverflow.setProgress(0);
            if (tvDialogOverflowTitle != null) {
                tvDialogOverflowTitle.setText(isEnglish ? "Bonus Level 1" : "Дополнительный уровень 1");
            }
            if (tvOverflowStatus != null) {
                tvOverflowStatus.setText(isEnglish ? "0 of " + goal + " m" : "0 из " + goal + " м");
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v1 -> dialog.dismiss());
        }

        dialog.show();
    }

    private void setupGreeting() {
        if (tvGreeting == null) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isEnglish = getString(R.string.nav_home).equals("Home");

        if (hour < 12) {
            tvGreeting.setText(isEnglish ? "Good morning! 🌅" : "Доброе утро! 🌅");
        } else if (hour < 18) {
            tvGreeting.setText(isEnglish ? "Good afternoon! 🌊" : "Добрый день! 🌊");
        } else {
            tvGreeting.setText(isEnglish ? "Good evening! 🌙" : "Добрый вечер! 🌙");
        }
    }
}