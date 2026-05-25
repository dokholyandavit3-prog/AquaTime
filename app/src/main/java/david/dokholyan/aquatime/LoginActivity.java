package david.dokholyan.aquatime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
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

            boolean isEn = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

            if (!email.isEmpty() && !password.isEmpty()) {
                loginUser(email, password);
            } else {
                Toast.makeText(this, isEn ? "Please enter email and password" : "Пожалуйста, введите email и пароль", Toast.LENGTH_SHORT).show();
            }
        });

        btnGuest.setOnClickListener(v -> navigateToMain());
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
        boolean isEn = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (user.isEmailVerified()) {
                                    syncDataFromFirebase(user.getUid());
                                } else {
                                    Toast.makeText(this, isEn ? "Please verify your email first!" : "Пожалуйста, подтвердите ваш email!", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    btnLogin.setEnabled(true);
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, (isEn ? "Login failed: " : "Ошибка входа: ") + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);
                    }
                });
    }

    private void syncDataFromFirebase(String userId) {
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            boolean isEn = prefs.getString("app_lang", "ru").equalsIgnoreCase("en");
            if (task.isSuccessful() && task.getResult().exists()) {
                DataSnapshot snapshot = task.getResult();

                String allRes = snapshot.child("all_res").getValue(String.class);
                Long totalMeters = snapshot.child("total_meters").getValue(Long.class);
                Long trainingsCount = snapshot.child("trainings_count").getValue(Long.class);
                Long weeklyMeters = snapshot.child("weekly_meters").getValue(Long.class);

                SharedPreferences.Editor editor = prefs.edit();
                if (allRes != null) editor.putString("all_res", allRes);
                if (totalMeters != null) editor.putInt("total_meters", totalMeters.intValue());
                if (trainingsCount != null)
                    editor.putInt("trainings_count", trainingsCount.intValue());
                if (weeklyMeters != null) editor.putInt("weekly_meters", weeklyMeters.intValue());
                editor.apply();

                Toast.makeText(this, isEn ? "Data successfully synced! 🌊" : "Данные успешно синхронизированы! 🌊", Toast.LENGTH_SHORT).show();
            }
            navigateToMain();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        if (mAuth.getCurrentUser() == null) {
            intent.putExtra("isGuest", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}