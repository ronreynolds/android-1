# Talking Clock

my first Android app which has mutated into an event-driven service that emits the current time via
the TextToSpeech service.

the point of this app is so that one (me) can tell the time in the morning without opening one's (my) eyes so that
the choice to get up or not uses minimal brain power (i.e., it's easier to go back to sleep if it's still too early).

interestingly it has also been a useful tool to keep my day somewhat on schedule without having to check the time.

currently (as of 0.9 on an old Android-6) there is a crazy amount of drift and the app also is put to sleep in a way
that it stops emitting sound a while after the phone has gone into standby.  possibly that's an OS setting...  nope,
nothing i could find.  it does seem to work longer if the phone is plugged in but it's definitely not a battery
problem (lots of life left in the battery).

## Classes
(current as of 0.9.1)
* `...clock.`
  * `MainActivity` - main (only) window of app which bootstraps almost everything else
  * `Settings` - wrapper around Android `SharedPreferences` which is used to persist our settings; supports Observers
  * `ToneReceiver` - routes broadcast `Intent` messages (emitted by `AlarmManager`) into the `ToneService` 
  * `ToneService` - uses TTS (Text-To-Speech) to say the current time when it receives an `Intent`
* `...util.`
  * `Logs` - a wrapper around Android's `Log` to make it observable and a little nicer to use
  * `StringSupplier` - API23 is only Java-7 with lambdas so `Supplier<T>` isn't available
  * `Tones` - formerly known as `Beeps` this is the original A-440 tone used for testing; kept to use later maybe