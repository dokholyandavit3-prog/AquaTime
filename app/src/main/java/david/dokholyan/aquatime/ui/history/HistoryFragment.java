package david.dokholyan.aquatime.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import david.dokholyan.aquatime.R;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Убедитесь, что имя layout точно fragment_history_full
        View view = inflater.inflate(R.layout.fragment_history_full, container, false);

        // Используем view.findViewById — важно в фрагментах
        Button back = view.findViewById(R.id.btn_back);
        LinearLayout list = view.findViewById(R.id.history_list);

        // Защита: если кнопка не найдена — не вызывать setOnClickListener на null
        if (back != null) {
            back.setOnClickListener(v -> requireActivity().onBackPressed());
        }

        // Пример заполнения списка — лучше потом заменить на реальные данные
        if (list != null) {
            list.removeAllViews();
            String[] trainings = {
                    "🏁 1000 м вольный стиль — 18:22",
                    "🔥 1500 м баттерфляй — 26:05",
                    "🏅 200 м комплекс — 5:11"
            };
            for (String t : trainings) {
                TextView tv = new TextView(getContext());
                tv.setText(t);
                tv.setTextSize(16);
                tv.setTextColor(getResources().getColor(R.color.text_primary));
                tv.setPadding(8, 8, 8, 8);
                list.addView(tv);
            }
        }

        return view;
    }
}
