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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import david.dokholyan.aquatime.R;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvDetails, tvLevel, tvMainRating, tvCountry;
    private ImageView imgProfile;
    private SharedPreferences prefs;
    private ActivityResultLauncher<String> imagePicker;
    private static final String DEFAULT_EMOJI = "🏊‍♂️";

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
        imgProfile = v.findViewById(R.id.img_avatar);

        v.findViewById(R.id.btn_edit_profile).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.editProfileFragment));

        // ПЕРЕХОД В НАСТРОЙКИ
        v.findViewById(R.id.btn_settings).setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.settingsFragment));

        imgProfile.setOnClickListener(view -> showAvatarSelectionMenu());

        Button btnLogout = v.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> showLogoutDialog());
        }
    }

    private void loadProfileData() {
        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        String firstName = prefs.getString("firstName", "David");
        String lastName = prefs.getString("lastName", "");
        tvName.setText(firstName + " " + lastName);

        if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            tvEmail.setText(currentUser.getEmail());
        } else {
            tvEmail.setText("Гостевой режим");
        }

        String nation = prefs.getString("nation", "Армения 🇦🇲");
        tvCountry.setText("📍 " + nation);

        tvDetails.setText("📏 " + prefs.getString("height", "-") + " см | ⚖ " +
                prefs.getString("weight", "-") + " кг\n🏊 Стиль: " +
                prefs.getString("style", "Не выбран"));

        int meters = prefs.getInt("total_meters", 0);
        int trainings = prefs.getInt("trainings_count", 0);
        int xp = meters + (trainings * 50);
        tvMainRating.setText(String.valueOf(xp));

        if (xp < 1000) tvLevel.setText("Уровень: Beginner 🟢");
        else if (xp < 5000) tvLevel.setText("Уровень: Amateur 🔵");
        else tvLevel.setText("Уровень: Pro 🟣");

        renderProfileImage();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти? Все данные профиля будут удалены.")
                .setPositiveButton("Выйти", (dialog, which) -> performLogout())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void performLogout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        if (prefs != null) {
            prefs.edit().clear().apply();
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
        String[] options = {"📸 Галерея", "✨ Выбрать Эмодзи"};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (d, w) -> {
                    if (w == 0) imagePicker.launch("image/*");
                    else showEmojiSelectionDialog();
                }).show();
    }

    private void showEmojiSelectionDialog() {
        final String[] emojis = {
                "😀","😁","😂","🤣","😃","😄","😅","😆","😉","😊",
                "😋","😎","😍","😘","🥰","😗","😙","😚","🙂","🤗",
                "🤩","🤔","🤨","😐","😑","😶","🙄","😏","😣","😥",
                "😮","🤐","😯","😪","😫","🥱","😴","😌","😛","😜",
                "😝","🤤","😒","😓","😔","😕","🙃","🫠","🤑","😲",
                "☹️","🙁","😖","😞","😟","😤","😢","😭","😦","😧",
                "😨","😩","🤯","😬","😰","😱","🥵","🥶","😳","🤪",
                "😵","🥴","😠","😡","🤬","😷","🤒","🤕","🫡","🥳",
                "😇","🤠","🤡","👻","💀","☠️","👽","🤖","👾","🎃",
                "⚽","🏀","🏈","⚾","🥎","🎾","🏐","🏉","🥏","🎱",
                "🏓","🏸","🏒","🏑","🥍","🏏","🥅","⛳","🪁","🏹",
                "🎣","🤿","🥊","🥋","🎽","🛹","🛷","⛸️","🥌","🎿",
                "⛷️","🏂","🪂","🏋️","🤼","🤸","⛹️","🤺","🤾","🏌️",
                "🏇","🧘","🏄","🏊","🏊‍♂️","🏊‍♀️","🚣","🚴","🚴‍♂️","🚴‍♀️",
                "🚵","🏃","🏃‍♂️","🏃‍♀️","🚶","💪","🔥","🏆","🥇","🥈",
                "🥉","🏅","🎖️","🐶","🐱","🐭","🐹","🐰","🦊","🐻",
                "🐼","🐨","🐯","🦁","🐮","🐷","🐽","🐸","🐵","🙈",
                "🙉","🙊","🐒","🐔","🐧","🐦","🐤","🐣","🐥","🦆",
                "🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🪱","🐛",
                "🦋","🐌","🐞","🐜","🪰","🪲","🪳","🦟","🦗","🕷️",
                "🕸️","🦂","🐢","🐍","🦎","🦖","🦕","🐙","🦑","🪼",
                "🦐","🦞","🦀","🐡","🐠","🐟","🐬","🐳","🐋","🦈",
                "🐊","🐅","🐆","🦓","🦍","🦧","🐘","🦛","🦏","🐪",
                "🐫","🦒","🦘","🦬","🐃","🐂","🐄","🐎","🐖","🐏",
                "🐑","🦙","🐐","🦌","🐕","🐩","🦮","🐕‍🦺","🐈","🐈‍⬛",
                "🪶","🐓","🦃","🦤","🦚","🦜","🪽","🐇","🦝","🦨",
                "🦡","🦫","🦦","🦥","🐁","🐀","🐿️","🦔"
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
                .setTitle("Выберите стиль")
                .setView(gridView)
                .setNegativeButton("Отмена", null)
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
            byte[] buf = new byte[1024]; int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close(); is.close();
            return f.getAbsolutePath();
        } catch (Exception e) { return null; }
    }
}