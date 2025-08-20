
Secure Notes App (Kotlin / Jetpack Compose)

A privacy-focused Android note-taking application built with Kotlin and Jetpack Compose, designed to protect sensitive text and images using strong AES-256 GCM encryption.
Users can create notes, attach images, and choose whether to store them in plain or encrypted form.
Decryption requires a 6-digit PIN, with optional biometric authentication support.

✨ Features

📝 Rich Notes: Create, edit, and delete text-based notes with titles and content.

🔒 Encryption:

AES-256 GCM with PBKDF2-HMAC-SHA256 key derivation.

Text and images encrypted using a PIN-derived key and unique salt/IV.

Encrypted data is stored in internal storage with .enc extensions.

🖼️ Image Support:

Pick from gallery or capture with the camera.

Images are encrypted/decrypted alongside note content.

👆 PIN Protection:

6-digit numeric PIN required to encrypt or decrypt.

UI prevents weak inputs (forces digits only).

🧬 Biometric Authentication:

Optionally prompt for fingerprint/face unlock before PIN entry.

🧹 Internal Storage Only:

Notes and encrypted images never leave the app’s sandbox.

No external storage or cloud leaks.

📱 Modern Android UI:

Built fully in Jetpack Compose + Material 3.

Responsive layouts and intuitive flows.

🔧 Technical Overview
Core Components

Language: Kotlin

UI Framework: Jetpack Compose with Material 3

Encryption:

javax.crypto AES/GCM/NoPadding

PBKDF2WithHmacSHA256 key derivation

65,536 iterations

256-bit keys

Data Storage:

Notes stored in an in-app SQLite database (via Room / ViewModel).

Images written to internal app storage (/data/data/<package>/files/).

Encrypted images are saved as IMG_<UUID>.enc.

Navigation: Jetpack Navigation for Compose.

Coroutines: Asynchronous I/O operations via Dispatchers.IO.

App Flow

Create/Edit Note

User enters title + content.

Optionally picks/attaches an image.

Encrypt & Save

App prompts for a 6-digit PIN.

PIN + salt → PBKDF2 → 256-bit AES key.

Text + image encrypted with unique IV and stored internally.

Decrypt Note

User re-enters PIN (or authenticates biometrically).

Encrypted bytes → Decrypt with derived key.

Restored plain text + decoded bitmap preview shown.

Delete Note

Secure deletion of note + associated file.

📂 Project Structure

app/
 ├── ui/               # Jetpack Compose screens (NoteEditScreen, etc.)
 ├── viewmodel/        # NoteViewModel for state handling
 ├── data/             # Note model, DAO, Room database (if used)
 ├── crypto/           # EncryptionManager (AES-256 GCM)
 ├── utils/            # BiometricManager, helpers
 ├── MainActivity.kt   # Entry point
 └── ...

🖥️ Screenshots (conceptual)

    Unencrypted Note
    Title + body + image preview shown.

    Encrypted Note
    Title locked, body hidden, image replaced with 🔒 icon.

    PIN Dialog
    Material 3 AlertDialog with secure numeric input.

🚀 Getting Started
Prerequisites

    Android Studio Ladybug | 2024.2.1 or later

    Android SDK 35

    Kotlin 2.x

    JDK 17

Build & Run

    Clone the repository:

    git clone https://github.com/<your-username>/secure-notes-app.git
    cd secure-notes-app

    Open in Android Studio.

    Sync Gradle to install dependencies.

    Run on an emulator or physical device (min SDK 24).

🔑 Security Notes

    Never store PINs in plaintext. This app only derives ephemeral keys from user PINs using salt + PBKDF2.

    Salts are random per-note, stored alongside encrypted payloads.

    IVs are unique per encryption operation (critical for AES/GCM).

    Biometric authentication wraps PIN entry but does not replace cryptographic validation.

    This is a demo/educational app — not audited for production-grade security.

📌 Roadmap

Add search & tag support.

Cloud backup (client-side encrypted).

Multi-device sync with zero-knowledge storage.

PIN change + re-encryption for existing notes.

    Theming (dark mode, custom accents).

🐞 Known Issues

    If a user forgets their PIN, data cannot be recovered (by design).

    Large image files may slow down encryption/decryption.

    No export/import yet (planned in roadmap).

🤝 Contributing

Pull requests are welcome!
Please fork the repo and submit PRs against the main branch.
When contributing:

    Write clear commit messages.

    Test on emulator + at least one physical device.

    Add/update comments in code.

📜 License

This project is licensed under the MIT License — see LICENSE

for details.
🙏 Acknowledgements

    Android Jetpack libraries (Compose, Navigation, Room).

    Kotlin coroutines.

    Javax Crypto libraries.

    Inspiration from common password managers & privacy-focused note apps.

⚡ TL;DR: Secure Notes is a Compose-based Android app for private note-taking with AES-256 encryption of both text and images, locked behind a PIN + optional biometrics.
