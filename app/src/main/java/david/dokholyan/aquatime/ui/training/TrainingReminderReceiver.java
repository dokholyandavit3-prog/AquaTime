package david.dokholyan.aquatime.ui.training;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import david.dokholyan.aquatime.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrainingReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "aquatime_reminders_channel";
    private static final String ACTION_START = "david.dokholyan.aquatime.START_TRAINING";
    private static final String ACTION_END = "david.dokholyan.aquatime.END_TRAINING";
    private static final String ACTION_CONFIRM = "david.dokholyan.aquatime.CONFIRM_FINISH_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);
        String action = intent.getAction();
        if (action == null) return;

        int distance = intent.getIntExtra("distance", 500);

        if (ACTION_START.equals(action)) {
            int minutesBefore = intent.getIntExtra("minutes_before", 30);
            String message = (minutesBefore == 0)
                    ? "Твоя тренировка начинается прямо сейчас! Пора в бассейн! 🏊‍♂️"
                    : "У тебя тренировка через " + minutesBefore + " минут!";

            showNotification(context, 801, "AquaTime: Тренировка", message, null);

        } else if (ACTION_END.equals(action)) {

            Intent confirmIntent = new Intent(context, TrainingReminderReceiver.class);
            confirmIntent.setAction(ACTION_CONFIRM);
            confirmIntent.putExtra("distance", distance);

            PendingIntent confirmPendingIntent = PendingIntent.getBroadcast(
                    context, 803, confirmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Action actionYes = new NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_add, "Да, обновить счетчик", confirmPendingIntent).build();

            showNotification(context, 802, "Тренировка завершена?",
                    "Ты закончил заплыв на " + distance + " м? Обновить статистику?", actionYes);

        } else if (ACTION_CONFIRM.equals(action)) {

            SharedPreferences prefs = context.getSharedPreferences("AquaTime", Context.MODE_PRIVATE);

            int currentTotalMeters = prefs.getInt("total_meters", 0);
            int currentTrainingsCount = prefs.getInt("trainings_count", 0);
            int currentWeeklyMeters = prefs.getInt("weekly_meters", 0);


            int calcTotalSeconds = Math.max(60, (distance / 50) * 45);
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", calcTotalSeconds / 60, calcTotalSeconds % 60);
            String fullDateStr = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

            String historyEntry = distance + " | " + formattedTime + " | " + fullDateStr;
            String oldHistory = prefs.getString("all_res", "");
            String updatedHistory = oldHistory.isEmpty() ? historyEntry : historyEntry + ";" + oldHistory;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("all_res", updatedHistory);
            editor.putInt("total_meters", currentTotalMeters + distance);
            editor.putInt("trainings_count", currentTrainingsCount + 1);
            editor.putInt("weekly_meters", currentWeeklyMeters + distance);
            editor.apply();


            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(802);
            }
        }
    }

    private void showNotification(Context context, int id, String title, String text, NotificationCompat.Action action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (action != null) {
            builder.addAction(action);
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(id, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Напоминания AquaTime", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Уведомления о запланированных тренировках");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}