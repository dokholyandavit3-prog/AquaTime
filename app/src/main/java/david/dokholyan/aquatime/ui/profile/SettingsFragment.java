package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import java.util.Locale;
import david.dokholyan.aquatime.MainActivity;
import david.dokholyan.aquatime.R;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);


        v.findViewById(R.id.btn_back).setOnClickListener(view -> {
            if (getFragmentManager() != null) {
                requireActivity().onBackPressed();
            }
        });

        v.findViewById(R.id.layout_theme).setOnClickListener(view -> showThemeSelectionDialog());
        v.findViewById(R.id.layout_language).setOnClickListener(view -> showLanguageSelectionDialog());

        return v;
    }

    private void showThemeSelectionDialog() {
        String[] themes = {"Светлая", "Темная", "Системная"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите тему")
                .setItems(themes, (dialog, which) -> {
                    int mode = (which == 0) ? AppCompatDelegate.MODE_NIGHT_NO :
                            (which == 1) ? AppCompatDelegate.MODE_NIGHT_YES :
                                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

                    prefs.edit().putInt("theme_mode", mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                }).show();
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"Русский", "English"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String lang = (which == 1) ? "en" : "ru";
                    updateResource(lang);
                }).show();
    }

    private void updateResource(String langCode) {

        prefs.edit().putString("app_lang", langCode).apply();


        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = requireActivity().getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());


        Intent intent = new Intent(requireActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}