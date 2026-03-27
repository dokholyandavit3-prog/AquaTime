package david.dokholyan.aquatime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class UserSetupActivity extends AppCompatActivity {

    EditText etFirstName, etLastName, etEmail, etAge, etHeight, etWeight, etStyle;
    Spinner spinnerLevel, spinnerNation;
    Button btnContinue;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AquaTime", MODE_PRIVATE);

        // ✅ если уже зарегистрирован → сразу в Main
        if (!prefs.getBoolean("isFirstLaunch", true)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_user_setup);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etStyle = findViewById(R.id.et_style);

        spinnerNation = findViewById(R.id.spinner_nation);
        spinnerLevel = findViewById(R.id.spinner_level);
        btnContinue = findViewById(R.id.btn_continue);

        // 🔥 Уровень
        String[] levels = {"Начинающий", "Средний", "Профессионал"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, levels);
        spinnerLevel.setAdapter(levelAdapter);

        // 🌍 СПИСОК СТРАН С ФЛАГАМИ
        setupCountries();

        btnContinue.setOnClickListener(v -> {

            String firstName = etFirstName.getText().toString();
            String lastName = etLastName.getText().toString();
            String email = etEmail.getText().toString();
            String age = etAge.getText().toString();
            String height = etHeight.getText().toString();
            String weight = etWeight.getText().toString();
            String style = etStyle.getText().toString();
            String nation = spinnerNation.getSelectedItem().toString();
            String level = spinnerLevel.getSelectedItem().toString();

            if (firstName.isEmpty() || email.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "Заполни обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString("firstName", firstName)
                    .putString("lastName", lastName)

                    .putString("email", email)
                    .putString("user_email", email)

                    .putString("age", age)
                    .putString("user_age", age)

                    .putString("height", height)
                    .putString("weight", weight)
                    .putString("style", style)
                    .putString("nation", nation)

                    .putString("user_name", firstName)
                    .putString("user_level", level)

                    .putBoolean("isFirstLaunch", false)
                    .putBoolean("logged_in", true)
                    .apply();

            Toast.makeText(this, "Профиль создан!", Toast.LENGTH_SHORT).show();

            openMain();
        });
    }

    // 🌍 страны
    private void setupCountries() {

        ArrayList<String> countries = new ArrayList<>();

        String[] iso = Locale.getISOCountries();

        for (String code : iso) {
            Locale locale = new Locale("", code);
            countries.add(getFlagEmoji(code) + " " + locale.getDisplayCountry());
        }

        // 🔥 сортировка
        Collections.sort(countries);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                countries
        );spinnerNation.setAdapter(adapter);
    }

    // 🇦🇲 флаг
    private String getFlagEmoji(String countryCode) {

        int firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;

        return new String(Character.toChars(firstLetter)) +
                new String(Character.toChars(secondLetter));
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}