# Wireless Presentation Remote

This project provides a complete solution to control presentations (like PowerPoint, Keynote, Google Slides) from your Android phone using WebSockets. 

It intercepts the **physical volume keys** on your Android device (even when the screen is off) to trigger "Next" and "Previous" slide actions on your PC.

## Structure

1. `server/`: A Node.js WebSocket server that runs on your PC and simulates key presses (`Left Arrow` and `Right Arrow`).
2. `android/`: An Android application (Kotlin) that connects to the server and sends commands via on-screen buttons or volume keys.

## Part 1: Running the PC Server

The server requires Node.js to be installed on your computer.

1. Open a terminal/command prompt.
2. Navigate to the `server/` directory:
   ```bash
   cd server
   ```
3. Install dependencies:
   ```bash
   npm install
   ```
   *(Note: This uses `@nut-tree/nut-js` which is a modern, cross-platform library that doesn't usually require complex C++ build tools like `robotjs` does).*
4. Start the server:
   ```bash
   node server.js
   ```
5. You should see a message like `WebSocket server listening on ws://0.0.0.0:8765`. 
6. Find your PC's local IP address on the WiFi network (e.g., `192.168.1.10`) by running `ipconfig` (Windows) or `ifconfig`/`ip a` (Mac/Linux).

## Part 2: Building and Using the Android App

### Option A: Download APK via GitHub Actions (Recommended)

1. Upload this entire repository to a private GitHub repository.
2. Go to the **Actions** tab in your GitHub repository.
3. Click on the **Build Android APK** workflow on the left.
4. Click **Run workflow** -> **Run workflow** (if it didn't trigger automatically).
5. Once the build succeeds (green checkmark), click on the workflow run.
6. Scroll down to **Artifacts** and download the `remote-controller-apk.zip` file.
7. Extract the ZIP to find `app-debug.apk`. Transfer this file to your Android phone and install it (you may need to allow "Install from unknown sources").

### Option B: Build Locally using Android Studio

1. Open Android Studio.
2. Select **Open an existing Project**.
3. Select the `android/` folder inside this repository.
4. Wait for Gradle to sync (Android Studio will automatically generate the `gradlew` wrapper files if missing).
5. Press the **Run** (Play) button to install it on your connected device.

## Usage Instructions

1. Ensure both your PC and your Android phone are on the **same WiFi network**.
2. Run the Node.js server on your PC (`node server.js`).
3. Open the Android app.
4. Enter your PC's Local IP Address (e.g., `192.168.1.10`) in the text field.
5. Tap **Connect**. The status should change to "Connected".
6. Use the on-screen **Next/Previous** buttons to test. You should see log outputs in your PC terminal, and it will simulate Arrow Key presses.
7. **Volume Keys**: Once connected, you can lock your phone screen or put it in your pocket. Pressing the physical **Volume Up** or **Volume Down** buttons will act as Next/Previous slide.

## Troubleshooting

- **Server fails to start:** Ensure port 8765 is not being used by another application.
- **App fails to connect:** 
  - Ensure Windows Firewall (or your OS firewall) is not blocking port 8765. You may need to add an inbound rule for Node.js.
  - Verify the IP address is correct.
- **Volume keys aren't working with the screen off:** Make sure you haven't dismissed the persistent notification. The foreground service requires the notification to stay alive in the background. Ensure Battery Optimization is disabled for the app if your device heavily kills background tasks.
