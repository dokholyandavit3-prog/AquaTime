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
import java.util.HashSet;
import java.util.Locale;
import david.dokholyan.aquatime.R;

public class EditProfileFragment extends Fragment {

    private EditText etFirst, etLast, etHeight, etWeight, etAchievements;
    private AutoCompleteTextView spinStyle, spinNation;
    private SharedPreferences prefs;
    private boolean isEnglish;

    private final String[] styleKeys = {"freestyle", "breaststroke", "butterfly", "backstroke", "medley"};
    private final ArrayList<String> countryIsoKeys = new ArrayList<>();
    private final ArrayList<String> countryDisplayNames = new ArrayList<>();
    private String[] styleDisplayNames;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        isEnglish = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

        initFields(view);
        setupSpinners();
        loadCurrentData();

        view.findViewById(R.id.btn_save).setOnClickListener(this::saveAndExit);
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
        if (getContext() == null) return;

        // 1. Настройка стилей плавания
        styleDisplayNames = isEnglish ?
                new String[]{"Freestyle", "Breaststroke", "Butterfly", "Backstroke", "Individual Medley"} :
                new String[]{"Вольный стиль", "Брасс", "Баттерфляй", "На спине", "Комплекс"};

        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, styleDisplayNames);
        spinStyle.setAdapter(styleAdapter);


        ArrayList<CountryItem> countryList = new ArrayList<>();
        HashSet<String> addedCodes = new HashSet<>();
        Locale displayLocale = isEnglish ? Locale.ENGLISH : new Locale("ru");


        for (String code : Locale.getISOCountries()) {
            if (!addedCodes.contains(code)) {
                addedCodes.add(code);

                Locale locale = new Locale("", code);
                String name = locale.getDisplayCountry(displayLocale);


                if (!name.isEmpty() && !name.equals(code)) {
                    countryList.add(new CountryItem(code, name));
                }
            }
        }


        Collections.sort(countryList, (c1, c2) -> c1.displayName.compareToIgnoreCase(c2.displayName));

        countryDisplayNames.clear();
        countryIsoKeys.clear();

        for (CountryItem item : countryList) {
            countryIsoKeys.add(item.isoCode);
            countryDisplayNames.add(item.displayName);
        }

        ArrayAdapter<String> nationAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, countryDisplayNames);
        spinNation.setAdapter(nationAdapter);
    }

    private void loadCurrentData() {
        etFirst.setText(prefs.getString("firstName", ""));
        etLast.setText(prefs.getString("lastName", ""));
        etHeight.setText(prefs.getString("height", ""));
        etWeight.setText(prefs.getString("weight", ""));

        if (isEnglish) {
            etAchievements.setText(prefs.getString("achievements_en", ""));
        } else {
            etAchievements.setText(prefs.getString("achievements_ru", ""));
        }

        String savedStyleKey = prefs.getString("style_key", "freestyle");
        String savedCountryIso = prefs.getString("nation_iso", "");


        for (int i = 0; i < styleKeys.length; i++) {
            if (styleKeys[i].equals(savedStyleKey)) {
                spinStyle.setText(styleDisplayNames[i], false);
                break;
            }
        }


        if (!savedCountryIso.isEmpty()) {
            int countryPosition = countryIsoKeys.indexOf(savedCountryIso);
            if (countryPosition >= 0) {
                spinNation.setText(countryDisplayNames.get(countryPosition), false);
            }
        }
    }

    private void saveAndExit(View v) {
        String styleDisplay = spinStyle.getText().toString();
        String countryDisplay = spinNation.getText().toString();


        int selectedStylePos = -1;
        for (int i = 0; i < styleDisplayNames.length; i++) {
            if (styleDisplayNames[i].equals(styleDisplay)) {
                selectedStylePos = i;
                break;
            }
        }


        int selectedCountryPos = countryDisplayNames.indexOf(countryDisplay);

        String styleKeyToSave = (selectedStylePos >= 0) ? styleKeys[selectedStylePos] : "freestyle";
        String countryIsoToSave = (selectedCountryPos >= 0) ? countryIsoKeys.get(selectedCountryPos) : "";

        SharedPreferences.Editor editor = prefs.edit()
                .putString("firstName", etFirst.getText().toString())
                .putString("lastName", etLast.getText().toString())
                .putString("height", etHeight.getText().toString())
                .putString("weight", etWeight.getText().toString())
                .putString("style_key", styleKeyToSave)
                .putString("nation_iso", countryIsoToSave)
                .putString("style", styleDisplay)
                .putString("nation", countryDisplay);

        String enteredAchievements = etAchievements.getText().toString();
        if (isEnglish) {
            editor.putString("achievements_en", enteredAchievements);
        } else {
            editor.putString("achievements_ru", enteredAchievements);
        }

        editor.apply();

        String message = isEnglish ? "Profile updated" : "Профиль обновлен";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        Navigation.findNavController(v).popBackStack();
    }

    private static class CountryItem {
        String isoCode;
        String displayName;

        CountryItem(String isoCode, String displayName) {
            this.isoCode = isoCode;
            this.displayName = displayName;
        }
    }
}