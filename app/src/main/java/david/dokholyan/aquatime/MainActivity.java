package david.dokholyan.aquatime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AquaTimeMain";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedSettings();
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);

        if (!isGuest && currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (currentUser != null && !isGuest) {
            String cachedName = prefs.getString("firstName", null);
            if (cachedName == null) {
                Log.d(TAG, "Сработал автологин. Кэш пуст, запускаем фоновую синхронизацию с Firestore.");
                syncUserCacheAsync(currentUser.getUid());
            }
        }

        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.nav_view);
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
    }

    private void syncUserCacheAsync(String userId) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        DocumentSnapshot snapshot = task.getResult();
                        SharedPreferences.Editor editor = prefs.edit();

                        Log.d(TAG, "Данные автологина успешно подтянуты из Firestore для: " + userId);

                        if (snapshot.contains("firstName")) {
                            editor.putString("firstName", snapshot.getString("firstName"));
                        }
                        if (snapshot.contains("all_res")) editor.putString("all_res", snapshot.getString("all_res"));

                        Long totalMeters = snapshot.getLong("total_meters");
                        Long trainingsCount = snapshot.getLong("trainings_count");
                        Long weeklyMeters = snapshot.getLong("weekly_meters");

                        if (totalMeters != null) editor.putInt("total_meters", totalMeters.intValue());
                        if (trainingsCount != null) editor.putInt("trainings_count", trainingsCount.intValue());
                        if (weeklyMeters != null) editor.putInt("weekly_meters", weeklyMeters.intValue());

                        if (snapshot.contains("lastName")) editor.putString("lastName", snapshot.getString("lastName"));
                        if (snapshot.contains("nation")) editor.putString("nation", snapshot.getString("nation"));
                        if (snapshot.contains("height")) editor.putString("height", snapshot.getString("height"));
                        if (snapshot.contains("weight")) editor.putString("weight", snapshot.getString("weight"));
                        if (snapshot.contains("style")) editor.putString("style", snapshot.getString("style"));

                        editor.putBoolean("registered", true);
                        editor.putString("userId", userId);
                        editor.apply();

                        recreate();
                    } else {
                        Log.e(TAG, "Не удалось обновить кэш при автологине", task.getException());
                    }
                });
    }

    private void applySavedSettings() {
        SharedPreferences prefs = getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        int mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);

        String lang = prefs.getString("app_lang", "ru");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}