package david.dokholyan.aquatime.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import david.dokholyan.aquatime.R;

public class AnalyticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        TextView totalDistance = view.findViewById(R.id.tv_total_distance);
        TextView avgSpeed = view.findViewById(R.id.tv_avg_speed);
        TextView totalTime = view.findViewById(R.id.tv_total_time);
        TextView historyList = view.findViewById(R.id.tv_history_list);
        TextView plannerList = view.findViewById(R.id.tv_planner_list);
        EditText trainingGoal = view.findViewById(R.id.et_training_goal);
        Button addTraining = view.findViewById(R.id.btn_add_training);
        Button openHistory = view.findViewById(R.id.btn_open_history);
        Button openChampions = view.findViewById(R.id.btn_open_champions);

        // Загружаем статистику из SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("training_data", getContext().MODE_PRIVATE);
        int distance = prefs.getInt("distance", 0);
        int time = prefs.getInt("time", 0);
        float speed = prefs.getFloat("speed", 0f);

        totalDistance.setText(distance + " м");
        totalTime.setText(String.format("%02d:%02d", time / 60, time % 60));
        avgSpeed.setText(String.format("%.2f м/с", speed));

        // Короткая история (короткий блок)
        String history = prefs.getString("history", "Пока нет завершённых тренировок");
        historyList.setText(history);

        // Планировщик
        String planner = prefs.getString("planner", "Нет запланированных тренировок");
        plannerList.setText(planner);

        addTraining.setOnClickListener(v -> {
            String goal = trainingGoal.getText().toString().trim();
            if (!goal.isEmpty()) {
                String existing = prefs.getString("planner", "");
                String updated = existing + "• " + goal + "\n";
                prefs.edit().putString("planner", updated).apply();
                plannerList.setText(updated);
                trainingGoal.setText("");
            }
        });

        // Переход на отдельный экран Истории (через NavController)
        openHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.historyFragment)
        );

        // Переход на отдельный экран Советов чемпионов (через NavController)
        openChampions.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.championsFragment)
        );

        return view;
    }
}
