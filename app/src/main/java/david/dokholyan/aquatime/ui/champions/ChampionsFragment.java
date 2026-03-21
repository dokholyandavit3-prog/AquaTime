package david.dokholyan.aquatime.ui.champions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import david.dokholyan.aquatime.R;

public class ChampionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_champions_full, container, false);
        LinearLayout list = view.findViewById(R.id.champions_list);
        Button back = view.findViewById(R.id.btn_back);
        back.setOnClickListener(v -> requireActivity().onBackPressed());

        Champion[] champions = {
                new Champion("Майкл Фелпс", "🏅 28 олимпийских медалей",
                        "«Плыви не ради победы, а ради совершенства.»",
                        getDrawableIdByName("phelps"),
                        "🏆 22 золота, 3 серебра, 3 бронзы."),
                new Champion("Иэн Торп", "🏅 5 золотых медалей",
                        "«Дисциплина — это то, что делает чемпиона.»",
                        getDrawableIdByName("ian_thorpe"),
                        "🏆 11 золотых медалей чемпионатов мира."),
                new Champion("Александр Попов", "🏅 4 золотых медали",
                        "«Контроль дыхания — контроль победы.»",
                        getDrawableIdByName("alexander_popov"),
                        "🏆 Олимпийский чемпион 1992 и 1996."),
                new Champion("Кэти Ледеки", "🏅 7 золотых медалей",
                        "«Ты должна чувствовать каждую волну.»",
                        getDrawableIdByName("ledecky"),
                        "🏆 Рекордсменка на 800 и 1500 м."),
                new Champion("Райан Лохте", "🏅 12 олимпийских медалей",
                        "«Каждый заплыв — это возможность показать лучшее в себе.»",
                        getDrawableIdByName("ryan_lochte"),
                        "🏆 6 золота, 3 серебра, 3 бронзы."),
                new Champion("Сара Шёстрём", "🏅 6 олимпийских медалей",
                        "«Не бойся быть первой.»",
                        getDrawableIdByName("sarah_sjostrom"),
                        "🏆 Рекорды на 50 и 100 м баттерфляй."),
                new Champion("Кэйлеб Дрессел", "🏅 7 олимпийских медалей",
                        "«Каждый гребок имеет значение.»",
                        getDrawableIdByName("caeleb_dressel"),
                        "🏆 Рекордсмен на 50 и 100 м вольным стилем.")
        };

        for (Champion champ : champions) {
            View card = inflater.inflate(R.layout.item_champion_card, list, false);

            ImageView photo = card.findViewById(R.id.champion_photo);
            TextView name = card.findViewById(R.id.champion_name);
            TextView medals = card.findViewById(R.id.champion_medals);
            TextView quote = card.findViewById(R.id.champion_quote);
            Button more = card.findViewById(R.id.btn_more);

            name.setText(champ.name);
            medals.setText(champ.medals);
            quote.setText(champ.quote);

            if (champ.photoRes != 0)
                photo.setImageResource(champ.photoRes);

            more.setOnClickListener(v -> openAthleteWiki(v, champ.name));

            list.addView(card);
        }

        return view;
    }

    private void openAthleteWiki(View v, String name) {
        String url = "https://en.wikipedia.org/wiki/Michael_Phelps";
        if (name.contains("Торп")) url = "https://en.wikipedia.org/wiki/Ian_Thorpe";
        else if (name.contains("Попов")) url = "https://ru.wikipedia.org/wiki/Попов,_Александр_Владимирович_(пловец)";
        else if (name.contains("Ледеки")) url = "https://en.wikipedia.org/wiki/Katie_Ledecky";
        else if (name.contains("Лохте")) url = "https://en.wikipedia.org/wiki/Ryan_Lochte";
        else if (name.contains("Шёстр")) url = "https://en.wikipedia.org/wiki/Sarah_Sj%C3%B6str%C3%B6m";
        else if (name.contains("Дрессел")) url = "https://en.wikipedia.org/wiki/Caeleb_Dressel";

        Bundle args = new Bundle();
        args.putString("url", url);
        Navigation.findNavController(v).navigate(R.id.athleteDetailFragment, args);
    }

    private int getDrawableIdByName(String name) {
        int id = requireContext().getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
        return id != 0 ? id : android.R.drawable.ic_menu_report_image;
    }

    private static class Champion {
        String name, medals, quote, details;
        int photoRes;

        Champion(String name, String medals, String quote, int photoRes, String details) {
            this.name = name;
            this.medals = medals;
            this.quote = quote;
            this.photoRes = photoRes;
            this.details = details;
        }
    }
}
