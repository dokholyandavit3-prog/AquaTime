package david.dokholyan.aquatime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvToLogin;
    private ImageView btnChangeLanguage;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        prefs = getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        applySavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            navigateToMain();
            return;
        }

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvToLogin = findViewById(R.id.tv_to_login);
        btnChangeLanguage = findViewById(R.id.btn_change_language);

        tvToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> registerUser());

        if (btnChangeLanguage != null) {
            btnChangeLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        }
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"English", "Русский"};
        boolean isEn = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEn ? "Select Language" : "Выберите язык");

        builder.setItems(languages, (dialog, which) -> {
            String targetLang = (which == 0) ? "en" : "ru";


            prefs.edit().putString("app_lang", targetLang).apply();


            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        builder.show();
    }

    private void applySavedLanguage() {
        String lang = prefs.getString("app_lang", "ru");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean isEn = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, isEn ? "Please fill all fields" : "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, isEn ? "Password must be at least 6 characters" : "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, isEn ? "Passwords do not match!" : "Пароли не совпадают!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", username);
                            userMap.put("email", email);
                            userMap.put("total_meters", 0);
                            userMap.put("trainings_count", 0);
                            userMap.put("weekly_meters", 0);
                            userMap.put("all_res", "");

                            mDatabase.child("users").child(userId).setValue(userMap);

                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    Toast.makeText(this, isEn ? "Verification email sent! Confirm it and login." : "Письмо подтверждения отправлено! Подтвердите его и войдите.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();

                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, (isEn ? "Error: " : "Ошибка: ") + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}