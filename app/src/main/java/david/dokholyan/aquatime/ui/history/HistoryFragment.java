package david.dokholyan.aquatime.ui.history;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import david.dokholyan.aquatime.R;

import java.util.Locale;

public class HistoryFragment extends Fragment {

    private LinearLayout historyContainer, goalsContainer;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_full, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        historyContainer = view.findViewById(R.id.history_list);
        goalsContainer = view.findViewById(R.id.goals_container);

        view.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());

        loadData();
        return view;
    }

    private void loadData() {
        if (goalsContainer == null || historyContainer == null) return;

        goalsContainer.removeAllViews();
        historyContainer.removeAllViews();


        String plannerData = prefs.getString("planner", "").trim();
        if (!plannerData.isEmpty() && !plannerData.equals(getString(R.string.planner_empty))) {
            String[] goals = plannerData.split("\n");
            for (int i = 0; i < goals.length; i++) {
                final int index = i;
                String goalText = goals[i].trim();
                if (!goalText.isEmpty()) {

                    addCardToContainer(goalsContainer, goalText, view -> {
                        showDeleteDialog(index, goals, true);
                        return true;
                    });
                }
            }
        } else {
            addEmptyMessage(goalsContainer, isEnglish() ? "No active goals." : "Нет активных целей.");
        }


        String historyData = prefs.getString("all_res", "");
        if (!historyData.isEmpty()) {
            String[] results = historyData.split(";");
            for (int i = 0; i < results.length; i++) {
                final int index = i;
                String res = results[i];
                String formattedRes = formatHistoryText(res);
                addCardToContainer(historyContainer, formattedRes, view -> {
                    showDeleteDialog(index, results, false);
                    return true;
                });
            }
        } else {
            addEmptyMessage(historyContainer, isEnglish() ? "History is empty." : "История тренировок пуста.");
        }
    }

    private String formatHistoryText(String rawData) {
        try {
            String[] parts = rawData.split("\\|");
            if (parts.length >= 3) {
                return parts[0].trim() + "м (" + parts[2].trim() + ")";
            }
        } catch (Exception e) {
            return rawData;
        }
        return rawData;
    }


    private void addCardToContainer(LinearLayout container, String text, View.OnLongClickListener longClickListener) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(32f);
        card.setCardElevation(0f);
        card.setStrokeWidth(2);
        card.setStrokeColor(getResources().getColor(R.color.aqua_primary));
        card.setContentPadding(40, 32, 40, 32);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(getResources().getColor(R.color.aqua_deep));

        card.addView(tv);
        card.setOnLongClickListener(longClickListener);
        container.addView(card);
    }


    private void addEmptyMessage(LinearLayout container, String message) {
        TextView tvEmpty = new TextView(getContext());
        tvEmpty.setText(message);
        tvEmpty.setTextColor(android.graphics.Color.GRAY);
        tvEmpty.setPadding(16, 16, 16, 16);
        container.addView(tvEmpty);
    }

    private void showDeleteDialog(int index, String[] dataItems, boolean isGoal) {
        if (getContext() == null) return;

        String title = isEnglish() ? "Delete item" : "Удаление";
        String message = isGoal
                ? (isEnglish() ? "Do you want to delete this goal?" : "Вы хотите удалить эту цель?")
                : (isEnglish() ? "Do you want to delete this training record?" : "Вы хотите удалить эту тренировку?");

        String positiveBtn = isEnglish() ? "Yes" : "Да";
        String negativeBtn = isEnglish() ? "No" : "Нет";

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeBtn, null)
                .setPositiveButton(positiveBtn, (dialog, which) -> {
                    if (isGoal) {
                        deleteGoal(index, dataItems);
                    } else {
                        deleteTraining(index, dataItems);
                    }
                })
                .show();
    }

    private void deleteGoal(int indexToDelete, String[] goals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < goals.length; i++) {
            if (i == indexToDelete) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(goals[i]);
        }

        String updatedPlanner = sb.toString().trim();
        if (updatedPlanner.isEmpty()) {
            updatedPlanner = getString(R.string.planner_empty);
        }

        prefs.edit().putString("planner", updatedPlanner).apply();


        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid()).child("planner").setValue(updatedPlanner);
        }

        loadData();
        Toast.makeText(getContext(), isEnglish() ? "Goal deleted" : "Цель удалена", Toast.LENGTH_SHORT).show();
    }

    private void deleteTraining(int indexToDelete, String[] records) {
        StringBuilder sb = new StringBuilder();
        int newTotalMeters = 0;
        int newWeeklyMeters = 0;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.US);
        java.util.Calendar now = java.util.Calendar.getInstance();
        int currentWeek = now.get(java.util.Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(java.util.Calendar.YEAR);

        for (int i = 0; i < records.length; i++) {
            if (i == indexToDelete) continue;

            if (sb.length() > 0) sb.append(";");
            sb.append(records[i]);


            try {
                String[] p = records[i].split("\\|");
                int meters = Integer.parseInt(p[0].trim());
                java.util.Date date = sdf.parse(p[2].trim());
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(date);

                newTotalMeters += meters;
                if (cal.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && cal.get(java.util.Calendar.YEAR) == currentYear) {
                    newWeeklyMeters += meters;
                }
            } catch (Exception ignored) {}
        }

        String updatedHistory = sb.toString();
        int newTrainingsCount = (updatedHistory.isEmpty()) ? 0 : updatedHistory.split(";").length;


        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("all_res", updatedHistory);
        editor.putInt("total_meters", newTotalMeters);
        editor.putInt("trainings_count", newTrainingsCount);
        editor.putInt("weekly_meters", newWeeklyMeters);
        editor.apply();


        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users").child(user.getUid());

            userRef.child("all_res").setValue(updatedHistory);
            userRef.child("total_meters").setValue(newTotalMeters);
            userRef.child("trainings_count").setValue(newTrainingsCount);
            userRef.child("weekly_meters").setValue(newWeeklyMeters);
        }

        loadData();
        Toast.makeText(getContext(), isEnglish() ? "Record deleted" : "Тренировка удалена", Toast.LENGTH_SHORT).show();
    }

    private boolean isEnglish() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("en");
    }
}