package david.dokholyan.aquatime.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import david.dokholyan.aquatime.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StopwatchHistoryFragment extends Fragment {

    private static final String TAG = "AquaTimeStopwatch";
    private SharedPreferences prefs;
    private LinearLayout listContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stopwatch_history, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        listContainer = v.findViewById(R.id.st_list_container);

        TextView tvTitle = v.findViewById(R.id.tv_st_title);
        if (tvTitle != null && Locale.getDefault().getLanguage().equalsIgnoreCase("en")) {
            tvTitle.setText("Stopwatch Full History");
        }

        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                androidx.navigation.Navigation.findNavController(view).navigateUp());

        loadFullStopwatchHistory();
        return v;
    }

    private void loadFullStopwatchHistory() {
        if (listContainer == null) return;
        listContainer.removeAllViews();

        String rawLog = prefs.getString("stopwatch_log", "");
        if (rawLog.isEmpty()) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText(isEnglish() ? "No timer records found." : "История замеров пуста.");
            tvEmpty.setTextColor(Color.GRAY);
            listContainer.addView(tvEmpty);
            return;
        }

        String[] logs = rawLog.split(";");

        for (int i = 0; i < logs.length; i++) {
            final int indexToDelete = i;
            String logEntry = logs[i];
            String[] pieces = logEntry.split("\\|");
            if (pieces.length < 3) continue;

            String style = pieces[0].trim();
            String time = pieces[1].trim();
            String date = pieces[2].trim();

            TextView tvItem = new TextView(getContext());
            tvItem.setText(style + " — " + time + " (" + date + ")");
            tvItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvItem.setTextColor(Color.parseColor("#2C3E50"));
            tvItem.setPadding(32, 24, 32, 24);
            tvItem.setBackgroundColor(Color.WHITE);

            tvItem.setOnLongClickListener(view -> {
                showDeleteDialog(indexToDelete, logs);
                return true;
            });

            View divider = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"));

            listContainer.addView(tvItem);
            listContainer.addView(divider);
        }
    }

    private void showDeleteDialog(int index, String[] logs) {
        if (getContext() == null) return;

        String title = isEnglish() ? "Delete record" : "Удалить заплыв";
        String message = isEnglish() ? "Do you want to delete this record?" : "Вы хотите удалить этот замер?";
        String positiveBtn = isEnglish() ? "Yes" : "Да";
        String negativeBtn = isEnglish() ? "No" : "Нет";

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeBtn, null)
                .setPositiveButton(positiveBtn, (dialog, which) -> deleteLogEntry(index, logs))
                .show();
    }

    private void deleteLogEntry(int indexToDelete, String[] logs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logs.length; i++) {
            if (i == indexToDelete) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(logs[i]);
        }

        String updatedLog = sb.toString();

        prefs.edit().putString("stopwatch_log", updatedLog).apply();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("stopwatch_log", updatedLog);

            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .set(updateMap, SetOptions.merge())
                    .addOnFailureListener(e -> Log.e(TAG, "Ошибка удаления замера в Firestore", e));
        }

        loadFullStopwatchHistory();
        Toast.makeText(getContext(), isEnglish() ? "Deleted" : "Удалено", Toast.LENGTH_SHORT).show();
    }

    private boolean isEnglish() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("en");
    }
}