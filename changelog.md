# Talking Clock

* based on https://keepachangelog.com/en/1.0.0/, https://semver.org/, https://www.conventionalcommits.org/en/v1.0.0/
* sections: 
  * **Breaking** **Added** **Changed** **Deprecated** **Fixed** **Removed** **Security** **ToDo** (in that order)
* commit messages: `<type>[(<scope>)]: <description>`
    * prefixes: fix, feature, build, test, chore, perf, docs, style, refactor, revert, ci, logs

## 1.1.2 - unreleased
### Added
### Changed
### Fixed
### Removed
### ToDo
* consider simple trace lib to timing specific methods to log
* change text prefix to real setting?  (currently hard-coded in `Settings`)

## 1.1.1 - 2026-10-01
### Added
* log-file cleanup to limit storage used (last N logs)
* logging of current volume, max-volume, and current volume dB (if available)
* signing of release APK
* release-variant (debug or release) to app-name + version string
* ability to set Quiet volume to current volume
### Changed
* upgraded AGP from 9.4.0 to 9.4.1
* simplified UI (main-activity)
### Removed
* many log statements at startup

## 1.1.0 - 2026-09-28
### Added
* `MainApplication` to move non-GUI app tasks OUT of `MainActivity`, which is just a view that can be restarted whenever
* log file
* added logging of the min-volume and max-volume dB
* app-name and version header to UI and pulled from `BuildConfig`
* add a button to upload the app log to Google drive (because we can't read the log directory on the device)
* log-name rotation so every run's logs are unique (only renamed on upload or app restart)
  * also embed the app startup time to keep names unique but bucketed to a particular app run
### Changed
* changed "quiet" volume from 1 (which is too quiet on my Samsung Phone) to 10% of max volume to see if that works generally
* `MainActivity` now creates `SpeechService` and lets it handle all scheduling (including the first intent)
### Fixed
* disabled rotation from causing `MainActivity` to be restarted
  * IDE warning indicates our solution is deprecated and will not work after Android-16 :-/
  * at which point we'll just have to let it rotate and recreate the `MainActivity` (and be smarter about startup-vs-restart)
* hold ref in `SpeechService` to last pending intent to ensure shutdown is clean and doesn't leave the service hanging
### Removed
* `MainActivity.sendFirstIntent()` and all code related to `MainActivity` creating the first intent to trigger `SpeechService`

## 1.0.2 - 2026-09-20
### Added
* logging of the app name and version on startup
* moved anything not related to creation of GUI elements out of `MainActivity.onCreate()` and into `finishSetup()` for faster startup 
  * `finishSetup()` is then invoked via a posted message that is processed after the first render
  * due to info message from `Choreographer`: Skipped 40 frames!  The application may be doing too much work on its main thread.
  * this also required adding `muteSettingsChanges` to prevent: settings -> GUI -> change-handler -> settings
* migrated build and app to Android-15(API-35) (see readme for changes)
  * confirmed app still works with Android-6 via emulator
### Changed
* source and target JVM upgraded to 17 (from 11)
* replacing variable types with `var` where reasonable
  * unfortunately can't replace `SimpleDateFormat` with `DateTimeFormatter` because we're still targeting API-23
  * API-26 is required to use certain Java-11 features :-/
### Fixed
* logging of an epoch-millis rather than the number of millis until next event in `MainActivity`
### Removed
* `SpeechService.dateFormat` to simplify code (we don't invoke it more than once/minute so caching ROI is ~0)
  * `SpeechSevice.updateTimeFormat()` and `Settings` observer also removed as they're no longer needed

## 1.0.1 - 2026-09-19
### Added
* `Settings.setObserver()` to support single-observer use-case
### Fixed
* `SpeechService` observer of `Settings` is lost due to `WeakRef`
  * REAL service is native peer that can outlive Java object
  * fix is to hold strong ref to single `Observer`; not as elegant but it works (which is an elegance in itself). :)
### Removed
* `Settings.addObserver()` since we no longer support (for now) multiple `Settings` observers (KISS)

## 1.0.0 - 2026-09-18
### Added
* `util.`
  * `LimitedTextView` - prevents a text-view from exceeding a maximum line count 
  * `Logs` - wrapper around Android's Log to allow us to maintain our own log-view of our logs in the GUI
  * `StringSupplier` - since API-23 is only Java-7 compliant (with a few "unsugar" tricks to support Java-8 lambdas)
  * `Time` - collecting util methods for dealing with time and durations
  * `Tones` - renamed version of `Beeps`
  * `WeakArrayList` - used with observer pattern to prevent observers becoming immortal ONLY because they're in the list
    * my first 100% Copilot-written class (feels like cheating)
* reload settings on-change without restart
  * this means changing the scheduling of the `AlarmManager` in `MainActivity`
  * also `ToneService.DATE_TIME_FORMAT` must be non-final ref
  * it also means we won't be using the built-in settings GUI (which is probably an improvement)
* "Quiet" button to set volume to lowest value
### Changed
* change app icon to something more personal (Evil Calvin)
* move settings to `MainActivity` - there's so little there we might as well display settings too
* renamed `ToneService` to `SpeechService` and `ToneReceiver` to `IntentRelay`
### Fixed
* due to old Preferences UI integers were stored as strings and converted to ints as needed; they are now stored as ints
  * which means we probably can't go back to Preferences UI but so what?! :)
* address scheduling drift of `AlarmManager.setRepeating` by using `setAndAllowWhileIdle` and explicit re-emit with delay
* added limit to log view retention to avoid eating memory infinitely
### Removed
* unused class `SettingsActivity`
* action-bar from main-activity theme as it's no longer needed (yeah, more screen space!)
### ToDo
* nothing?  (ROFL)

## 0.9.0 - 2026-09-16
basically functional Android-6 app tested on a ZTE Z981; some clock drift and a few missing features but it "works".
### Added
* Android-Studio-generated project files (gradle build and resources)
* `MainActivity` - the root of all windows in this app
* `SpeechService`(was `ToneService`) - a foreground service that generates sounds
* `IntentRelay`(was `ToneReceiver`) - a relay for broadcast messages(`Intent`) to route them into the `SpeechService`
* `settings.xml` - a collection of key-value pairs to configure the app
* `Beeps` - a dumping ground for some code i didn't want to lose; will probably rename soon
* `Settings` - a classic wrapper for the Android preferences system
* `SettingsActivity` - a window for tweaking settings; considering removing soon.
