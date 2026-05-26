package david.dokholyan.aquatime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "AquaTimeLogin";


    private static final String GUEST_EMAIL = "innovationcampus26@gmail.com";
    private static final String GUEST_PASSWORD = "Samsung2026";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGuest;
    private TextView tvToRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        applySavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGuest = findViewById(R.id.btn_guest);
        tvToRegister = findViewById(R.id.tv_to_register);

        tvToRegister.setOnClickListener(v -> finish());


        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            boolean isEn = isEnglish();

            if (!email.isEmpty() && !password.isEmpty()) {

                prepareCacheBeforeLogin();
                loginUser(email, password);
            } else {
                Toast.makeText(this, isEn ? "Please enter email and password" : "Пожалуйста, введите email и пароль", Toast.LENGTH_SHORT).show();
            }
        });


        btnGuest.setOnClickListener(v -> {
            btnGuest.setEnabled(false);
            prepareCacheBeforeLogin();
            executeGuestFirebaseLogin();
        });
    }

    private void prepareCacheBeforeLogin() {
        String currentLang = prefs.getString("app_lang", "ru");
        int currentTheme = prefs.getInt("theme_mode", 2);

        prefs.edit().clear().apply();

        prefs.edit()
                .putString("app_lang", currentLang)
                .putInt("theme_mode", currentTheme)
                .apply();
    }

    private void applySavedLanguage() {
        String lang = prefs.getString("app_lang", "ru");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void loginUser(String email, String password) {
        btnLogin.setEnabled(false);
        boolean isEn = isEnglish();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            syncDataFromFirebaseAndNavigate(user.getUid(), false);
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, (isEn ? "Login failed: " : "Ошибка входа: ") + errorMsg, Toast.LENGTH_LONG).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }

    private void executeGuestFirebaseLogin() {
        boolean isEn = isEnglish();

        mAuth.signInWithEmailAndPassword(GUEST_EMAIL, GUEST_PASSWORD)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {

                            syncDataFromFirebaseAndNavigate(user.getUid(), true);
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException ||
                                e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {

                            mAuth.createUserWithEmailAndPassword(GUEST_EMAIL, GUEST_PASSWORD)
                                    .addOnCompleteListener(this, createWithEmailTask -> {
                                        if (createWithEmailTask.isSuccessful()) {
                                            FirebaseUser newUser = mAuth.getCurrentUser();
                                            if (newUser != null) {
                                                syncDataFromFirebaseAndNavigate(newUser.getUid(), true);
                                            }
                                        } else {
                                            Toast.makeText(this, isEn ? "Login failed" : "Ошибка входа", Toast.LENGTH_LONG).show();
                                            btnGuest.setEnabled(true);
                                        }
                                    });
                        } else {
                            Toast.makeText(this, isEn ? "Login failed" : "Ошибка входа", Toast.LENGTH_LONG).show();
                            btnGuest.setEnabled(true);
                        }
                    }
                });
    }


    private void syncDataFromFirebaseAndNavigate(String userId, boolean isGuestMode) {
        boolean isEn = isEnglish();

        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            SharedPreferences.Editor editor = prefs.edit();

            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();


                String allRes = snapshot.child("all_res").getValue(String.class);
                Long totalMeters = snapshot.child("total_meters").getValue(Long.class);
                Long trainingsCount = snapshot.child("trainings_count").getValue(Long.class);
                Long weeklyMeters = snapshot.child("weekly_meters").getValue(Long.class);

                if (allRes != null) editor.putString("all_res", allRes);
                if (totalMeters != null) editor.putInt("total_meters", totalMeters.intValue());
                if (trainingsCount != null) editor.putInt("trainings_count", trainingsCount.intValue());
                if (weeklyMeters != null) editor.putInt("weekly_meters", weeklyMeters.intValue());


                if (snapshot.hasChild("firstName")) editor.putString("firstName", snapshot.child("firstName").getValue(String.class));
                if (snapshot.hasChild("lastName")) editor.putString("lastName", snapshot.child("lastName").getValue(String.class));
                if (snapshot.hasChild("nation")) editor.putString("nation", snapshot.child("nation").getValue(String.class));
                if (snapshot.hasChild("height")) editor.putString("height", snapshot.child("height").getValue(String.class));
                if (snapshot.hasChild("weight")) editor.putString("weight", snapshot.child("weight").getValue(String.class));
                if (snapshot.hasChild("style")) editor.putString("style", snapshot.child("style").getValue(String.class));

            } else {

                if (isGuestMode) {
                    editor.putString("firstName", "Guest");
                    editor.putString("nation", "");
                }
            }


            editor.putBoolean("registered", !isGuestMode);
            editor.putString("userId", userId);
            editor.apply();


            if (isGuestMode) {
                Toast.makeText(this, isEn ? "Welcome, Guest! 🌊" : "Добро пожаловать, Гость! 🌊", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, isEn ? "Data successfully synced! 🌊" : "Данные успешно синхронизированы! 🌊", Toast.LENGTH_SHORT).show();
            }

            navigateToMain();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || GUEST_EMAIL.equalsIgnoreCase(user.getEmail())) {
            intent.putExtra("isGuest", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isEnglish() {
        return prefs.getString("app_lang", "ru").equalsIgnoreCase("en");
    }
}