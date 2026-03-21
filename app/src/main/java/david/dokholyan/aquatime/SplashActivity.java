package david.dokholyan.aquatime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            // Проверяем, есть ли имя пользователя в памяти
            SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);
            if (prefs.getString("name", null) == null) {
                // Если данных нет → первый запуск → идём в ввод данных
                startActivity(new Intent(this, UserSetupActivity.class));
            } else {
                // Если данные уже есть → сразу открываем главное меню
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 1500); // Задержка 1.5 секунды
    }
}
