package david.dokholyan.aquatime.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import david.dokholyan.aquatime.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvDetails, tvLevel, tvMainRating, tvCountry, tvAchievements;
    private ImageView imgProfile;
    private ProgressBar pbAvatarLoading;
    private SharedPreferences langPrefs;

    private ActivityResultLauncher<Intent> imagePicker;
    private static final String DEFAULT_EMOJI = "🏊‍♂️";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    private String cloudAllRes = "";
    private String cloudLastCompletedDate = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUid = currentUser.getUid();
        }

        langPrefs = requireContext().getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

        initViews(view);

        imagePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                Uri selectedImageUri = result.getData().getData();
                if (selectedImageUri != null && currentUid != null) {
                    Uri safeUri = copyUriToCache(selectedImageUri);
                    if (safeUri != null) {
                        processAndSaveImageToFirestore(safeUri);
                    } else {
                        if (pbAvatarLoading != null) pbAvatarLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), isEnglish() ? "Failed to process image" : "Ошибка обработки фото", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        if (currentUid != null) {
            loadProfileDataFromFirestore();
        } else {
            setupGuestMode();
        }

        return view;
    }

    private Uri copyUriToCache(Uri sourceUri) {
        try {
            Context context = getContext();
            if (context == null) return null;

            InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), "temp_avatar.jpg");
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        pbAvatarLoading = v.findViewById(R.id.pb_avatar_loading);

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

    private void setupGuestMode() {
        boolean en = isEnglish();
        tvName.setText("Guest");
        tvEmail.setText(en ? "Guest Mode" : "Гостевой режим");
        tvCountry.setText("📍");
        tvDetails.setText("📏 - " + (en ? "cm" : "см") + " | ⚖ - " + (en ? "kg" : "кг") + "\n🏊 " + (en ? "Style: Freestyle" : "Стиль: Вольный стиль"));
        tvAchievements.setText(en ? "No achievements added yet" : "Достижения еще не добавлены");
        tvMainRating.setText("45");
        updateLevelCardText(45);
        setEmojiAvatar(DEFAULT_EMOJI);
    }

    private void loadProfileDataFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean en = isEnglish();

        if (currentUser != null && currentUser.getEmail() != null) {
            tvEmail.setText(currentUser.getEmail());
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {

                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        if (firstName == null || firstName.isEmpty()) firstName = "Guest";
                        if (lastName == null) lastName = "";
                        tvName.setText((firstName + " " + lastName).trim());

                        String savedCountryIso = documentSnapshot.getString("nation_iso");
                        if (savedCountryIso == null || savedCountryIso.isEmpty()) {
                            tvCountry.setText("📍");
                        } else {
                            Locale displayLocale = en ? Locale.ENGLISH : new Locale("ru");
                            Locale countryLocale = new Locale("", savedCountryIso);
                            tvCountry.setText("📍 " + countryLocale.getDisplayCountry(displayLocale));
                        }

                        String styleLabel = en ? "Style: " : "Стиль: ";
                        String cmUnit = en ? "cm" : "см";
                        String kgUnit = en ? "kg" : "кг";
                        String height = documentSnapshot.getString("height");
                        String weight = documentSnapshot.getString("weight");
                        String styleKey = documentSnapshot.getString("style_key");

                        if (height == null || height.isEmpty()) height = "-";
                        if (weight == null || weight.isEmpty()) weight = "-";
                        if (styleKey == null) styleKey = "freestyle";

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
                        tvDetails.setText("📏 " + height + " " + cmUnit + " | ⚖ " + weight + " " + kgUnit + "\n🏊 " + styleLabel + userStyle);

                        if (tvAchievements != null) {
                            String achievementsText;
                            if (en) {
                                achievementsText = documentSnapshot.getString("achievements_en");
                                if (achievementsText == null || achievementsText.isEmpty())
                                    achievementsText = documentSnapshot.getString("achievements_ru");
                            } else {
                                achievementsText = documentSnapshot.getString("achievements_ru");
                                if (achievementsText == null || achievementsText.isEmpty())
                                    achievementsText = documentSnapshot.getString("achievements_en");
                            }

                            if (achievementsText == null || achievementsText.isEmpty()) {
                                tvAchievements.setText(en ? "No achievements added yet" : "Достижения еще не добавлены");
                            } else {
                                tvAchievements.setText(achievementsText);
                            }
                        }

                        cloudAllRes = documentSnapshot.getString("all_res");
                        if (cloudAllRes == null) cloudAllRes = "";

                        cloudLastCompletedDate = documentSnapshot.getString("last_completed_date");
                        if (cloudLastCompletedDate == null) cloudLastCompletedDate = "";

                        int updatedRating = calculateSwimRating();
                        tvMainRating.setText(String.valueOf(updatedRating));
                        updateLevelCardText(updatedRating);

                        String avatarType = documentSnapshot.getString("profile_type");
                        String avatarVal = documentSnapshot.getString("profile_val");
                        if (avatarType == null) avatarType = "emoji";
                        if (avatarVal == null) avatarVal = DEFAULT_EMOJI;
                        renderProfileImage(avatarType, avatarVal);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) setupGuestMode();
                });
    }


    private void processAndSaveImageToFirestore(Uri fileUri) {
        if (currentUid == null || getContext() == null) return;

        if (pbAvatarLoading != null) pbAvatarLoading.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();

                if (originalBitmap != null) {
                    int width = originalBitmap.getWidth();
                    int height = originalBitmap.getHeight();
                    int size = Math.min(width, height);

                    int x = (width - size) / 2;
                    int y = (height - size) / 2;

                    Bitmap squaredBitmap = Bitmap.createBitmap(originalBitmap, x, y, size, size);

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(squaredBitmap, 200, 200, true);

                    if (squaredBitmap != scaledBitmap) squaredBitmap.recycle();
                    originalBitmap.recycle();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                    byte[] imageBytes = baos.toByteArray();
                    scaledBitmap.recycle();

                    String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        saveAvatarToFirestore("base64", base64Image);
                    });
                } else {
                    hideLoadingAndShowError();
                }
            } catch (Exception e) {
                e.printStackTrace();
                hideLoadingAndShowError();
            }
        });
    }

    private void hideLoadingAndShowError() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (pbAvatarLoading != null) pbAvatarLoading.setVisibility(View.GONE);
            Toast.makeText(getContext(), isEnglish() ? "Upload failed" : "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveAvatarToFirestore(String type, String value) {
        if (currentUid == null) return;

        Map<String, Object> avatarData = new HashMap<>();
        avatarData.put("profile_type", type);
        avatarData.put("profile_val", value);

        db.collection("users").document(currentUid)
                .set(avatarData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        renderProfileImage(type, value);
                    }
                })
                .addOnFailureListener(e -> {
                    if (pbAvatarLoading != null) pbAvatarLoading.setVisibility(View.GONE);
                });
    }


    private void renderProfileImage(String type, String val) {
        if (pbAvatarLoading != null) pbAvatarLoading.setVisibility(View.GONE);

        if ("base64".equals(type)) {
            try {
                byte[] decodedString = Base64.decode(val, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (bitmap != null) {
                    imgProfile.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imgProfile.setImageDrawable(createCircularBitmap(bitmap));
                } else {
                    setEmojiAvatar(DEFAULT_EMOJI);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                    if (w == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*");
                        imagePicker.launch(intent);
                    } else {
                        showEmojiSelectionDialog();
                    }
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
            saveAvatarToFirestore("emoji", emojis[position]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showLogoutDialog() {
        boolean en = isEnglish();
        String title = en ? "Logout Account" : "Выход из аккаунта";
        String msg = en ? "Are you sure you want to log out?" : "Вы уверены, что хотите выйти?";
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
            mAuth.signOut();
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

    private boolean isEnglish() {
        return langPrefs.getString("app_lang", "ru").equalsIgnoreCase("en");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
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

    private int calculateSwimRating() {
        if (cloudAllRes.isEmpty()) return 45;

        String[] entries = cloudAllRes.split(";");
        int totalMeters = 0;

        for (String entry : entries) {
            try {
                String[] p = entry.split("\\|");
                int meters = Integer.parseInt(p[0].trim());
                totalMeters += meters;
            } catch (Exception ignored) {
            }
        }

        int progressPoints = (totalMeters / 1000) * 2;
        int rating = 45 + progressPoints;

        if (!cloudLastCompletedDate.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                java.util.Date lastWorkoutDate = sdf.parse(cloudLastCompletedDate);

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

        if (rating < 45) rating = 45;
        if (rating > 99) rating = 99;

        return rating;
    }
}