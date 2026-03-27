package david.dokholyan.aquatime.ui.profile;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import david.dokholyan.aquatime.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvDetails,tvEmail;
    private TextView tvAchievements, tvLevel, tvRating;
    private TextView tvFreestyle, tvBreast, tvFly, tvBack;
    private TextView tvMainRating;

    private ImageView imgAvatar;
    private SharedPreferences prefs;

    private ActivityResultLauncher<String> imagePicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tv_name);
        tvDetails = view.findViewById(R.id.tv_age_weight);
        tvEmail = view.findViewById(R.id.tv_email);
        tvAchievements = view.findViewById(R.id.tv_achievements);
        tvLevel = view.findViewById(R.id.tv_level);
        tvRating = view.findViewById(R.id.tv_rating);
        tvMainRating = view.findViewById(R.id.tv_main_rating);

        imgAvatar = view.findViewById(R.id.img_avatar);

        tvFreestyle = view.findViewById(R.id.tv_freestyle);
        tvBreast = view.findViewById(R.id.tv_breast);
        tvFly = view.findViewById(R.id.tv_fly);
        tvBack = view.findViewById(R.id.tv_back);

        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        prefs.edit().putString("avatar_uri", uri.toString()).apply();
                        loadProfile();
                        Toast.makeText(getContext(), "Фото обновлено", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        Button btnEdit = view.findViewById(R.id.btn_edit_profile);

        btnEdit.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.editProfileFragment);
        });
        loadProfile();

        imgAvatar.setOnClickListener(v -> showAvatarMenu());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {

        String firstName = prefs.getString("firstName", "Имя не указано");
        String lastName = prefs.getString("lastName", "");
        String height = prefs.getString("height", "-");
        String weight = prefs.getString("weight", "-");
        String style = prefs.getString("style", "Не выбран");
        String nation = prefs.getString("nation", "");
        String achievements = prefs.getString("achievements", "Нет достижений");
        String email = prefs.getString("user_email", "не указан");

        tvName.setText("👤 " + firstName + " " + lastName + " " + nation);

        tvDetails.setText(
                "📏 Рост: " + height + " см | ⚖ Вес: " + weight + " кг\n" +
                        "🏊 Стиль: " + style
        );

        tvFreestyle.setText("🏊 Кроль: " + calculateStyleRating("freestyle"));
        tvBreast.setText("🏊 Брасс: " + calculateStyleRating("breast"));
        tvFly.setText("🏊 Баттерфляй: " + calculateStyleRating("fly"));
        tvBack.setText("🏊‍♂️ Спина: " + calculateStyleRating("back"));

        tvAchievements.setText(achievements);
        tvEmail.setText("📧 " + email);

        calculateLevelAndRating();

        String uri = prefs.getString("avatar_uri", null);if (uri != null) {
            imgAvatar.setImageURI(Uri.parse(uri));
        } else {
            String avatar = prefs.getString("avatar", "avatar1");
            switch (avatar) {
                case "avatar1": imgAvatar.setImageResource(R.drawable.avatar_swim_1); break;
                case "avatar2": imgAvatar.setImageResource(R.drawable.avatar_swim_2); break;
                case "avatar3": imgAvatar.setImageResource(R.drawable.avatar_swim_3); break;
                case "avatar4": imgAvatar.setImageResource(R.drawable.avatar_swim_4); break;
            }
        }
    }

    private String calculateStyleRating(String styleKey) {
        int meters = prefs.getInt(styleKey + "_meters", 0);
        int trainings = prefs.getInt(styleKey + "_trainings", 0);

        int rating = (trainings * 5) + (meters / 100);
        if (rating > 99) rating = 99;

        String level;
        if (rating < 30) level = "Beginner";
        else if (rating < 60) level = "Amateur";
        else if (rating < 80) level = "Pro";
        else level = "Elite";

        return rating + " (" + level + ")";
    }

    private void calculateLevelAndRating() {

        int totalMeters = prefs.getInt("total_meters", 0);
        int trainings = prefs.getInt("trainings_count", 0);

        int rating = (trainings * 5) + (totalMeters / 100);
        if (rating > 99) rating = 99;

        String level;
        if (rating < 30) level = "Beginner 🟢";
        else if (rating < 60) level = "Amateur 🔵";
        else if (rating < 80) level = "Pro 🟣";
        else level = "Elite 🔴";

        tvRating.setText("⭐ Рейтинг: " + rating);
        tvLevel.setText("🏆 Уровень: " + level);

        // 🔥 ГЛАВНЫЙ РЕЙТИНГ + АНИМАЦИЯ
        tvMainRating.setText(rating + "/100");

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvMainRating, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvMainRating, "scaleY", 0.5f, 1f);

        scaleX.setDuration(500);
        scaleY.setDuration(500);

        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());

        scaleX.start();
        scaleY.start();
    }

    private void showAvatarMenu() {
        String[] options = {"🎭 Выбрать аватар", "📸 Загрузить фото"};

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Аватар")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) chooseAvatar();
                    else imagePicker.launch("image/*");
                })
                .show();
    }

    private void chooseAvatar() {

        String[] avatars = {"Аватар 1", "Аватар 2", "Аватар 3", "Аватар 4"};

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Выбери аватар")
                .setItems(avatars, (dialog, which) -> {

                    String selected = "avatar" + (which + 1);

                    prefs.edit()
                            .putString("avatar", selected)
                            .remove("avatar_uri")
                            .apply();

                    loadProfile();
                })
                .show();
    }
}