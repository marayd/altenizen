# Minimal docs

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

---

### 🚫 Do Not Use GitHub for Installation

Cloning or downloading this project directly from **GitHub** is **strictly prohibited**, even for non-commercial use.

If you wish to use this project legitimately **(ONLY FOR NON-COMMERCIAL PURPOSES)**, you must obtain builds (YOU MAY DOWNLOAD ONLY ARTIFACTS) through the official **Jenkins pipeline**:

🔗 [https://jenkins.mryd.org/](https://jenkins.mryd.org/)

---

### 📄 Read the `README.md` and `LICENSE.md`

Before using any part of the software—especially builds obtained through Jenkins—you **must read and accept** the terms described in:

* `README.md` — contains usage instructions and general policies
* `LICENSE.md` — outlines detailed legal terms, restrictions, and distribution rules

Use of this project implies agreement with all listed terms. If you do not agree, **do not proceed with installation or execution**.

---
