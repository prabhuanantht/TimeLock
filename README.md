# TimeLock - Secured Access 🔒📱

**TimeLock** is a powerful Android application designed to help you regain control of your time. By enforcing temporary guest sessions, it restricts access to your device, allowing only selected "Allowed Apps" to be used for a set duration.

## 🚀 Key Features

*   **Kiosk Mode**: When a session starts, TimeLock becomes your home screen, showing *only* the apps you selected.
*   **Strict Lockout**: Once the timer expires, the device completely locks down until the PIN is entered.
*   **Smart Timers**:
    *   **Combined Mode**: Set a global timer for the entire session.
    *   **Individual Mode**: Set specific time limits for specific apps (Projected duration is based on the longest timer).
*   **Tamper Proof**: Uses Device Admin and Accessibility Services to prevent bypassing or uninstallation during an active session.
*   **Modern UI**: Features a sleek Dark/Light mode, intuitive Navigation Drawer, and a distraction-free interface.

## 🛠️ Tech Stack

*   **Language**: Kotlin
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Components**:
    *   `DevicePolicyManager` (Device Admin)
    *   `AccessibilityService` (App Monitoring)
    *   `ForegroundService` (Timer & State Management)
    *   `BroadCastReceiver` (Boot & Admin Events)
    *   `RecyclerView` & `ConstraintLayout` (UI)

## 📥 Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/prabhuanantht/TimeLock.git
    ```
2.  Open in **Android Studio**.
3.  Build and Run on your device.

## 🛡️ Permissions

TimeLock requires high-level permissions to function effectively:
*   **Device Admin**: To prevent uninstallation during a session.
*   **Accessibility Service**: To monitor foreground apps and block restricted ones.
*   **Display Over Other Apps**: To show the lock screen immediately when a restricted app is opened.

## 📸 Screenshots

*(will add soon)*

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

## 📄 License

[MIT](LICENSE)
