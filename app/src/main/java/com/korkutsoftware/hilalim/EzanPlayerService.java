package com.korkutsoftware.hilalim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
public class EzanPlayerService extends Service {

    private static final String TAG = "EzanPlayerService";
    private static final String CHANNEL_ID = "Hilalim_Ezan_Player_Channel";
    private static final int NOTIFICATION_ID = 9001;

    public static final String ACTION_PLAY_EZAN = "com.korkutsoftware.hilalim.ACTION_PLAY_EZAN";
    public static final String ACTION_STOP_EZAN = "com.korkutsoftware.hilalim.ACTION_STOP_EZAN";
    public static final String EXTRA_PRAYER_NAME = "extra_prayer_name";
    public static final String PREF_EZAN_SOUND_INDEX = "notif_ezan_sound_index";
    public static final int SOUND_CLASSIC = 0;
    public static final int SOUND_ISTANBUL = 1;
    public static final int SOUND_MAKKAH = 2;
    public static final int SOUND_SHORT_TONE = 3;
    public static final int SOUND_SILENT = 4;
    private static volatile boolean playing = false;
    private MediaPlayer mediaPlayer;

    public static boolean isPlaying() {
        return playing;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_EZAN.equals(intent.getAction())) {
            stopEzan();
            return START_NOT_STICKY;
        }

        String prayerName = intent != null ? intent.getStringExtra(EXTRA_PRAYER_NAME) : null;
        if (prayerName == null || prayerName.isEmpty()) {
            prayerName = getString(R.string.app_name);
        }

        startForeground(NOTIFICATION_ID, buildNotification(prayerName));
        startAudio();
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String prayerName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.ezan_playing_title),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent stopIntent = new Intent(this, EzanPlayerService.class);
        stopIntent.setAction(ACTION_STOP_EZAN);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent openIntent = new Intent(this, Dashboard.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mosque)
                .setContentTitle(getString(R.string.ezan_playing_title))
                .setContentText(getString(R.string.ezan_playing_desc, prayerName))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_stop, getString(R.string.ezan_stop_action), stopPendingIntent)
                .build();
    }

    private void startAudio() {
        releasePlayer();
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        int soundIndex = prefs.getInt(PREF_EZAN_SOUND_INDEX, SOUND_CLASSIC);
        if (soundIndex == SOUND_SILENT) {
            stopEzan();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            Uri soundUri = resolveEzanSoundUri(soundIndex);
            mediaPlayer.setDataSource(this, soundUri);

            mediaPlayer.setOnCompletionListener(mp -> stopEzan());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopEzan();
                return true;
            });
            mediaPlayer.setOnPreparedListener(mp -> {
                playing = true;
                mp.start();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error playing Ezan audio", e);
            stopEzan();
        }
    }

    private Uri resolveEzanSoundUri(int soundIndex) {
        if (soundIndex == SOUND_SHORT_TONE) {
            Uri notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (notifUri != null) return notifUri;
        }
        int resId = R.raw.ezan;
        if (soundIndex == SOUND_ISTANBUL) {
            int custom = getResources().getIdentifier("ezan_istanbul", "raw", getPackageName());
            if (custom != 0) resId = custom;
        } else if (soundIndex == SOUND_MAKKAH) {
            int custom = getResources().getIdentifier("ezan_mekke", "raw", getPackageName());
            if (custom != 0) resId = custom;
        }
        return Uri.parse("android.resource://" + getPackageName() + "/" + resId);
    }

    private void stopEzan() {
        playing = false;
        releasePlayer();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        playing = false;
        releasePlayer();
        super.onDestroy();
    }
}
