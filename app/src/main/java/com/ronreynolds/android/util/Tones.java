package com.ronreynolds.android.util;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * just to keep track of this code; might come in handy someday :)
 */
public class Tones {
    private final String LOG_TAG = getClass().getSimpleName();

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
