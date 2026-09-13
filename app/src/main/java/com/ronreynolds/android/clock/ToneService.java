package com.ronreynolds.android.clock;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class ToneService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "tone_channel")
                .setContentTitle("Tone Clock Running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
        playTone();
        return START_STICKY;
    }

    private void playTone() {
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
