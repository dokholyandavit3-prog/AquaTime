package david.dokholyan.aquatime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class UserSetupActivity extends AppCompatActivity {

    EditText etName, etEmail, etAge;
    Spinner spinnerLevel;
    Button btnContinue;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AquaTime", MODE_PRIVATE);

        if (prefs.getBoolean("logged_in", false)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_user_setup);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etAge = findViewById(R.id.et_age);
        spinnerLevel = findViewById(R.id.spinner_level);
        btnContinue = findViewById(R.id.btn_continue);

        // уровни
        String[] levels = {"Начинающий", "Средний", "Профессионал"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, levels);
        spinnerLevel.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> {

            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String age = etAge.getText().toString();
            String level = spinnerLevel.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "Заполни все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString("user_name", name)
                    .putString("user_email", email)
                    .putString("user_age", age)
                    .putString("user_level", level)
                    .putBoolean("logged_in", true)
                    .apply();

            openMain();
        });
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}