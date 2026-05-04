package david.dokholyan.aquatime.ui.champions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import david.dokholyan.aquatime.R;

public class ChampionsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Надуваем макет. Убедись, что в XML-файле используется LinearLayout с ID champions_list
        View view = inflater.inflate(R.layout.fragment_champions_full, container, false);

        // Находим контейнер для карточек
        LinearLayout list = view.findViewById(R.id.champions_list);

        // Настраиваем кнопку "Назад"
        if (view.findViewById(R.id.btn_back) != null) {
            view.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());
        }

        // Получаем полный список чемпионов
        Champion[] champions = getChampionsList();

        // Программно добавляем карточки для каждого чемпиона
        for (Champion champ : champions) {
            // Надуваем макет карточки
            View card = inflater.inflate(R.layout.item_champion_card, list, false);

            // Находим элементы внутри карточки
            ImageView photo = card.findViewById(R.id.champion_photo);
            TextView name = card.findViewById(R.id.champion_name);
            TextView medals = card.findViewById(R.id.champion_medals);
            TextView quote = card.findViewById(R.id.champion_quote);
            MaterialButton more = card.findViewById(R.id.btn_more);

            // Заполняем данные
            name.setText(champ.name);
            medals.setText(champ.medals);
            quote.setText(champ.quote);

            // Устанавливаем фото, если оно есть
            if (champ.photoRes != 0) {
                photo.setImageResource(champ.photoRes);
            } else {
                // Иконка по умолчанию, если фото не найдено
                photo.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Настраиваем кнопку "Подробнее"
            more.setOnClickListener(v -> {
                // Передаем URL в бандл для WebView
                Bundle args = new Bundle();
                args.putString("url", champ.wikiUrl);
                // Навигация согласно твоему nav_graph.xml
                Navigation.findNavController(v).navigate(R.id.athleteDetailFragment, args);
            });

            // Добавляем готовую карточку в контейнер
            list.addView(card);
        }

        return view;
    }

    // --- Вспомогательные методы и классы ---

    private Champion[] getChampionsList() {
        return new Champion[]{
                // 1. Майкл Фелпс
                new Champion(
                        "Майкл Фелпс",
                        "🏅 28 олимпийских медалей",
                        "«Плыви не ради победы, а ради совершенства.»",
                        getDrawableId("phelps"),
                        "https://en.wikipedia.org/wiki/Michael_Phelps"
                ),
                // 2. Иэн Торп
                new Champion(
                        "Иэн Торп",
                        "🏅 5 золотых медалей",
                        "«Дисциплина — это то, что делает чемпиона.»",
                        getDrawableId("ian_thorpe"),
                        "https://en.wikipedia.org/wiki/Ian_Thorpe"
                ),
                // 3. Александр Попов
                new Champion(
                        "Александр Попов",
                        "🏅 4 золотых медали",
                        "«Контроль дыхания — контроль победы.»",
                        getDrawableId("alexander_popov"),
                        "https://ru.wikipedia.org/wiki/Попов,_Александр_Владимирович_(пловец)"
                ),
                // 4. Кэти Ледеки
                new Champion(
                        "Кэти Ледеки",
                        "🏅 7 золотых медалей",
                        "«Ты должна чувствовать каждую волну.»",
                        getDrawableId("ledecky"),
                        "https://en.wikipedia.org/wiki/Katie_Ledecky"
                ),
                // 5. Райан Лохте
                new Champion(
                        "Райан Лохте",
                        "🏅 12 олимпийских медалей",
                        "«Каждый заплыв — это возможность показать лучшее в себе.»",
                        getDrawableId("ryan_lochte"),
                        "https://en.wikipedia.org/wiki/Ryan_Lochte"
                ),
                // 6. Сара Шёстрём
                new Champion(
                        "Сара Шёстрём",
                        "🏅 6 олимпийских медалей",
                        "«Не бойся быть первой.»",
                        getDrawableId("sarah_sjostrom"),
                        "https://en.wikipedia.org/wiki/Sarah_Sj%C3%B6str%C3%B6m"
                ),
                // 7. Кэйлеб Дрессел
                new Champion(
                        "Кэйлеб Дрессел",
                        "🏅 7 олимпийских медалей",
                        "«Каждый гребок имеет значение.»",
                        getDrawableId("caeleb_dressel"),
                        "https://en.wikipedia.org/wiki/Caeleb_Dressel"
                )
        };
    }

    // Безопасный поиск ID изображения в drawable
    private int getDrawableId(String name) {
        int id = requireContext().getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
        // Если изображение не найдено, возвращаем 0
        return id;
    }

    // Класс данных для чемпиона
    private static class Champion {
        String name, medals, quote, wikiUrl;
        int photoRes;

        Champion(String name, String medals, String quote, int photoRes, String wikiUrl) {
            this.name = name;
            this.medals = medals;
            this.quote = quote;
            this.photoRes = photoRes;
            this.wikiUrl = wikiUrl;
        }
    }
}