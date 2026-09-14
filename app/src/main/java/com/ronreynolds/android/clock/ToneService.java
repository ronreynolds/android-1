package com.ronreynolds.android.clock;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * the service that actually provides the sound output
 */
public class ToneService extends Service {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("H m", Locale.US);
    private final String LOG_TAG = getClass().getSimpleName();
    private TextToSpeech textToSpeech;
    private volatile boolean ttsReady = false;
    private Handler handler;

    static float getPositiveFloat(SharedPreferences prefs, String key, float defaultValue) {
        float value;
        try {
            value = Float.parseFloat(prefs.getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException badData) {
            value = defaultValue;
        }
        return Math.max(0.1f, value);
    }

    /**
     * @return the communication channel to this service; null since we have none
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());  // used for async callback until TTS is ready

/*
        @SuppressWarnings("deprecation")    // required for API-23
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        float speechPitch = getPositiveFloat(prefs, "speech_pitch", 1.0f);
        float speechRate = getPositiveFloat(prefs, "speech_rate", 1.0f);
*/

        Log.d(LOG_TAG, "onCreate; creating TextToSpeech");
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(LOG_TAG, "TTS initialized");
                ttsReady = true;    // this tells us it's safe to use the TTS
            } else {
                Log.e(LOG_TAG, "TTS init failed");
            }
        });
    }

    /**
     * called every time an intent/event is sent to the service.
     *
     * @param intent  The Intent supplied to {@link android.content.Context#startService},
     *                as given.  This may be null if the service is being restarted after
     *                its process has gone away, and it had previously returned anything
     *                except {@link #START_STICKY_COMPATIBILITY}.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to
     *                start.  Use with {@link #stopSelfResult(int)}.
     * @return tells caller to keep this service alive between calls
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        startForeground(1, new NotificationCompat.Builder(this, "tone_channel")
                .setContentTitle("Tone Clock Running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build());
        sayTime();
        return START_STICKY;
    }

    private void sayTime() {
        if (!ttsReady) {
            Log.d(LOG_TAG, "TTS not ready; adding recursive delayed callback");
            handler.postDelayed(this::sayTime, 100);    // call us back in 100ms
        } else {
            String text;
            synchronized (dateFormat) { // because SimpleDateFormat isn't thread-safe
                text = dateFormat.format(new Date());
            }
            Log.d(LOG_TAG, "sayTime - " + text);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ToneService.sayTime");
        }
    }

    // old A-440 beep
    private void playTone() {
        Log.d(LOG_TAG, "playTone");
        int sampleRate = 44100;
        int durationMs = 200;
        int numSamples = durationMs * sampleRate / 1000;

        double[] samples = new double[numSamples];
        for (int i = 0; i < numSamples; i++) {
            samples[i] = Math.sin(2 * Math.PI * 440 * i / sampleRate);
        }

        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            buffer[i] = (byte) (samples[i] * 127);
        }

        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_8BIT,
                buffer.length,
                AudioTrack.MODE_STATIC
        );

        track.write(buffer, 0, buffer.length);
        track.play();
    }
}
