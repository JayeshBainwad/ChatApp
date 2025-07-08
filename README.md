# 💬 ChatApp

**A real-time, modern chat application built using Kotlin, Jetpack Compose, Firebase, and Ktor.**

---

## 🚀 About

**ChatApp** is a full-featured, real-time messaging app built using **Jetpack Compose**, **Firebase**, and a **Ktor-powered backend** for delivering custom notifications via **Firebase Cloud Messaging (FCM)**. The project uses **Clean Architecture** with the **MVI pattern**, ensuring modularity, testability, and scalability.



---

## 🧩 Features

| Category | Description |
|----------|-------------|
| 🔐 Authentication | Email/Password & Google Sign-In (One-Tap UI) |
| 💬 Real-time Messaging | Built using Firebase Firestore with live typing indicators & user presence |
| 📁 Media Sharing | Upload and download files/images via Firebase Storage |
| 🔔 Push Notifications | FCM + Ktor Server for real-time delivery, with inline reply support |
| 📰 News Integration | [In Progress] Fetch and share trending news via NewsAPI |
| 🔄 Offline Handling | Intelligent retry logic for push messages when the receiver is offline |

---

## 🧱 Architecture & Tech Stack

| Layer / Purpose | Technology / Library |
|----------------|---------------------|---------|
| 👨‍💻 Language | Kotlin | |
| 🧱 Architecture | MVI + Clean Architecture | |
| 🖼 UI Framework | Jetpack Compose | |
| ⚙️ Dependency Injection | Hilt | |
| ☁️ Realtime DB | Firebase Firestore | |
| 🔐 Authentication | Firebase Auth (Email + Google Sign-In) | |
| ☁️ File Storage | Firebase Storage | |
| 📬 Notifications | Firebase Cloud Messaging + Ktor Server (Render) | |
| 🌍 News API | [NewsAPI.org](https://newsapi.org/) (In Progress) | |
| 🧰 IDE | Android Studio Narwhal (2025.1.1) |
| 🚀 Backend Hosting | [Render](https://render.com/) | |

---

## 📦 Dependencies & Versions

| Library | Version |
|---------|---------|
| Kotlin | 1.9.x |
| Jetpack Compose | 1.5.x – 1.6.x |
| Navigation Compose | 2.7.x |
| Hilt | 2.48+ |
| ViewModel / Lifecycle | 2.6.x |
| Accompanist Permissions | 0.34.x |
| Firebase Auth | 22.2.x |
| Firebase Firestore | 26.5.x |
| Firebase Messaging (FCM) | 23.3.x |
| Firebase Storage | 21.3.x |
| Ktor (Android Client) | 2.3.5 |

> 🔍 *See `build.gradle.kts` for complete list and updates.*

---

## 🧪 Project Structure (Clean Code Architecture)

```
ChatApp/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/jsb/chatapp/
│   │       │   │
│   │       │   ├── feature_auth/
│   │       │   │   ├── data/
│   │       │   │   │   ├── auth_datasource/    # Authentication data sources
│   │       │   │   │   └── auth_repository/    # Auth repository implementations
│   │       │   │   ├── domain/
│   │       │   │   │   └── usecase/           # Authentication use cases
│   │       │   │   └── presentation/          # Auth UI screens & ViewModels
│   │       │   │
│   │       │   ├── feature_chat/
│   │       │   │   ├── data/
│   │       │   │   │   ├── chat_datasource/   # Chat data sources (Firebase)
│   │       │   │   │   ├── chat_repository/   # Chat repository implementations
│   │       │   │   │   └── fcm/              # Firebase Cloud Messaging
│   │       │   │   ├── domain/
│   │       │   │   │   ├── model/            # Chat domain models
│   │       │   │   │   └── usecase/          # Chat business logic
│   │       │   │   └── presentation/         # Chat UI screens & ViewModels
│   │       │   │
│   │       │   ├── feature_core/
│   │       │   │   ├── core_data/
│   │       │   │   │   ├── main_datasource/   # Core data sources
│   │       │   │   │   └── main_repository/   # Core repository implementations
│   │       │   │   ├── core_domain/
│   │       │   │   │   ├── main_model/        # Core domain models
│   │       │   │   │   └── main_usecase/      # Core use cases
│   │       │   │   ├── core_navigation/       # Navigation setup
│   │       │   │   ├── core_presentation/     # Common UI components
│   │       │   │   ├── di/                   # Hilt dependency injection
│   │       │   │   ├── main_util/            # Utility functions
│   │       │   │   └── theme/                # App theme & styling
│   │       │   │
│   │       │   └── feature_news/            # In Progress
│   │       │       ├── data/
│   │       │       │   └── networking/        # HTTP client & API calls
│   │       │       │       ├── HttpClientFactory.kt
│   │       │       │       ├── constructUrl.kt
│   │       │       │       ├── responseToResult.kt
│   │       │       │       └── safeCall.kt
│   │       │       ├── domain/
│   │       │       │   ├── Error.kt
│   │       │       │   ├── NetworkError.kt
│   │       │       │   └── Result.kt
│   │       │       └── presentation/
│   │       │           └── ui/
│   │       │               └── screens/       # News UI screens
│   │       │
│   │       ├── res/                          # Android resources
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

---

## 🧠 Learning Outcomes

This project showcases your ability to:

- Build real-time apps with **Firebase Firestore**
- Implement **Custom Push Notification Handling** with a **Ktor Backend**
- Design scalable apps using **MVI + Clean Architecture**
- Use **Hilt** for effective dependency injection
- Build UI with **Jetpack Compose**
- Handle authentication securely (Google Sign-In + Email/Password)
- Manage async push logic for **offline/online receivers**

---

## 🧭 Screenshots / Demo

> 📸 Coming soon: UI screenshots including Light/Dark themes, chat screen, and notification replies.

---

## 🧑‍💻 Getting Started

### Prerequisites

- Android Studio Narwhal 2025.1.1 or later
- Android SDK 24+
- Firebase Account
- Render Account (for backend hosting)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/JayeshBainwad/ChatApp.git
   cd ChatApp
   ```

2. **Open in Android Studio (Narwhal 2025.1.1)**
   
   Sync Gradle and run the app on an emulator or physical device.

3. **Firebase Setup**

   - Create a Firebase project
   - Enable Firestore, Firebase Auth, Storage, and FCM
   - Add `google-services.json` to the `app/` folder

4. **Backend Setup (Ktor FCM Server)**

   - Visit [Render](https://render.com/) and deploy the [Ktor Server Repo](#)
   - Configure your FCM keys and backend routes

---

## 📩 Contact

Looking to collaborate or hire?

| Type | Link |
|------|------|
| 💼 LinkedIn | [LinkedIn Profile](www.linkedin.com/in/jayesh-bainwad-a09b93250) |
| 📧 Email | [Email](jbainwad@gmail.com) |

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Firebase](https://firebase.google.com/) for backend services
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI
- [Ktor](https://ktor.io/) for server-side development
- [NewsAPI](https://newsapi.org/) for news integration

---

<div align="center">
  <strong>⭐ Star this repo if you found it helpful!</strong>
</div>
