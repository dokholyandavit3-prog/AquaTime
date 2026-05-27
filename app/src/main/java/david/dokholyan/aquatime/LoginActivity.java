package david.dokholyan.aquatime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "AquaTimeLogin";

    private static final String GUEST_EMAIL = "innovationcampus26@gmail.com";
    private static final String GUEST_PASSWORD = "Samsung2026";

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGuest;
    private TextView tvToRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        applySavedLanguage();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGuest = findViewById(R.id.btn_guest);
        tvToRegister = findViewById(R.id.tv_to_register);

        tvToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            boolean isEn = isEnglish();

            if (!isNetworkAvailable()) {
                Toast.makeText(this, isEn ? "No internet connection. Please check your Wi-Fi or Mobile data." : "Нет подключения к интернету. Проверьте Wi-Fi или мобильную сеть.", Toast.LENGTH_LONG).show();
                return;
            }

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!email.isEmpty() && !password.isEmpty()) {
                prepareCacheBeforeLogin();
                loginUser(email, password);
            } else {
                Toast.makeText(this, isEn ? "Please enter email and password" : "Пожалуйста, введите email и пароль", Toast.LENGTH_SHORT).show();
            }
        });

        btnGuest.setOnClickListener(v -> {
            boolean isEn = isEnglish();

            if (!isNetworkAvailable()) {
                Toast.makeText(this, isEn ? "Internet connection required for guest sync." : "Для гостевого входа и синхронизации нужен интернет.", Toast.LENGTH_LONG).show();
                return;
            }

            btnGuest.setEnabled(false);
            prepareCacheBeforeLogin();
            executeGuestFirebaseLogin();
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
        }
        return false;
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

        mFirestore.collection("users").document(userId).get().addOnCompleteListener(task -> {
            SharedPreferences.Editor editor = prefs.edit();

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot snapshot = task.getResult();
                Log.d(TAG, "Пользователь найден в Firestore! ID: " + userId);

                String allRes = snapshot.getString("all_res");
                Long totalMeters = snapshot.getLong("total_meters");
                Long trainingsCount = snapshot.getLong("trainings_count");
                Long weeklyMeters = snapshot.getLong("weekly_meters");

                if (allRes != null) editor.putString("all_res", allRes);
                if (totalMeters != null) editor.putInt("total_meters", totalMeters.intValue());
                if (trainingsCount != null) editor.putInt("trainings_count", trainingsCount.intValue());
                if (weeklyMeters != null) editor.putInt("weekly_meters", weeklyMeters.intValue());

                if (snapshot.contains("last_completed_distance")) {
                    Long lastDist = snapshot.getLong("last_completed_distance");
                    if (lastDist != null) editor.putInt("last_completed_distance", lastDist.intValue());
                }
                if (snapshot.contains("last_completed_time")) editor.putString("last_completed_time", snapshot.getString("last_completed_time"));
                if (snapshot.contains("last_completed_date")) editor.putString("last_completed_date", snapshot.getString("last_completed_date"));
                if (snapshot.contains("last_completed_style")) editor.putString("last_completed_style", snapshot.getString("last_completed_style"));


                if (snapshot.contains("current_streak")) {
                    Long currentStreak = snapshot.getLong("current_streak");
                    if (currentStreak != null) editor.putInt("current_streak", currentStreak.intValue());
                }
                if (snapshot.contains("best_streak")) {
                    Long bestStreak = snapshot.getLong("best_streak");
                    if (bestStreak != null) editor.putInt("best_streak", bestStreak.intValue());
                }
                if (snapshot.contains("last_visit_date_string")) {
                    editor.putString("last_visit_date_string", snapshot.getString("last_visit_date_string"));
                }

                if (isGuestMode) {
                    Log.d(TAG, "Гостевой режим: принудительно ставим Test User");
                    editor.putString("firstName", "Test User");
                    editor.putString("lastName", "");
                    saveGuestNameInFirestore(userId, "Test User");
                } else {
                    if (snapshot.contains("firstName")) editor.putString("firstName", snapshot.getString("firstName"));
                    if (snapshot.contains("lastName")) editor.putString("lastName", snapshot.getString("lastName"));
                }

                if (snapshot.contains("nation")) editor.putString("nation", snapshot.getString("nation"));
                if (snapshot.contains("height")) editor.putString("height", snapshot.getString("height"));
                if (snapshot.contains("weight")) editor.putString("weight", snapshot.getString("weight"));
                if (snapshot.contains("style")) editor.putString("style", snapshot.getString("style"));

            } else {
                Log.d(TAG, "Документ пользователя НЕ существует в Firestore или ошибка.");
                if (isGuestMode) {
                    editor.putString("firstName", "Test User");
                    editor.putString("lastName", "");
                    editor.putString("nation", "");
                    saveGuestNameInFirestore(userId, "Test User");
                }
            }

            editor.putBoolean("registered", !isGuestMode);
            editor.putString("userId", userId);
            editor.apply();

            if (isGuestMode) {
                Toast.makeText(this, isEn ? "Welcome, Test User! 🌊" : "Добро пожаловать, Test User! 🌊", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, isEn ? "Data successfully synced! 🌊" : "Данные успешно синхронизированы! 🌊", Toast.LENGTH_SHORT).show();
            }

            navigateToMain();
        });
    }

    private void saveGuestNameInFirestore(String userId, String guestName) {
        Map<String, Object> guestUpdate = new HashMap<>();
        guestUpdate.put("firstName", guestName);
        guestUpdate.put("lastName", "");
        guestUpdate.put("email", GUEST_EMAIL);

        mFirestore.collection("users").document(userId)
                .set(guestUpdate, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Ошибка сохранения имени гостя в Firestore", e));
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