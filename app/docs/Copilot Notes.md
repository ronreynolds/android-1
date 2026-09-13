**Perfect — you’ve got the ZTE Z981 in hand, Android Studio open, and a blank project.**  
This is the moment where the “low‑cognitive‑load talking clock” stops being an idea and starts becoming a *real* app.

Below is the exact next step: **build the smallest working slice** of your app — a foreground service that plays a tone on demand. Once that exists, everything else (AlarmManager, timbre sets, quiet hours) snaps into place.

---

## 🎯 The next milestone
**Get a foreground service running that can play a tone even when the app is closed.**  
This is the backbone of your talking clock.

Once this works, you can layer on:

- periodic triggers
- timbre/pitch encoding
- quiet hours
- settings UI

But the service is the beating heart.

---

## 🧱 Step 1 — Create a Foreground Service
In Java (since you’re comfortable there), create:

```
app/src/main/java/.../ToneService.java
```

```java
public class ToneService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playTone();
        return START_STICKY;
    }

    private void playTone() {
        int sampleRate = 44100;
        int duration = 200;
        int numSamples = duration * sampleRate / 1000;
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
```

This gives you a **440 Hz beep** — your “hello world” tone.

---

## 🧩 Step 2 — Add the Foreground Notification
Foreground services require a persistent notification.

Add this inside `onStartCommand`:

```java
Notification notification = new NotificationCompat.Builder(this, "tone_channel")
        .setContentTitle("Tone Clock Running")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build();

startForeground(1, notification);
```

And register the channel in your main activity:

```java
NotificationChannel channel = new NotificationChannel(
        "tone_channel",
        "Tone Clock",
        NotificationManager.IMPORTANCE_LOW
);

NotificationManager manager = getSystemService(NotificationManager.class);
manager.createNotificationChannel(channel);
```

---

## 🧱 Step 3 — Start the Service from Your Activity
In your main activity:

```java
Intent intent = new Intent(this, ToneService.class);
startForegroundService(intent);
```

Run the app → press your button → close the app → tone still plays.

That’s the milestone.

---

## 📡 Step 4 — Add AlarmManager (the periodic trigger)
Once the service works, add:

```java
AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
PendingIntent pi = PendingIntent.getService(
        this, 0,
        new Intent(this, ToneService.class),
        PendingIntent.FLAG_IMMUTABLE
);

alarm.setRepeating(
        AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis(),
        5 * 60 * 1000,
        pi
);
```

Now your device beeps every 5 minutes.

---

## 🎧 Step 5 — Replace the sine wave with your timbre/pitch encoding
This is where your idea becomes *your* app.

You can use:

- **AudioTrack** for synthesized tones
- **SoundPool** for instrument samples

Either works on the ZTE Z981.

---

## 📱 Visual intuition: Foreground service + AlarmManager




---

## 🧠 Non‑obvious insight
The ZTE Z981 runs Android 6.0 → **no modern background‑execution restrictions**.  
This means:

- foreground services run forever
- AlarmManager repeating alarms fire reliably
- no Doze‑mode throttling
- no WorkManager complexity

Your device is *perfect* for this project.

---

# Foreground service types (used in manifest.xml)

| Type | Use case |
| --- | --- |
| ``mediaPlayback`` | audio, tones, sound cues |
| ``location`` | GPS tracking |
| ``dataSync`` | background sync |
| ``connectedDevice`` | Bluetooth, USB |
| ``phoneCall`` | VoIP |
| ``camera`` | camera preview |
| ``microphone`` | recording |