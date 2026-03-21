package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import david.dokholyan.aquatime.R;

public class EditProfileFragment extends Fragment {

    private EditText firstName, lastName, height, weight, style, nation, achievements;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        Context context = requireActivity();
        SharedPreferences prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);

        firstName = view.findViewById(R.id.input_first_name);
        lastName = view.findViewById(R.id.input_last_name);
        height = view.findViewById(R.id.input_height);
        weight = view.findViewById(R.id.input_weight);
        style = view.findViewById(R.id.input_style);
        nation = view.findViewById(R.id.input_nation);
        achievements = view.findViewById(R.id.input_achievements);

        // Заполняем существующие данные
        firstName.setText(prefs.getString("firstName", ""));
        lastName.setText(prefs.getString("lastName", ""));
        height.setText(prefs.getString("height", ""));
        weight.setText(prefs.getString("weight", ""));
        style.setText(prefs.getString("style", ""));
        nation.setText(prefs.getString("nation", ""));
        achievements.setText(prefs.getString("achievements", ""));

        Button saveButton = view.findViewById(R.id.btn_save);
        Button backButton = view.findViewById(R.id.btn_back);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        saveButton.setOnClickListener(v -> {
            prefs.edit()
                    .putString("firstName", firstName.getText().toString())
                    .putString("lastName", lastName.getText().toString())
                    .putString("height", height.getText().toString())
                    .putString("weight", weight.getText().toString())
                    .putString("style", style.getText().toString())
                    .putString("nation", nation.getText().toString())
                    .putString("achievements", achievements.getText().toString())
                    .apply();

            navController.popBackStack();
        });

        backButton.setOnClickListener(v -> navController.popBackStack());

        return view;
    }
}