# OSD PineTime DFU - Developer Guide

This guide provides a high-level overview of the application's architecture and user flow.

## Key Technologies

- **UI**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit & OkHttp
- **JSON Parsing**: Kotlinx Serialization
- **DFU**: Nordic Semiconductor's Android DFU Library

## File Structure & Responsibilities

The app is structured by feature into several key packages:

### `ui` - The User Interface

- **`MainScreen.kt`**: This is the single screen for the entire application. It uses a `Scaffold` for its basic layout and is responsible for observing state from the ViewModels and reacting to user input. It contains the logic for showing/hiding the device scanner and the firmware selection dialog.
- **`DeviceScanner.kt`**: A composable screen that handles Bluetooth scanning. It requests the necessary permissions and displays a list of nearby BLE devices.

### `viewmodel` - State Management

- **`FirmwareViewModel.kt`**: Manages all logic related to firmware. It fetches the `index.json` from the server, handles the download of the selected firmware file, and exposes the UI state for the firmware list and download progress.
- **`DfuViewModel.kt`**: Manages the device firmware update (DFU) process. It initiates the `DfuService` and listens for progress, success, or error events, exposing them as UI state.

### `remote` & `model` - Networking and Data

- **`model/Firmware.kt`**: Contains the Kotlin `data class` definitions (`FirmwareIndex`, `FirmwareRelease`) that match the structure of the `index.json` file. These are annotated for use with Kotlinx Serialization.
- **`remote/FirmwareService.kt`**: A `Retrofit` interface that defines the HTTP endpoints for fetching the JSON index and downloading the firmware `.zip` file.

### `service` - Background Operations

- **`DfuService.kt`**: The background service that performs the actual DFU process. It extends `DfuBaseService` from the Nordic library. It is configured to run as a foreground service to prevent the OS from killing it during the update.

### `di` - Dependency Injection

- **`NetworkModule.kt`**: A Hilt module responsible for providing singleton instances of the networking components (Retrofit, OkHttp, Json) and the `FirmwareService` to the rest of the application.

---

## User Flow & Interaction Logic

The app's flow is managed by observing state changes in the ViewModels.

1.  **App Start**:
    - `MainActivity` sets up the edge-to-edge display and loads `MainScreen`.
    - `MainScreen` immediately requests the `POST_NOTIFICATIONS` permission on modern Android versions.

2.  **User Clicks "Select Firmware"**:
    - The `onClick` lambda calls `firmwareViewModel.fetchFirmwareIndex()` and sets a flag to show the `FirmwareDialog`.
    - The `FirmwareDialog` observes the `firmwareState`. It shows a loading spinner, then the list of available firmwares. The recommended firmware is highlighted in bold.
    - User taps a firmware version.
    - `onFirmwareSelected` callback is triggered, which calls `firmwareViewModel.downloadFirmware()`.
    - `MainScreen` observes the `downloadState` and displays a progress bar while the firmware is downloading.
    - Upon completion, the `downloadedUri` state in the `FirmwareViewModel` is updated, which recomposes `MainScreen` to show the selected file.

3.  **User Clicks "Select BLE Device"**:
    - The `onClick` lambda sets a flag to display the `DeviceScanner` composable.
    - `DeviceScanner` requests Bluetooth & Location permissions.
    - Once permissions are granted, it calls `deviceScannerViewModel.startScan()`.
    - It displays a list of discovered devices (Name and MAC Address).
    - User taps a device.
    - The `onDeviceSelected` callback is triggered, updating the `selectedDevice` state in `MainScreen`.

4.  **User Clicks "Start DFU"**:
    - This button is only enabled when both a firmware URI and a device are selected.
    - The `onClick` lambda calls `dfuViewModel.startDfu()`, passing the device address and file URI.
    - The `DfuViewModel` immediately sets its state to `Waiting`.
    - `MainScreen` observes this and displays "Waiting for device...".
    - The `DfuViewModel` then initiates the `DfuService`.
    - The `DfuService` runs in the background. The `DfuProgressListener` inside the `DfuViewModel` listens for events from the service and updates the `dfuState` (e.g., `InProgress`, `Success`, `Error`).
    - `MainScreen` observes the `dfuState` and updates the UI to show the progress bar and status messages.

---

## App Icon Concept

A circular icon with three main elements layered together:

1.  **The Base: A Stylized Watch**
    -   A simple, dark grey or black circular outline representing the watch face.
    -   Add two small lugs (the parts that hold the strap) on the top and bottom to make it instantly recognizable as a watch.

2.  **The Action: Update Arrows**
    -   Inside the watch face, draw two concentric circular arrows in a light grey. This is the universal symbol for "sync" or "update."

3.  **The Brand & Technology: A Blue "Star of Life" with Radio Waves**
    -   In the very center of the arrows, place the signature blue **"star of life"** icon.
    -   Have the top and bottom arms of the star extend slightly and subtly transform into small radio wave icons (`((•))`). This merges the OSD brand with the concept of a wireless (Bluetooth) connection.

### Why this works:

-   **Instantly Recognizable:** The watch shape is clear.
-   **Communicates Purpose:** The update arrows and radio waves clearly signify a firmware update.
-   **Maintains Brand Identity:** The blue star of life is the most prominent element, connecting it immediately to the OpenSeizureDetector family of apps.
