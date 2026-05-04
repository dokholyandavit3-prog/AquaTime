package david.dokholyan.aquatime.ui.history;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import david.dokholyan.aquatime.R;

public class HistoryFragment extends Fragment {

    private LinearLayout historyContainer, goalsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_full, container, false);

        historyContainer = view.findViewById(R.id.history_list);
        goalsContainer = view.findViewById(R.id.goals_container);

        // Кнопка назад
        view.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());

        loadData();
        return view;
    }

    private void loadData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        // 1. Загрузка текущих целей (Planner)
        String plannerData = prefs.getString("planner", "");
        if (!plannerData.isEmpty()) {
            String[] goals = plannerData.split("\n");
            for (String goal : goals) {
                if (!goal.trim().isEmpty()) {
                    addCardToContainer(goalsContainer, "• " + goal.trim(), R.color.aqua_primary);
                }
            }
        }

        // 2. Загрузка выполненных тренировок
        // Используем all_res, куда сохраняются данные из твоего конструктора
        String historyData = prefs.getString("all_res", "");
        if (!historyData.isEmpty()) {
            String[] results = historyData.split(";");
            for (String res : results) {
                // Если данные сохранены как "500|10:00|14.04.2026", приводим к красивому виду
                String formattedRes = formatHistoryText(res);
                addCardToContainer(historyContainer, formattedRes, R.color.aqua_primary);
            }
        }
    }

    private String formatHistoryText(String rawData) {
        try {
            String[] parts = rawData.split("\\|");
            if (parts.length >= 3) {
                // Формат: "Кроль 50м | 00:31:51 (14.04 15:50)"
                return parts[0].trim() + "м | " + parts[1].trim() + " (" + parts[2].trim() + ")";
            }
        } catch (Exception e) {
            return rawData;
        }
        return rawData;
    }

    private void addCardToContainer(LinearLayout container, String text, int colorRes) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16); // Отступ между карточками
        card.setLayoutParams(params);
        card.setRadius(32f); // Большое закругление как на фото
        card.setCardElevation(0f); // На скрине карточки плоские с контуром или легкой тенью
        card.setStrokeWidth(2);
        card.setStrokeColor(getResources().getColor(R.color.aqua_primary));
        card.setContentPadding(40, 32, 40, 32);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(getResources().getColor(R.color.aqua_deep));

        card.addView(tv);
        container.addView(card);
    }
}