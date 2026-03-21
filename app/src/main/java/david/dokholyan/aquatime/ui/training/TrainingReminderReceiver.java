package david.dokholyan.aquatime.ui.training;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import david.dokholyan.aquatime.MainActivity;
import david.dokholyan.aquatime.R;

public class TrainingReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "training_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Открыть приложение при клике на уведомление
            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Создаём канал только на Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Training Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Уведомления о запланированных тренировках");
                if (manager != null) manager.createNotificationChannel(channel);
            }

            // Используем иконку приложения, чтобы избежать ошибки, если ic_training отсутствует
            int iconRes = context.getApplicationInfo().icon != 0
                    ? context.getApplicationInfo().icon
                    : R.mipmap.ic_launcher;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(iconRes)
                    .setContentTitle("🏊 Тренировка через 1 час")
                    .setContentText("Не забудь про тренировку!")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            if (manager != null) {
                // Используем уникальный id — можно улучшить, привязывая к времени
                manager.notify((int) System.currentTimeMillis(), builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}