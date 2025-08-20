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
