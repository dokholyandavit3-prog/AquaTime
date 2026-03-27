package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.Locale;

import david.dokholyan.aquatime.R;

public class EditProfileFragment extends Fragment {

    private EditText firstName, lastName, height, weight, achievements;
    private Spinner spinnerStyle, spinnerNation;
    private TextView tvEmail;

    private String selectedAvatar = "avatar1";

    private ActivityResultLauncher<String> imagePicker;

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        Context context = requireActivity();
        prefs = context.getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        tvEmail = view.findViewById(R.id.tv_email);
        firstName = view.findViewById(R.id.input_first_name);
        lastName = view.findViewById(R.id.input_last_name);
        height = view.findViewById(R.id.input_height);
        weight = view.findViewById(R.id.input_weight);
        achievements = view.findViewById(R.id.input_achievements);

        spinnerStyle = view.findViewById(R.id.spinner_style);
        spinnerNation = view.findViewById(R.id.spinner_nation);

        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnBack = view.findViewById(R.id.btn_back);
        Button btnAvatar = view.findViewById(R.id.btn_choose_avatar);
        Button btnPickImage = view.findViewById(R.id.btn_pick_image);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // 📧 EMAIL
        tvEmail.setText("📧 " + prefs.getString("user_email", "не указан"));

        // 🏊 СТИЛИ
        String[] styles = {"Кроль", "Брасс", "Баттерфляй", "Спина"};
        spinnerStyle.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, styles));

        // 🌍 СТРАНЫ
        ArrayList<String> countries = new ArrayList<>();
        String[] iso = Locale.getISOCountries();

        for (String code : iso) {
            Locale locale = new Locale("", code);
            countries.add(getFlagEmoji(code) + " " + locale.getDisplayCountry());
        }

        spinnerNation.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, countries));

        // 📸 ГАЛЕРЕЯ
        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        prefs.edit().putString("avatar_uri", uri.toString()).apply();
                        Toast.makeText(getContext(), "Фото выбрано", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 🎭 выбор аватара
        btnAvatar.setOnClickListener(v -> {
            String[] avatars = {"Аватар 1", "Аватар 2", "Аватар 3", "Аватар 4"};

            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Выбери аватар")
                    .setItems(avatars, (dialog, which) -> {selectedAvatar = "avatar" + (which + 1);
                        prefs.edit().remove("avatar_uri").apply(); // убираем фото если было
                        Toast.makeText(getContext(), "Аватар выбран", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        // 📸 загрузка фото
        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        // 📥 загрузка данных
        firstName.setText(prefs.getString("firstName", ""));
        lastName.setText(prefs.getString("lastName", ""));
        height.setText(prefs.getString("height", ""));
        weight.setText(prefs.getString("weight", ""));
        achievements.setText(prefs.getString("achievements", ""));
        selectedAvatar = prefs.getString("avatar", "avatar1");

        // 💾 сохранить
        btnSave.setOnClickListener(v -> {

            prefs.edit()
                    .putString("firstName", firstName.getText().toString())
                    .putString("lastName", lastName.getText().toString())
                    .putString("height", height.getText().toString())
                    .putString("weight", weight.getText().toString())
                    .putString("style", spinnerStyle.getSelectedItem().toString())
                    .putString("nation", spinnerNation.getSelectedItem().toString())
                    .putString("achievements", achievements.getText().toString())
                    .putString("avatar", selectedAvatar)
                    .apply();

            navController.popBackStack();
        });

        btnBack.setOnClickListener(v -> navController.popBackStack());

        return view;
    }

    private String getFlagEmoji(String countryCode) {
        int first = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int second = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}