package david.dokholyan.aquatime.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import david.dokholyan.aquatime.R;

public class AchievementsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_achievements, container, false);

        Button backButton = view.findViewById(R.id.btn_back);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        backButton.setOnClickListener(v -> navController.popBackStack());

        return view;
    }
}
