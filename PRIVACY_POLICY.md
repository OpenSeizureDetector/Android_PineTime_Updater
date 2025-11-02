# Privacy Policy for OSD PineTime Firmware Updater

**Last Updated:** 2024-10-31

This privacy policy governs your use of the software application "OSD PineTime Firmware Updater" (the "Application") for mobile devices.

### Information We Do Not Collect

The Application is designed with user privacy as a primary concern. We want to be completely transparent about what information the Application does **not** collect:

-   The Application does **not** collect, store, or transmit any Personally Identifiable Information (PII) such as your name, email address, or phone number.
-   The Application does **not** use any analytics frameworks to track your behavior or usage patterns.
-   The Application does **not** access your contacts, calendars, photos, or any other personal files on your device.

### Information the Application Accesses and Why

To perform its core function of updating a PineTime watch, the Application requires access to certain services and permissions on your device. All access is for functional purposes only.

1.  **Network Access**
    -   The Application connects to `https://osdapi.org.uk/` to download a list of available firmwares (`index.json`) and the firmware file (`.zip`) you select. 
    -   No personal or device information is sent with these download requests. The requests are purely to fetch the files required for the update.

2.  **Bluetooth Permissions (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`)
    -   **Purpose**: These permissions are essential for the core functionality of the app, which is to find, connect to, and transfer a firmware update to your PineTime watch via Bluetooth Low Energy (BLE).

3.  **Location Permission (`ACCESS_FINE_LOCATION`)
    -   **Purpose**: On many versions of the Android operating system, this permission is a technical requirement for an app to be able to scan for nearby Bluetooth LE devices. The Application does **not** use this permission to determine, track, or store your geographical location.

4.  **Notification Permission (`POST_NOTIFICATIONS`)
    -   **Purpose**: This permission is required to display a persistent notification in your device's status bar while a firmware update is in progress. This is a requirement by Android for background services to ensure the user is aware of ongoing operations.

5.  **Foreground Service Permission (`FOREGROUND_SERVICE`)
    -   **Purpose**: A firmware update can take several minutes. This permission allows the update process to run reliably in a background "foreground service" without being terminated by the operating system if you switch to another app.

6.  **Internal Storage**
    -   The Application temporarily saves downloaded firmware files to its private, sandboxed cache directory. This data is not personal, is not accessed by other apps, and is cleared by the operating system as needed.

### Changes to This Privacy Policy

This privacy policy may be updated from time to time. We will notify you of any changes by posting the new privacy policy within the Application's Play Store listing and on this web page. You are advised to consult this privacy policy regularly for any changes.

### Contact Us

If you have any questions regarding privacy while using the Application, or have questions about our practices, please contact us via email at graham@openseizuredetector.org.uk.
