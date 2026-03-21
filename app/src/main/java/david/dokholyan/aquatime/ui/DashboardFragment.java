package david.dokholyan.aquatime.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import david.dokholyan.aquatime.R;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        Button btnStart = view.findViewById(R.id.btn_start_training);
        Button btnHistory = view.findViewById(R.id.btn_history);

        NavController navController = NavHostFragment.findNavController(this);

        btnStart.setOnClickListener(v -> {
            navController.navigate(R.id.trainingFragment);
        });

        btnHistory.setOnClickListener(v -> {
            navController.navigate(R.id.analyticsFragment);
        });

        return view;
    }
}