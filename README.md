# TaskFlow  Android To-Do App with Login

TaskFlow is a modern native Android task management application built with Java and XML. It allows users to create personal accounts, securely log in, organize their tasks, track their progress, and manage completed activities through a clean and intuitive interface.

The application uses SQLite for local data storage and works entirely offline, without requiring a remote backend or an internet connection.

## Features

### Authentication and Security
- User registration and login.
- Input validation for account creation.
- Password hashing using PBKDF2-HMAC-SHA256.
- Local session management.
- Automatic session restoration.
- Secure logout flow.
- User-specific task access and data isolation.

### Task Management
- Create new tasks.
- Edit existing tasks.
- Delete tasks.
- Mark tasks as completed or active.
- Filter tasks by All, Active, and Completed.
- Track task completion progress.
- View task statistics and counters.
- Persist tasks locally between application launches.
- Display appropriate empty states when no tasks are available.

### User Interface
- Modern Material Design 3-inspired interface.
- Dark visual theme with refined accent colors.
- Responsive layouts for different Android screen sizes.
- Clear task status indicators.
- Accessible controls and consistent visual styling.

### Offline Functionality
- Local SQLite database.
- No remote server required.
- No internet connection required for core functionality.
- Persistent user and task data stored on the device.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Application logic |
| XML | User interface layouts |
| Android SDK | Native Android development |
| SQLite | Local database |
| PBKDF2-HMAC-SHA256 | Password hashing |
| SharedPreferences | Local session information, where implemented |
| Material Design 3 | User interface styling |
| Gradle | Project build and dependency management |
| JUnit / Robolectric | Automated testing, where configured |

## Application Architecture

TaskFlow separates authentication, database operations, session management, and task management into dedicated components.

The application uses a local SQLite database containing user and task records. Each task is associated with its owner through a user identifier, allowing task operations to be scoped to the authenticated account.

Passwords are stored as derived password hashes rather than plaintext passwords. Session information is managed locally, and logging out returns the user to the authentication flow.

## Database Structure

### Users

The user database stores account information required for authentication, including a user identifier, username, normalized email address, password hash, and associated password-hashing metadata.

### Tasks

The task database stores task information, including a task identifier, owner identifier, title, description where supported, completion status, and timestamps. Optional fields such as priority and due date depend on the implemented features.

Foreign keys and database indexes support relational integrity and efficient access to user-specific tasks.

## Getting Started

### Requirements

- Android Studio.
- A compatible Android SDK.
- Java and Gradle versions compatible with the project configuration.
- An Android emulator or physical Android device.

### Installation

1. Clone the repository:

   ```bash
   git clone <YOUR_REPOSITORY_URL>
   ```

2. Open Android Studio.

3. Select **Open** and choose the TaskFlow project directory.

4. Allow Gradle to synchronize the project and download any required dependencies.

5. Connect an Android device or start an emulator.

6. Build and run the application using Android Studio.

Replace `<YOUR_REPOSITORY_URL>` with the actual repository URL before publishing this README.

## How to Use

1. Launch TaskFlow.
2. Create an account using the registration screen.
3. Log in with your credentials.
4. Add tasks using the Add Task control.
5. Edit, complete, reopen, or delete tasks as needed.
6. Use the task filters to view all tasks, active tasks, or completed tasks.
7. Monitor your task statistics and completion progress.
8. Log out when you finish your session.

## Testing

The project generation report indicates that 15 automated tests passed successfully.

Reported verification includes:
- Successful applet compilation.
- Successful configured unit and Robolectric test execution.
- Local SQLite storage and database relationships.
- Password hashing and authentication.
- User-specific task operations.
- Task filters and progress indicators.
- Session restoration and logout behavior.

**Note:** These results reflect the reported generation checks. A complete Android Gradle build and manual testing on an emulator or physical device should also be performed before release.

## Privacy and Security

TaskFlow is designed to keep its core functionality local to the device.

- Passwords should never be stored in plaintext.
- Task database operations should enforce user ownership.
- Authentication should validate stored password hashes.
- Session data should not contain plaintext passwords.
- Account and task data remain in the application's local storage unless additional export or synchronization functionality is implemented.

Local storage does not provide cloud synchronization. Uninstalling the application or clearing its data may remove locally stored accounts and tasks.

## Project Structure

The Android project contains the application source code, XML layouts, resource files, manifest, Gradle configuration, database and authentication components, and tests.

The exact package names and file organization depend on the generated project structure.

## Future Improvements

Potential enhancements include:
- Task reminders and notifications.
- Search and advanced task sorting.
- Due-date calendar.
- Additional task priorities.
- Light and dark theme selection.
- Task export and backup.
- Optional cloud synchronization.

These are potential future improvements and are not necessarily included in the current version.

## Project Information

- **Project:** TaskFlow
- **Platform:** Android
- **Language:** Java
- **UI:** XML
- **Database:** SQLite
- **Connectivity:** Offline
- **Category:** Mobile Application Development

## License

This project is intended for educational and portfolio purposes. Add a `LICENSE` file if you wish to distribute it under a specific open-source license.
