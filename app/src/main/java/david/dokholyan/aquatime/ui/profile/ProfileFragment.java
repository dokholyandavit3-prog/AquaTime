package david.dokholyan.aquatime.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import david.dokholyan.aquatime.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvDetails, tvNextWorkout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Находим все элементы
        tvName = view.findViewById(R.id.tv_name);
        tvDetails = view.findViewById(R.id.tv_age_weight);
        tvNextWorkout = view.findViewById(R.id.tv_next_workout);
        Button btnStartWorkout = view.findViewById(R.id.btn_start_next_workout);
        Button btnEdit = view.findViewById(R.id.btn_edit_profile);
        Button btnAchievements = view.findViewById(R.id.btn_achievements);

        // Загружаем данные из SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_data", getContext().MODE_PRIVATE);

        String firstName = prefs.getString("firstName", "Имя не указано");
        String lastName = prefs.getString("lastName", "");
        String height = prefs.getString("height", "-");
        String weight = prefs.getString("weight", "-");
        String style = prefs.getString("style", "Не выбран");
        String nation = prefs.getString("nation", "");
        String achievements = prefs.getString("achievements", "Нет достижений");

        // Отображаем данные
        tvName.setText(firstName + " " + lastName + " " + nation);
        tvDetails.setText("Рост: " + height + " см | Вес: " + weight + " кг\n"
                + "Стиль: " + style + "\n"
                + "🏆 " + achievements);

        // Пример данных следующей тренировки
        tvNextWorkout.setText("Вольный стиль, 1000 м, 16:00");

        // Навигация
        NavController navController = NavHostFragment.findNavController(this);

        // Редактирование профиля
        btnEdit.setOnClickListener(v -> navController.navigate(R.id.editProfileFragment));

        // Достижения
        btnAchievements.setOnClickListener(v -> navController.navigate(R.id.achievementsFragment));

        // Начать тренировку → переходим на вкладку “Тренировка”
        btnStartWorkout.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.nav_view);
            bottomNav.setSelectedItemId(R.id.trainingFragment);
        });

        return view;
    }
}
