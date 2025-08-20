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
