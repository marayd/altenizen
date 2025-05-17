---

## 🎙️ Events

### `on player ends speaking`

**Context:**

* `<context.player>` or `linked-player` — returns the player who was speaking
* `<context.phrase>` — returns parsed words (using Vosk)
* `<context.activation>` — returns the activation name

---

## 🛠️ Commands

### `plasmoactivation`

**Syntax:**

```plaintext
plasmoactivation create name:<name> translation:<translation> texture:<texture> permission:<permission> weight:<weight>
```

* Creates or deletes an activation in **PlasmoVoice**

---

### `delete_audio`

**Definition:**

```java
setName("delete_audio");
setSyntax("delete_audio name:<[name]>");
```

* Deletes an audio file from the default path

---

### `download_plasmo_sound`

**Definition:**

```java
setName("download_plasmo_sound");
setSyntax("download_plasmo_sound [url:<[url]>] [name:<[name]>]");
```

* Downloads and converts a sound file
* ⚠️ Only supports `.wav` format

---

### `plasmo`

**Definition:**

```java
setName("plasmo");
setSyntax("plasmo (send|stop|take|playonloc|playonentity) path:<path> location:<loc> distance:<distance> [id:<id>]");
```

* Creates a voice source (on entity, location, etc.)
* You can save it using `save:entry`
* To stop the sound:
  Use `<entry[entry].audio_id>` to get the audio ID

---

### ⚠️ Not Open Source. Read-Only Access Only.
This code is proprietary and owned by marayd. No use, reuse, modification, execution, or redistribution is permitted without explicit written permission.
Commercial and derivative use is strictly prohibited.
For requests, contact the Author directly.

