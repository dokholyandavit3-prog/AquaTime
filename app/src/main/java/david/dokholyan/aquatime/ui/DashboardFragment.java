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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Calendar;
import david.dokholyan.aquatime.R;

public class DashboardFragment extends Fragment {

    private SharedPreferences prefs;
    private TextView tvGreeting, tvStreakText, tvDailyTip;
    private TextView tvLastDist, tvLastDate, tvWeeklyStatus;
    private ProgressBar ringDist, ringCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(v);
        setupGreeting();
        updateUI();

        return v;
    }

    private void initViews(View v) {
        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvStreakText = v.findViewById(R.id.tv_streak_count_text);
        ringDist = v.findViewById(R.id.ring_dist);
        ringCount = v.findViewById(R.id.ring_count);
        tvDailyTip = v.findViewById(R.id.tv_daily_tip_text);
        tvWeeklyStatus = v.findViewById(R.id.tv_weekly_status_text);
        tvLastDist = v.findViewById(R.id.tv_last_distance);
        tvLastDate = v.findViewById(R.id.tv_last_date);

        Button btnToWorkout = v.findViewById(R.id.btn_to_workout);
        Button btnToAnalytics = v.findViewById(R.id.btn_to_analytics);


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

    private void updateUI() {
        int streak = prefs.getInt("current_streak", 0);
        tvStreakText.setText(streak + " " + getDayAddition(streak) + " подряд");

        int weeklyMeters = prefs.getInt("weekly_meters", 0);
        int weeklyCount = prefs.getInt("weekly_count", 0);
        int distGoal = 5000;
        int countGoal = 4;

        ringDist.setProgress(distGoal > 0 ? (weeklyMeters * 100) / distGoal : 0);
        ringCount.setProgress(countGoal > 0 ? (weeklyCount * 100) / countGoal : 0);
        tvWeeklyStatus.setText(weeklyMeters + " из " + distGoal + " м пройдено");

        tvLastDist.setText(prefs.getInt("last_dist", 0) + " м");
        tvLastDate.setText(prefs.getString("last_date", "00.00.26"));

        tvDailyTip.setText("Для лучшего скольжения держи ладонь плоской, но не напряженной.");
    }

    private String getDayAddition(int num) {
        if (num % 10 == 1 && num % 100 != 11) return "день";
        if (num % 10 >= 2 && num % 10 <= 4 && (num % 100 < 10 || num % 100 >= 20)) return "дня";
        return "дней";
    }

    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(hour < 12 ? "Доброе утро! 🌅" : hour < 18 ? "Добрый день! 🌊" : "Добрый вечер! 🌙");
    }
}