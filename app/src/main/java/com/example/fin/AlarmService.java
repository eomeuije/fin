package com.example.fin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private MediaPlayer mediaPlayer;
    private boolean isRunning;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String state = intent.getStringExtra("state");
        if (!this.isRunning && state.equals("on")) {
            // 알람음 재생 OFF, 알람음 시작 상태
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            this.mediaPlayer = MediaPlayer.create(this, uri);
            this.mediaPlayer.start();

            this.isRunning = true;

        } else if (this.isRunning & state.equals("off")) {
            // 알람음 재생 ON, 알람음 중지 상태
            this.mediaPlayer.stop();
            this.mediaPlayer.reset();
            this.mediaPlayer.release();

            this.isRunning = false;


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }
            return START_NOT_STICKY;
        }
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "app:alarm");
        wakeLock.acquire();
        wakeLock.release();
        // Foreground 에서 실행되면 Notification 을 보여줘야 됨
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Oreo(26) 버전 이후 버전부터는 channel 이 필요함
            String channelId = createNotificationChannel();

            Intent intentOff = new Intent(this, AlarmReceiver.class);
            intentOff.putExtra("state", "off");
            //sendBroadcast(intent);
            PendingIntent snoozePendingIntent =
                    PendingIntent.getBroadcast(this, 2, intentOff, 0);
            //full
            Intent fullScreenIntent = new Intent(this, MainActivity.class);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                    fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
            Notification notification = builder.setOngoing(true)
                    .setContentTitle(intent.getStringExtra("busNM") + "번 버스가 " + intent.getStringExtra("stationNM") + "에 곧 도착합니다")
                    .setSmallIcon(R.drawable.busicon)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .addAction(R.drawable.ic_launcher_background, "알람 종료",
                            snoozePendingIntent)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();
            startForeground(1, notification);
        }

        return START_NOT_STICKY;
    }

    private String createNotificationChannel() {
        String channelId = "Alarm";
        String channelName = "버스 알람";
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            //channel.setDescription(channelName);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        return channelId;
    }
}
