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
import david.dokholyan.aquatime.MainActivity;
import david.dokholyan.aquatime.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrainingReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "training_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("AquaTime", Context.MODE_PRIVATE);
        String action = intent.getAction();


        if ("ACTION_COMPLETE_WORKOUT".equals(action)) {
            int savedDist = intent.getIntExtra("meters", 1000);

            int currentWeeklyMeters = prefs.getInt("weekly_meters", 0);
            int totalTimeInWater = prefs.getInt("total_time_in_water", 0);
            int currentRankXp = prefs.getInt("user_experience", 0);


            int calcMinutes = Math.max(10, savedDist / 50);
            String date = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());


            String oldHistory = prefs.getString("measure_history_full", "");
            String newEntry = "📅 Выполнено по плану | " + savedDist + "м (" + date + ")";
            String updatedHistory = oldHistory.isEmpty() ? newEntry : newEntry + ";" + oldHistory;


            prefs.edit()
                    .putInt("weekly_meters", currentWeeklyMeters + savedDist)
                    .putInt("total_time_in_water", totalTimeInWater + calcMinutes)
                    .putInt("user_experience", currentRankXp + 100) // +100 XP за плановую тренировку!
                    .putInt("last_dist", savedDist)
                    .putString("last_date", date)
                    .putString("last_style", "По расписанию")
                    .putString("measure_history_full", updatedHistory)

                    .putInt("saved_total_dist", 0)
                    .putString("saved_wu", "")
                    .putString("saved_main", "")
                    .putString("saved_cd", "")
                    .apply();


            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(1);
            }
            return;
        }


        int meters = intent.getIntExtra("meters", 1000);
        String message = "Твое время тренировки завершено. Ты выполнил заплыв на " + meters + "м?";

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, flags);


        Intent yesIntent = new Intent(context, TrainingReminderReceiver.class);
        yesIntent.setAction("ACTION_COMPLETE_WORKOUT");
        yesIntent.putExtra("meters", meters);

        int actionFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent yesPendingIntent = PendingIntent.getBroadcast(context, 1, yesIntent, actionFlags);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "AquaTime", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🏊‍♂️ AquaTime: Тренерский отчет")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(android.R.drawable.checkbox_on_background, "Да, выполнил! ✅", yesPendingIntent);

        if (manager != null) manager.notify(1, builder.build());
    }
}