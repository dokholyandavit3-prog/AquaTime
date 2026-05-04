package david.dokholyan.aquatime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.nav_view);

        // Находим контроллер навигации
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // СВЯЗЫВАЕМ меню с контроллером (чтобы при клике менялись фрагменты)
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Логика гостя и авторизации (которую мы писали ранее)
        boolean isGuest = getIntent().getBooleanExtra("isGuest", false);
        if (!isGuest && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
