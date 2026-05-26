package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import david.dokholyan.aquatime.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvDetails, tvLevel, tvMainRating, tvCountry, tvAchievements;
    private ImageView imgProfile;
    private SharedPreferences prefs;
    private ActivityResultLauncher<String> imagePicker;
    private static final String DEFAULT_EMOJI = "🏊‍♂️";

    private ProgressBar pbFreestyle, pbBreast, pbFly;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        prefs = requireActivity().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(view);

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String path = copyToInternalStorage(uri);
                if (path != null) {
                    prefs.edit().putString("profile_type", "uri").putString("profile_val", path).apply();
                    renderProfileImage();
                }
            }
        });

        loadProfileData();

        return view;
    }

    private void initViews(View v) {
        tvName = v.findViewById(R.id.tv_name);
        tvEmail = v.findViewById(R.id.tv_email);
        tvDetails = v.findViewById(R.id.tv_age_weight);
        tvLevel = v.findViewById(R.id.tv_level);
        tvMainRating = v.findViewById(R.id.tv_main_rating);
        tvCountry = v.findViewById(R.id.tv_country);
        tvAchievements = v.findViewById(R.id.tv_profile_achievements);
        imgProfile = v.findViewById(R.id.img_avatar);

        pbFreestyle = v.findViewById(R.id.pb_freestyle);
        pbBreast = v.findViewById(R.id.pb_breast);
        pbFly = v.findViewById(R.id.pb_fly);

        v.findViewById(R.id.btn_edit_profile).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.editProfileFragment));

        v.findViewById(R.id.btn_settings).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.settingsFragment));

        imgProfile.setOnClickListener(view -> showAvatarSelectionMenu());

        Button btnLogout = v.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> showLogoutDialog());
        }
    }

    private void loadProfileData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean en = isEnglish();

        String firstName = prefs.getString("firstName", "Guest");
        String lastName = prefs.getString("lastName", "");
        tvName.setText((firstName + " " + lastName).trim());

        if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            tvEmail.setText(currentUser.getEmail());
        } else {
            tvEmail.setText(en ? "Guest Mode" : "Гостевой режим");
        }

        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseDatabase.getInstance().getReference("users").child(userId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists() && isAdded() && getContext() != null) {
                            SharedPreferences.Editor editor = prefs.edit();

                            if (snapshot.hasChild("all_res")) {
                                editor.putString("all_res", snapshot.child("all_res").getValue(String.class));
                            }
                            if (snapshot.hasChild("total_meters")) {
                                Integer tm = snapshot.child("total_meters").getValue(Integer.class);
                                if (tm != null) editor.putInt("total_meters", tm);
                            }
                            if (snapshot.hasChild("trainings_count")) {
                                Integer tc = snapshot.child("trainings_count").getValue(Integer.class);
                                if (tc != null) editor.putInt("trainings_count", tc);
                            }
                            editor.apply();

                            int updatedRating = calculateFifaRating();
                            tvMainRating.setText(String.valueOf(updatedRating));
                            updateLevelCardText(updatedRating);
                            calculateSwimSkills();
                        }
                    }).addOnFailureListener(Throwable::printStackTrace);
        }


        String savedCountryIso = prefs.getString("nation_iso", "");
        if (savedCountryIso.isEmpty()) {
            tvCountry.setText("📍");
        } else {
            Locale displayLocale = en ? Locale.ENGLISH : new Locale("ru");
            Locale countryLocale = new Locale("", savedCountryIso);
            tvCountry.setText("📍 " + countryLocale.getDisplayCountry(displayLocale));
        }


        String styleLabel = en ? "Style: " : "Стиль: ";
        String cmUnit = en ? "cm" : "см";
        String kgUnit = en ? "kg" : "кг";

        String styleKey = prefs.getString("style_key", "freestyle");
        String userStyle;

        if (en) {
            if (styleKey.equals("breaststroke")) userStyle = "Breaststroke";
            else if (styleKey.equals("butterfly")) userStyle = "Butterfly";
            else if (styleKey.equals("backstroke")) userStyle = "Backstroke";
            else if (styleKey.equals("medley")) userStyle = "Individual Medley";
            else userStyle = "Freestyle";
        } else {
            if (styleKey.equals("breaststroke")) userStyle = "Брасс";
            else if (styleKey.equals("butterfly")) userStyle = "Баттерфляй";
            else if (styleKey.equals("backstroke")) userStyle = "На спине";
            else if (styleKey.equals("medley")) userStyle = "Комплекс";
            else userStyle = "Вольный стиль";
        }

        tvDetails.setText("📏 " + prefs.getString("height", "-") + " " + cmUnit + " | ⚖ " +
                prefs.getString("weight", "-") + " " + kgUnit + "\n🏊 " + styleLabel + userStyle);

        // --- ДВУЯЗЫЧНЫЕ ДОСТИЖЕНИЯ ---
        if (tvAchievements != null) {
            String achievementsText;
            if (en) {
                achievementsText = prefs.getString("achievements_en", "");
                if (achievementsText.isEmpty()) achievementsText = prefs.getString("achievements_ru", "");
            } else {
                achievementsText = prefs.getString("achievements_ru", "");
                if (achievementsText.isEmpty()) achievementsText = prefs.getString("achievements_en", "");
            }

            if (achievementsText.isEmpty()) {
                tvAchievements.setText(en ? "No achievements added yet" : "Достижения еще не добавлены");
            } else {
                tvAchievements.setText(achievementsText);
            }
        }

        int cbRating = calculateFifaRating();
        tvMainRating.setText(String.valueOf(cbRating));
        updateLevelCardText(cbRating);

        calculateSwimSkills();
        renderProfileImage();
    }

    private void calculateSwimSkills() {
        if (pbFreestyle == null || pbBreast == null || pbFly == null) return;

        String data = prefs.getString("all_res", "");

        if (data.isEmpty()) {
            pbFreestyle.setProgress(10);
            pbBreast.setProgress(10);
            pbFly.setProgress(10);
            return;
        }

        String[] entries = data.split(";");
        int crawlMeters = 0;
        int breastMeters = 0;
        int flyMeters = 0;

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                if (p.length < 2) continue;

                int meters = Integer.parseInt(p[0].trim());
                String workoutStyle = p[1].toLowerCase();

                if (workoutStyle.contains("вольный") || workoutStyle.contains("кроль") || workoutStyle.contains("freestyle") || workoutStyle.contains("crawl")) {
                    crawlMeters += meters;
                } else if (workoutStyle.contains("брасс") || workoutStyle.contains("breaststroke")) {
                    breastMeters += meters;
                } else if (workoutStyle.contains("баттерфляй") || workoutStyle.contains("butterfly") || workoutStyle.contains("дельфин")) {
                    flyMeters += meters;
                } else {
                    crawlMeters += meters / 3;
                    breastMeters += meters / 3;
                    flyMeters += meters / 3;
                }
            } catch (Exception ignored) {
            }
        }

        int maxSkillThreshold = 5000;

        int crawlPercent = Math.min(100, 10 + (crawlMeters * 90 / maxSkillThreshold));
        int breastPercent = Math.min(100, 10 + (breastMeters * 90 / maxSkillThreshold));
        int flyPercent = Math.min(100, 10 + (flyMeters * 90 / maxSkillThreshold));

        pbFreestyle.setProgress(crawlPercent);
        pbBreast.setProgress(breastPercent);
        pbFly.setProgress(flyPercent);
    }

    private void updateLevelCardText(int rating) {
        boolean en = isEnglish();
        if (rating < 55) {
            tvLevel.setText(en ? "Card: Bronze 🟫" : "Карточка: Бронза 🟫");
        } else if (rating < 75) {
            tvLevel.setText(en ? "Card: Silver ⬜" : "Карточка: Серебро ⬜");
        } else if (rating < 90) {
            tvLevel.setText(en ? "Card: Gold 🟨" : "Карточка: Золото 🟨");
        } else {
            tvLevel.setText(en ? "Card: Elite 💎" : "Карточка: Элита 💎");
        }
    }

    private int calculateFifaRating() {
        String data = prefs.getString("all_res", "");
        if (data.isEmpty()) return 45;

        String[] entries = data.split(";");
        int totalTrainings = entries.length;
        int totalMeters = 0;

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                int meters = Integer.parseInt(p[0].trim());
                totalMeters += meters;
            } catch (Exception ignored) {
            }
        }

        double experiencePoints = Math.min(15.0, totalTrainings * 0.5);
        double distancePoints = Math.pow(totalMeters, 0.25) * 7.1;
        int rating = 45 + (int) (experiencePoints + distancePoints);

        if (totalTrainings > 0 && (totalMeters / totalTrainings) >= 1500) {
            rating += 2;
        }

        String lastDateStr = prefs.getString("last_completed_date", "");
        if (!lastDateStr.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault());
                java.util.Date lastWorkoutDate = sdf.parse(lastDateStr);

                if (lastWorkoutDate != null) {
                    long diffInMs = Math.abs(System.currentTimeMillis() - lastWorkoutDate.getTime());
                    long diffInDays = diffInMs / (1000 * 60 * 60 * 24);

                    if (diffInDays > 3) {
                        long lazyDays = diffInDays - 3;
                        int penalty = (int) (lazyDays * 2);
                        if (penalty > 15) penalty = 15;
                        rating -= penalty;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (rating > 85) {
            int overEightFive = rating - 85;
            rating = 85 + (overEightFive / 3);
        }

        if (rating < 45) rating = 45;
        if (rating > 99) rating = 99;

        return rating;
    }

    private void showLogoutDialog() {
        boolean en = isEnglish();
        String title = en ? "Logout Account" : "Выход из аккаунта";
        String msg = en ? "Are you sure you want to log out? Your local workout data will be kept." : "Вы уверены, что хотите выйти? Ваши локальные данные тренировок будут сохранены.";
        String pos = en ? "Log Out" : "Выйти";
        String neg = en ? "Cancel" : "Отмена";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(pos, (dialog, which) -> performLogout())
                .setNegativeButton(neg, null)
                .show();
    }

    private void performLogout() {
        try {
            FirebaseAuth.getInstance().signOut();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (getActivity() != null) {
            android.content.Intent intent = new android.content.Intent(getActivity(), david.dokholyan.aquatime.LoginActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    private void renderProfileImage() {
        String type = prefs.getString("profile_type", "emoji");
        String val = prefs.getString("profile_val", DEFAULT_EMOJI);

        if ("uri".equals(type)) {
            File f = new File(val);
            if (f.exists()) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
                if (bitmap != null) {
                    imgProfile.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imgProfile.setImageDrawable(createCircularBitmap(bitmap));
                } else {
                    setEmojiAvatar(DEFAULT_EMOJI);
                }
            } else {
                setEmojiAvatar(DEFAULT_EMOJI);
            }
        } else {
            setEmojiAvatar(val);
        }
    }

    private void setEmojiAvatar(String emoji) {
        imgProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imgProfile.setImageDrawable(getBitmapFromEmoji(emoji));
    }

    private Drawable createCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(bitmap, x, y, size, size);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        BitmapShader shader = new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float r = size / 2f;

        canvas.drawCircle(r, r, r, paint);

        return new BitmapDrawable(getResources(), output);
    }

    private Drawable getBitmapFromEmoji(String emoji) {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(160);
        paint.setTextAlign(Paint.Align.CENTER);

        float y = (canvas.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(emoji, canvas.getWidth() / 2f, y, paint);

        return new BitmapDrawable(getResources(), bitmap);
    }

    private void showAvatarSelectionMenu() {
        boolean en = isEnglish();
        String[] options = en ? new String[]{"📸 Gallery", "✨ Choose Emoji"} : new String[]{"📸 Галерея", "✨ Выбрать Эмодзи"};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (d, w) -> {
                    if (w == 0) imagePicker.launch("image/*");
                    else showEmojiSelectionDialog();
                }).show();
    }

    private void showEmojiSelectionDialog() {
        final String[] emojis = {
                "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊",
                "😋", "😎", "😍", "😘", "🥰", "😗", "😙", "😚", "🙂", "🤗",
                "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥",
                "😮", "🤐", "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜",
                "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🫠", "🤑", "😲",
                "☹️", "🙁", "😖", "😞", "😟", "😤", "😢", "😭", "😦", "😧",
                "😨", "😩", "🤯", "😬", "😰", "😱", "🥵", "🥶", "😳", "🤪",
                "😵", "🥴", "😠", "😡", "🤬", "😷", "🤒", "🤕", "🫡", "🥳",
                "😇", "🤠", "🤡", "👻", "💀", "☠️", "👽", "🤖", "👾", "🎃",
                "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
                "🏓", "🏸", "🏒", "🏑", "🥍", "👑", "🥅", "⛳", "🪁", "🏹",
                "🎣", "🤿", "🥊", "🥋", "🎽", "🛷", "⛸️", "🥌", "🎿",
                "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "⛹️", "🌟", "🤾", "🏌️",
                "🏇", "🧘", "🏄", "🏊", "🏊‍♂️", "🏊‍♀️", "🚣", "🚴", "🚴‍♂️", "🚴‍♀️",
                "🚵", "🏃", "🏃‍♂️", "🏃‍♀️", "🚶", "💪", "🔥", "🏆", "🥇", "🥈",
                "🥉", "🏅", "🎖️", "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻",
                "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈",
                "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆",
                "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛",
                "🦋", "🐌", "🐞", "🐜", "🪰", "🪲", "🪳", "🦟", "👑", "🕷️",
                "🕸️", "🦂", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🪼",
                "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈",
                "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪",
                "🐫", "🦒", "🦘", "🦬", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏",
                "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛",
                "🪶", "🐓", "🦃", "🦤", "🦚", "🦜", "🪽", "🐇", "🦝", "🦨",
                "🦡", "🦫", "🦦", "🦥", "🐁", "🐿️", "🦔"
        };

        GridView gridView = new GridView(requireContext());
        gridView.setNumColumns(5);
        gridView.setVerticalSpacing(18);
        gridView.setHorizontalSpacing(18);
        gridView.setPadding(25, 25, 25, 25);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                emojis
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setText(emojis[position]);
                tv.setTextSize(28f);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(8, 8, 8, 8);
                return tv;
            }
        };

        gridView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEnglish() ? "Select Emoji" : "Выберите эмодзи")
                .setView(gridView)
                .setNegativeButton(isEnglish() ? "Cancel" : "Отмена", null)
                .create();

        gridView.setOnItemClickListener((parent, view1, position, id) -> {
            prefs.edit()
                    .putString("profile_type", "emoji")
                    .putString("profile_val", emojis[position])
                    .apply();

            renderProfileImage();
            dialog.dismiss();
        });

        dialog.show();
    }

    private String copyToInternalStorage(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            File f = new File(requireContext().getFilesDir(), "profile_aqua.jpg");
            OutputStream os = new FileOutputStream(f);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close();
            is.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isEnglish() {
        return prefs.getString("app_lang", "ru").equalsIgnoreCase("en");
    }
}