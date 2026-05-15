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
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import david.dokholyan.aquatime.R;

public class DashboardFragment extends Fragment {

    private SharedPreferences prefs;
    private TextView tvGreeting, tvStreakText, tvBestStreakText, tvDailyTip;
    private TextView tvLastDist, tvLastDate, tvWeeklyStatus;
    private TextView tvFavoritePool; // Новый TextView для любимого бассейна
    private ProgressBar ringDist, ringCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Используем единое имя файла "AquaTime"
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(v);
        setupGreeting();
        updateStreakLogic();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // При каждом возвращении на экран принудительно обновляем все текстовые поля и прогресс
        updateUI();
    }

    private void initViews(View v) {
        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvStreakText = v.findViewById(R.id.tv_streak_count_text);
        tvBestStreakText = v.findViewById(R.id.tv_best_streak_text);

        // Инициализируем наше новое текстовое поле
        tvFavoritePool = v.findViewById(R.id.tv_favorite_pool_dashboard);

        ringDist = v.findViewById(R.id.ring_dist);
        ringCount = v.findViewById(R.id.ring_count);
        tvDailyTip = v.findViewById(R.id.tv_daily_tip_text);
        tvWeeklyStatus = v.findViewById(R.id.tv_weekly_status_text);
        tvLastDist = v.findViewById(R.id.tv_last_distance);
        tvLastDate = v.findViewById(R.id.tv_last_date);

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

        btnToWorkout.setOnClickListener(view -> {
            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.trainingFragment);
                }
            }
        });

        btnToAnalytics.setOnClickListener(view -> {
            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.nav_view);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.analyticsFragment);
                }
            }
        });
    }

    private void updateStreakLogic() {
        long lastVisitDay = prefs.getLong("last_visit_day", 0);
        int currentStreak = prefs.getInt("current_streak", 0);
        int bestStreak = prefs.getInt("best_streak", 0);

        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

        if (lastVisitDay == 0) {
            currentStreak = 1;
        } else if (today == lastVisitDay) {
            // Тот же день
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

        tvStreakText.setText(getString(R.string.streak_days_count, streak));

        if (tvBestStreakText != null) {
            tvBestStreakText.setText(getString(R.string.streak_record, bestStreak));
        }


        if (tvFavoritePool != null) {
            String defaultText = getString(R.string.dashboard_no_pool_selected);
            String favoritePoolName = prefs.getString("FavoritePool", defaultText);
            tvFavoritePool.setText(favoritePoolName);
        }

        int weeklyMeters = prefs.getInt("weekly_meters", 0);
        int weeklyCount = prefs.getInt("weekly_count", 0);
        int distGoal = 5000;
        int countGoal = 4;

        ringDist.setProgress(distGoal > 0 ? (weeklyMeters * 100) / distGoal : 0);
        ringCount.setProgress(countGoal > 0 ? (weeklyCount * 100) / countGoal : 0);

        tvWeeklyStatus.setText(getString(R.string.weekly_goals_progress, weeklyMeters, distGoal));

        tvLastDist.setText(prefs.getInt("last_dist", 0) + " м");
        tvLastDate.setText(prefs.getString("last_date", "00.00.26"));

        tvDailyTip.setText(getString(R.string.default_coach_advice));
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            tvGreeting.setText(getString(R.string.nav_home).equals("Home") ? "Good morning! 🌅" : "Доброе утро! 🌅");
        } else if (hour < 18) {
            tvGreeting.setText(getString(R.string.nav_home).equals("Home") ? "Good afternoon! 🌊" : "Добрый день! 🌊");
        } else {
            tvGreeting.setText(getString(R.string.nav_home).equals("Home") ? "Good evening! 🌙" : "Добрый вечер! 🌙");
        }
    }
}