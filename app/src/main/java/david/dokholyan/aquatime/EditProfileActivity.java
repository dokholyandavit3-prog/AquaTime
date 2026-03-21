package david.dokholyan.aquatime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        EditText name = findViewById(R.id.edit_name);
        EditText age = findViewById(R.id.edit_age);
        EditText height = findViewById(R.id.edit_height);
        EditText weight = findViewById(R.id.edit_weight);
        Button save = findViewById(R.id.btn_save_changes);

        SharedPreferences prefs = getSharedPreferences("user_data", MODE_PRIVATE);

        // Подставляем текущие данные
        name.setText(prefs.getString("name", ""));
        age.setText(prefs.getString("age", ""));
        height.setText(prefs.getString("height", ""));
        weight.setText(prefs.getString("weight", ""));

        save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("name", name.getText().toString());
            editor.putString("age", age.getText().toString());
            editor.putString("height", height.getText().toString());
            editor.putString("weight", weight.getText().toString());
            editor.apply();

            // Возвращаемся обратно в MainActivity
            startActivity(new Intent(EditProfileActivity.this, MainActivity.class));
            finish();
        });
    }
}
