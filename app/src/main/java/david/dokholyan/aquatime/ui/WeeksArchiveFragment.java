package david.dokholyan.aquatime.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import david.dokholyan.aquatime.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class WeeksArchiveFragment extends Fragment {

    private SharedPreferences prefs;
    private LinearLayout container;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weeks_archive, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        this.container = view.findViewById(R.id.weeks_list_container);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        if (tvTitle != null && isEnglish()) {
            tvTitle.setText("Weeks Archive");
        }


        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v).navigateUp());

        buildWeeksArchive();
        return view;
    }

    private void buildWeeksArchive() {
        if (container == null) return;
        container.removeAllViews();

        String data = prefs.getString("all_res", "");
        if (data.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText(isEnglish() ? "Archive is empty" : "Архив пуст");
            emptyTv.setTextColor(Color.GRAY);
            container.addView(emptyTv);
            return;
        }

        String[] entries = data.split(";");

        HashMap<Integer, Integer> weekMeters = new HashMap<>();
        HashMap<Integer, Integer> weekCounts = new HashMap<>();
        HashMap<Integer, ArrayList<String>> weekDetails = new HashMap<>();
        ArrayList<Integer> sortedWeeks = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(Calendar.YEAR);

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                Date date = sdf.parse(p[2].trim());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                int dist = Integer.parseInt(p[0].trim());
                int wYear = cal.get(Calendar.YEAR);
                int wWeek = cal.get(Calendar.WEEK_OF_YEAR);

                if (wYear == currentYear && wWeek < currentWeek) {
                    if (!sortedWeeks.contains(wWeek)) {
                        sortedWeeks.add(wWeek);
                        weekMeters.put(wWeek, 0);
                        weekCounts.put(wWeek, 0);
                        weekDetails.put(wWeek, new ArrayList<>());
                    }
                    weekMeters.put(wWeek, weekMeters.get(wWeek) + dist);
                    weekCounts.put(wWeek, weekCounts.get(wWeek) + 1);


                    weekDetails.get(wWeek).add("• " + p[0].trim() + "м — (" + p[2].trim() + ")");
                }
            } catch (Exception ignored) {}
        }


        sortedWeeks.sort((w1, w2) -> Integer.compare(w2, w1));

        boolean en = isEnglish();

        for (int week : sortedWeeks) {
            LinearLayout rowHeader = new LinearLayout(getContext());
            rowHeader.setOrientation(LinearLayout.HORIZONTAL);
            rowHeader.setPadding(16, 24, 16, 24);
            rowHeader.setBackgroundColor(Color.parseColor("#F5F7FA"));
            rowHeader.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 8, 0, 8);
            rowHeader.setLayoutParams(rowParams);

            TextView tvInfo = new TextView(getContext());
            String trainWord = en ? "workouts" : getTrainingWordForm(weekCounts.get(week));
            String text = en ? "Week " + week + " (" + currentYear + ") | " + weekMeters.get(week) + "m | " + weekCounts.get(week) + " " + trainWord
                    : "Неделя " + week + " (" + currentYear + "г) | " + weekMeters.get(week) + "м | " + weekCounts.get(week) + " " + trainWord;

            tvInfo.setText(text);
            tvInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvInfo.setTextColor(Color.parseColor("#1C2D42"));
            tvInfo.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));

            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvInfo.setLayoutParams(infoParams);
            rowHeader.addView(tvInfo);

            ImageView ivArrow = new ImageView(getContext());
            ivArrow.setImageResource(android.R.drawable.arrow_down_float);
            rowHeader.addView(ivArrow);

            LinearLayout detailsBox = new LinearLayout(getContext());
            detailsBox.setOrientation(LinearLayout.VERTICAL);
            detailsBox.setPadding(32, 12, 16, 12);
            detailsBox.setVisibility(View.GONE);
            detailsBox.setBackgroundColor(Color.parseColor("#FFFFFF"));

            for (String subTask : weekDetails.get(week)) {
                TextView tvSub = new TextView(getContext());
                tvSub.setText(subTask);
                tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvSub.setTextColor(Color.DKGRAY);
                tvSub.setPadding(0, 6, 0, 6);
                detailsBox.addView(tvSub);
            }

            rowHeader.setOnClickListener(v -> {
                if (detailsBox.getVisibility() == View.GONE) {
                    detailsBox.setVisibility(View.VISIBLE);
                    ivArrow.setImageResource(android.R.drawable.arrow_up_float);
                } else {
                    detailsBox.setVisibility(View.GONE);
                    ivArrow.setImageResource(android.R.drawable.arrow_down_float);
                }
            });

            container.addView(rowHeader);
            container.addView(detailsBox);
        }
    }

    private String getTrainingWordForm(int count) {
        if (count % 10 == 1 && count % 100 != 11) return "тренировка";
        else if ((count % 10 >= 2 && count % 10 <= 4) && (count % 100 < 10 || count % 100 >= 20)) return "тренировки";
        else return "тренировок";
    }

    private boolean isEnglish() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("en");
    }
}