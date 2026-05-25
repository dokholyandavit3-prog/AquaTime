package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import david.dokholyan.aquatime.R;

public class EditProfileFragment extends Fragment {

    private EditText etFirst, etLast, etHeight, etWeight, etAchievements;
    private Spinner spinStyle, spinNation;
    private SharedPreferences prefs;
    private boolean isEnglish;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        isEnglish = getString(R.string.nav_home).equals("Home");

        initFields(view);
        setupSpinners();
        loadCurrentData();

        view.findViewById(R.id.btn_save).setOnClickListener(v -> saveAndExit(v));
        view.findViewById(R.id.btn_back).setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        return view;
    }

    private void initFields(View v) {
        etFirst = v.findViewById(R.id.input_first_name);
        etLast = v.findViewById(R.id.input_last_name);
        etHeight = v.findViewById(R.id.input_height);
        etWeight = v.findViewById(R.id.input_weight);
        etAchievements = v.findViewById(R.id.input_achievements);
        spinStyle = v.findViewById(R.id.spinner_style);
        spinNation = v.findViewById(R.id.spinner_nation);
    }

    private void setupSpinners() {
        String[] styles = isEnglish ?
                new String[]{"Freestyle", "Breaststroke", "Butterfly", "Backstroke", "Individual Medley"} :
                new String[]{"Кроль", "Брасс", "Баттерфляй", "Спина", "Комплекс"};

        if (getContext() != null) {
            spinStyle.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, styles));

            ArrayList<String> countries = new ArrayList<>();
            Locale targetLocale = isEnglish ? Locale.ENGLISH : new Locale("ru");

            for (String code : Locale.getISOCountries()) {
                Locale l = new Locale("", code);
                countries.add(l.getDisplayCountry(targetLocale));
            }
            Collections.sort(countries);
            spinNation.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, countries));
        }
    }

    private void loadCurrentData() {
        etFirst.setText(prefs.getString("firstName", ""));
        etLast.setText(prefs.getString("lastName", ""));
        etHeight.setText(prefs.getString("height", ""));
        etWeight.setText(prefs.getString("weight", ""));
        etAchievements.setText(prefs.getString("achievements", ""));

        String savedStyle = prefs.getString("style", "");
        String savedNation = prefs.getString("nation", "");

        if (!savedStyle.isEmpty() && spinStyle.getAdapter() != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinStyle.getAdapter();
            int pos = adapter.getPosition(savedStyle);
            if (pos >= 0) spinStyle.setSelection(pos);
        }

        if (!savedNation.isEmpty() && spinNation.getAdapter() != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinNation.getAdapter();
            int pos = adapter.getPosition(savedNation);
            if (pos >= 0) spinNation.setSelection(pos);
        }
    }

    private void saveAndExit(View v) {
        prefs.edit()
                .putString("firstName", etFirst.getText().toString())
                .putString("lastName", etLast.getText().toString())
                .putString("height", etHeight.getText().toString())
                .putString("weight", etWeight.getText().toString())
                .putString("style", spinStyle.getSelectedItem().toString())
                .putString("nation", spinNation.getSelectedItem().toString())
                .putString("achievements", etAchievements.getText().toString())
                .apply();

        String message = isEnglish ? "Profile updated" : "Профиль обновлен";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(v).popBackStack();
    }
}