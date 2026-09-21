# Talking Clock

my first Android app which has mutated into an event-driven service that emits the current time via
the TextToSpeech service.

the point of this app is so that one (me) can tell the time in the morning without opening one's (my) eyes so that
the choice to get up or not uses minimal brain power (i.e., it's easier to go back to sleep if it's still too early).

interestingly it has also been a useful tool to keep my day somewhat on schedule without having to check the time.

## v0.9.0
currently (as of 0.9 on an old Android-6) there is a crazy amount of drift and the app also is put to sleep in a way
that it stops emitting sound a while after the phone has gone into standby.  possibly that's an OS setting...  nope,
nothing i could find.  it does seem to work longer if the phone is plugged in but it's definitely not a battery
problem (lots of life left in the battery).

## v1.0.0
this version fixed MOST of the clock drift and sleep-pause issues by switching from `AlarmManager`'s `setRepeating` 
(which takes a start delay and a period) to `setAndAllowWhileIdle` (which only takes the delay and then fires the event).
this required that `SpeechService` picks up the ball when the alarm fires `onStartCommand` to then immediately schedule
the next `Intent` using the same method and a new timestamp (now + period).  his has greatly reduced the drift from the
0 second of the target minute but on the old ZTE it can still drift by up to 35-40 seconds.  it does seem to be surviving
the device going to sleep which was the real goal.  yay!

## v1.0.1
evidence (e.g., an empty `Settings` observer list) indicated that the `SpeechService` was being GCed (since that list
was using `WeakRef` to allow observers to drop out should the observer list be their ONLY ref).  this led to learning
that services in Android are a Java-native peer-pair and that the Java peer can be GCed at any time it's no longer needed
and the native peer service will continue to run just fine.  this explains so much weirdness seen in previous versions.
the fix (blunt as it is) was to keep a strong ref to the `Settings` observer (the `SpeechService`) which holds a tiny
bit more Java-heap but also makes things work as intended.  so... yay! :)

## vNext
next steps are to port the code to my Galaxy S21+ 5G phone (Android 15, API 35, "Vanilla Ice Cream", Java 17-ish) and 
address the probably inevitable impact that'll have (and the learning experience that comes with it). :)
### Android-6(API-23) -> Android-15(API-35) changes (per Copilot)
#### Required
* foreground service declaration and notification behavior
  * requires a notification channel
  * a persistent notification
  * correct foreground service type (mediaPlayback, systemExcepted, etc)
  * without a type Android 14+ may throttle or reject the service :(
* `AlarmManager` exact-timing restrictions
  * Android 15 has strict batching rules; will need to use `setExactAndAllowWhileIdle`
* Runtime permission model changes
  * Android 15 requires runtime requests for:
    * POST_NOTIFICATIONS
    * FOREGROUND_SERVICE_MEDIA_PLAYBACK (or similar)
    * SCHEDULE_EXACT_ALARM (if needed)
* TTS engine changes
  * initialization is async (i thought it already was)
  * voice selection may differ (meh - i do this all thru system settings anyway)
  * some engines require explicit locale config
* Manifest updates
  * `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`
  * `<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>`
  * `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>`
  * and declare the foreground service type:
  * `<service android:name=".SpeechService" android:foregroundServiceType="mediaPlayback" />`
* Target/compile SDK updates
  * `compileSdk = 35`
  * `targetSdk = 35`
  * `sourceCompatibility = 17`
  * `targetCompatibility = 17` (Android Gradle Plugin 8.x requires Java-17)
  
#### Optional
* Switch from `AlarmManager` to `WorkManager` (if flexible timing is OK)
  * If your clock doesn’t need exact 15‑minute ticks, WorkManager gives:
    * Automatic Doze handling
    * Battery‑friendly scheduling
    * Easier constraints
* Use modern TTS voices
  * Android 15 supports:
    * Neural voices
    * Higher‑quality synthesis
    * Faster startup
    * Better locale handling
    * Your clock could sound dramatically better.
* Use foreground service “trampoline” APIs
  * Android 14+ allows safer launching of foreground services from background contexts. 
    * Reduces crashes and ANRs (App Not Responding)
* Improve timing accuracy with monotonic clock
  * Android 6’s timing drift was partly due to:
    * Wall‑clock time changes 
    * Doze batching 
    * AlarmManager jitter 
  * On Android 15, you can use:
    * SystemClock.elapsedRealtime()
    * Exact alarms 
    * Foreground service timers

## Classes
(as of 1.0.0)
* `...clock.`
  * `IntentRelay`(was `ToneReceiver`) - routes broadcast `Intent` messages (emitted by `AlarmManager`) into the `SpeechService` 
  * `MainActivity` - main (only) window of app which bootstraps almost everything else
  * `Settings` - wrapper around Android `SharedPreferences` which is used to persist our settings; supports Observers
  * `SpeechService`(was `ToneService`) - uses TTS (Text-To-Speech) to say the current time when it receives an `Intent`
* `...util.`
  * `LimitedTextView` - wrapper around a `TextView` and its `ScrollView` that limits the number of lines stored  
  * `Logs` - a wrapper around Android's `Log` to make it observable and a little nicer to use
  * `StringSupplier` - API23 is only Java-7 with lambdas so `Supplier<T>` isn't available
  * `Time` - basic util for working with time and durations
  * `Tones` - formerly known as `Beeps` this is the original A-440 tone used for testing; kept to use later maybe
  * `WeakArrayList` - an `ArrayList` version of a `WeakHashMap` to prevent memory leaks in observers