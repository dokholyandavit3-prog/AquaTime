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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import david.dokholyan.aquatime.R;

public class EditProfileFragment extends Fragment {

    private EditText etFirst, etLast, etHeight, etWeight, etAchievements;
    private AutoCompleteTextView spinStyle, spinNation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUid = currentUser.getUid();
        }

        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        isEnglish = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

        initFields(view);
        setupSpinners();

        if (currentUid != null) {
            loadCurrentDataFromFirestore();
        } else {
            Toast.makeText(getContext(), isEnglish ? "User not authorized" : "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
        }

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

    private void loadCurrentDataFromFirestore() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        etFirst.setText(documentSnapshot.getString("firstName"));
                        etLast.setText(documentSnapshot.getString("lastName"));
                        etHeight.setText(documentSnapshot.getString("height"));
                        etWeight.setText(documentSnapshot.getString("weight"));

                        if (isEnglish) {
                            etAchievements.setText(documentSnapshot.getString("achievements_en"));
                        } else {
                            etAchievements.setText(documentSnapshot.getString("achievements_ru"));
                        }

                        String savedStyleKey = documentSnapshot.getString("style_key");
                        String savedCountryIso = documentSnapshot.getString("nation_iso");

                        if (savedStyleKey != null) {
                            for (int i = 0; i < styleKeys.length; i++) {
                                if (styleKeys[i].equals(savedStyleKey)) {
                                    spinStyle.setText(styleDisplayNames[i], false);
                                    break;
                                }
                            }
                        }

                        if (savedCountryIso != null && !savedCountryIso.isEmpty()) {
                            int countryPosition = countryIsoKeys.indexOf(savedCountryIso);
                            if (countryPosition >= 0) {
                                spinNation.setText(countryDisplayNames.get(countryPosition), false);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), isEnglish ? "Error loading data" : "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAndExit(View v) {
        if (currentUid == null) return;

        String firstName = etFirst.getText().toString().trim();
        String lastName = etLast.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();
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

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstName);
        userUpdates.put("lastName", lastName);
        userUpdates.put("height", height);
        userUpdates.put("weight", weight);
        userUpdates.put("style_key", styleKeyToSave);
        userUpdates.put("nation_iso", countryIsoToSave);
        userUpdates.put("style", styleDisplay);
        userUpdates.put("nation", countryDisplay);

        String enteredAchievements = etAchievements.getText().toString();
        if (isEnglish) {
            userUpdates.put("achievements_en", enteredAchievements);
        } else {
            userUpdates.put("achievements_ru", enteredAchievements);
        }

        db.collection("users").document(currentUid)
                .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("firstName", firstName);
                        editor.putString("lastName", lastName);
                        editor.putString("height", height);
                        editor.putString("weight", weight);
                        editor.putString("style", styleDisplay);
                        editor.putString("nation", countryDisplay);
                        editor.apply();

                        String message = isEnglish ? "Profile updated in cloud" : "Профиль обновлен в облаке";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(v).popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        String errorMsg = isEnglish ? "Save failed" : "Ошибка сохранения";
                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
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