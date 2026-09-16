# Talking Clock

* based on https://keepachangelog.com/en/1.0.0/, https://semver.org/, https://www.conventionalcommits.org/en/v1.0.0/
* sections: 
  * **Breaking** **Added** **Changed** **Deprecated** **Fixed** **Removed** **Security** **ToDo** (in that order)
* commit messages: `<type>[(<scope>)]: <description>`
    * prefixes: fix, feature, build, test, chore, perf, docs, style, refactor, revert, ci, logs

## 0.9.0 - 2026-09-16
basically functional Android-6 app tested on a ZTE Z981; some clock drift and a few missing features but it "works".
### Added
* Android-Studio-generated project files (gradle build and resources)
* `MainActivity` - the root of all windows in this app
* `ToneService` - a foreground service that generates sounds
* `ToneReceiver` - a relay for broadcast messages(`Intent`) to route them into the `ToneService`
* `settings.xml` - a collection of key-value pairs to configure the app
* `Beeps` - a dumping ground for some code i didn't want to lose; will probably rename soon
* `Settings` - a classic wrapper for the Android preferences system
* `SettingsActivity` - a window for tweaking settings; considering removing soon.
### ToDo
* move settings to MainActivity - there's so little there we might as well display settings too
* reload settings on-change without restart
  * this means changing the scheduling of the `AlarmManager` in `MainActivity`
  * also `ToneService.DATE_TIME_FORMAT` must be non-final ref
  * it also means we won't be using the built-in settings GUI (which is probably an improvement)
* see if newer Android systems have better scheduling (current clock drift is quite significant and can't be all GC)
* see if there's a way to get a GC log from a JVM running on Android
* create a wrapper for the `Log` system so we can display log events in `MainActivity` to make debugging easier