package com.korkutsoftware.hilalim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.korkutsoftware.hilalim.util.QuranPrefsHelper;
import com.korkutsoftware.hilalim.util.QuranReciterHelper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class QuranAudioService extends Service {

    private static final String CHANNEL_ID = "Hilalim_Quran_Audio_Channel";
    private static final int NOTIFICATION_ID = 9101;

    public static final String ACTION_PLAY_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_PLAY_VERSE";
    public static final String ACTION_TOGGLE_PLAY_PAUSE = "com.korkutsoftware.hilalim.ACTION_QURAN_TOGGLE";
    public static final String ACTION_NEXT_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_NEXT";
    public static final String ACTION_PREV_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_PREV";
    public static final String ACTION_STOP = "com.korkutsoftware.hilalim.ACTION_QURAN_STOP";
    public static final String ACTION_SEEK_TO = "com.korkutsoftware.hilalim.ACTION_QURAN_SEEK";
    public static final String ACTION_RELOAD_SETTINGS = "com.korkutsoftware.hilalim.ACTION_QURAN_RELOAD";

    public static final String EXTRA_SURAH_ID = "extra_surah_id";
    public static final String EXTRA_SURAH_NAME = "extra_surah_name";
    public static final String EXTRA_VERSE_ID = "extra_verse_id";
    public static final String EXTRA_MAX_VERSES = "extra_max_verses";
    public static final String EXTRA_SEEK_POS_MS = "extra_seek_pos_ms";

    public interface AudioStateListener {
        void onAudioStateChanged(boolean active, boolean playing, int surahId, int verseId,
                                 String surahName, long positionMs, long durationMs, int repeatTarget);
    }

    private static final Set<AudioStateListener> LISTENERS = new CopyOnWriteArraySet<>();

    private static volatile boolean active = false;
    private static volatile boolean playing = false;
    private static volatile int currentSurahId = 1;
    private static volatile int currentVerseId = 1;
    private static volatile int currentMaxVerses = 7;
    private static volatile String currentSurahName = "";
    private static volatile long currentPosMs = 0L;
    private static volatile long currentDurMs = 0L;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private int repeatRemaining = 1;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && active) {
                currentPosMs = Math.max(0L, player.getCurrentPosition());
                long dur = player.getDuration();
                currentDurMs = dur != C.TIME_UNSET ? Math.max(0L, dur) : 0L;
                notifyListeners();
                if (player.isPlaying()) {
                    progressHandler.postDelayed(this, 400);
                }
            }
        }
    };

    public static void addListener(AudioStateListener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(AudioStateListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPlayingNow() {
        return active && playing;
    }

    public static int getCurrentSurahId() {
        return currentSurahId;
    }

    public static int getCurrentVerseId() {
        return currentVerseId;
    }

    public static String getCurrentSurahName() {
        return currentSurahName;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attrs, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        mediaSession = new MediaSession.Builder(this, player).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                playing = isPlaying;
                updateNotification();
                progressHandler.removeCallbacks(progressRunnable);
                if (isPlaying) {
                    progressHandler.post(progressRunnable);
                } else {
                    notifyListeners();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    currentDurMs = dur != C.TIME_UNSET ? Math.max(0L, dur) : 0L;
                    notifyListeners();
                } else if (playbackState == Player.STATE_ENDED) {
                    handleVerseEnded();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Toast.makeText(getApplicationContext(), R.string.quran_audio_network_error, Toast.LENGTH_LONG).show();
                stopAudioService();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY_VERSE:
                currentSurahId = intent.getIntExtra(EXTRA_SURAH_ID, 1);
                currentVerseId = intent.getIntExtra(EXTRA_VERSE_ID, 1);
                currentMaxVerses = Math.max(1, intent.getIntExtra(EXTRA_MAX_VERSES, 7));
                String sName = intent.getStringExtra(EXTRA_SURAH_NAME);
                currentSurahName = sName != null ? sName : getString(R.string.quran_surah_format, currentSurahId);
                repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                startForeground(NOTIFICATION_ID, buildNotification());
                playCurrentVerse();
                break;

            case ACTION_TOGGLE_PLAY_PAUSE:
                if (player != null && active) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                }
                break;

            case ACTION_NEXT_VERSE:
                if (active && currentVerseId < currentMaxVerses) {
                    currentVerseId++;
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_PREV_VERSE:
                if (active && currentVerseId > 1) {
                    currentVerseId--;
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_SEEK_TO:
                if (player != null && active) {
                    long posMs = intent.getLongExtra(EXTRA_SEEK_POS_MS, 0L);
                    player.seekTo(Math.max(0L, posMs));
                    currentPosMs = Math.max(0L, posMs);
                    notifyListeners();
                }
                break;

            case ACTION_RELOAD_SETTINGS:
                if (active) {
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_STOP:
                stopAudioService();
                break;
        }
        return START_NOT_STICKY;
    }

    private void playCurrentVerse() {
        if (player == null) return;
        active = true;
        currentPosMs = 0L;
        currentDurMs = 0L;
        int reciterIdx = QuranPrefsHelper.getReciterIndex(this);
        String url = QuranReciterHelper.buildVerseAudioUrl(reciterIdx, currentSurahId, currentVerseId);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        updateNotification();
        notifyListeners();
    }

    private void handleVerseEnded() {
        if (repeatRemaining > 1) {
            repeatRemaining--;
            if (player != null) {
                player.seekTo(0);
                player.play();
            }
            return;
        }
        if (currentVerseId < currentMaxVerses) {
            currentVerseId++;
            repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
            playCurrentVerse();
        } else {
            stopAudioService();
        }
    }

    private void stopAudioService() {
        active = false;
        playing = false;
        currentPosMs = 0L;
        currentDurMs = 0L;
        progressHandler.removeCallbacks(progressRunnable);
        if (player != null) {
            player.stop();
        }
        notifyListeners();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void notifyListeners() {
        int repeatTarget = QuranPrefsHelper.getRepeatCount(this);
        for (AudioStateListener listener : LISTENERS) {
            listener.onAudioStateChanged(
                    active,
                    playing,
                    currentSurahId,
                    currentVerseId,
                    currentSurahName,
                    currentPosMs,
                    currentDurMs,
                    repeatTarget
            );
        }
    }

    private void updateNotification() {
        if (!active) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.quran_audio_notif_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription(getString(R.string.quran_audio_notif_desc));
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Intent openIntent = new Intent(this, SurahDetailActivity.class);
        openIntent.putExtra("surah_id", currentSurahId);
        openIntent.putExtra("surah_name", currentSurahName);
        openIntent.putExtra("verse_id", currentVerseId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPending = PendingIntent.getActivity(
                this, 201, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent togglePending = PendingIntent.getService(
                this, 202,
                new Intent(this, QuranAudioService.class).setAction(ACTION_TOGGLE_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent nextPending = PendingIntent.getService(
                this, 203,
                new Intent(this, QuranAudioService.class).setAction(ACTION_NEXT_VERSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent stopPending = PendingIntent.getService(
                this, 204,
                new Intent(this, QuranAudioService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String reciterName = QuranReciterHelper.getReciterName(this, QuranPrefsHelper.getReciterIndex(this));
        int toggleIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        String toggleText = getString(playing ? R.string.quran_pause_verse_desc : R.string.quran_play_verse_desc);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_quran_rounded)
                .setContentTitle(currentSurahName + " • " + getString(R.string.quran_verse_format, currentVerseId))
                .setContentText(reciterName)
                .setContentIntent(contentPending)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .addAction(toggleIcon, toggleText, togglePending)
                .addAction(R.drawable.ic_skip_next, getString(R.string.desc_next_surah), nextPending)
                .addAction(R.drawable.ic_close, getString(R.string.ezan_stop_action), stopPending)
                .build();
    }

    @Override
    public void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        active = false;
        playing = false;
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}