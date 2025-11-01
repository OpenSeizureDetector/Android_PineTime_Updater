OpenSeizureDetector PineTime Watch Firmware Updater
===================================================

This app is used to update a PineTime watch to use the OpenSeizureDetector (OSD) 
firmware so the PineTime can be used as an epileptic seizure detector.

Usage
-----

The app displays a single page with three buttons:
  - Select Firmware - the list of firmware files available on the OpenSeizureDetector
           server is presented, and the user selects the one to use.
  - Select Device - the list of available Bluetooth Low Energy devices is presented
            and the user selects the one to use (it should be identified 
            as 'infinitime' or 'pinetime')
  - Stard DFU - starts the Device Firmware Update (DFU) - the watch will re-boot
            when this is completed.

When it is first run the app will ask for a number of permissions which are 
essential for scanning for bluetooth devices and connecting to them,
and sending a notification to allow the DFU to happen in the background.


Privacy
-------
The app does not collect any personal information - see the [Privacy Policy](./PRIVACY_POLICY.md)
 for more information.



App Structure
-------------

This app is based heavily on the Nordic Seminconductors [DFU Library Example App](https://github.com/NordicSemiconductor/Android-DFU-Library).

The main modifications are that rather than selecting a file from the phone storage, 
we download the list of possible  firmwares from the OSD server and download the 
one selected by the user.   We also pre-configure the settings to work
with a PineTime watch by default.

To compile this you need a recent version of Android Studio (2025.2 or later).

See the [Developer Guide](./DEVELOPER_GUIDE.md) for more details.


License and Credits
-------------------

This project is licensed under the GNU General Public License v3 (see [LICENSE.md](./LICENSE.md)).
The [LICENSE_nordic](./LICENSE_nordic) file describes the license for the Nordic Seminconductors
code and libraries that are used as part of this app.

All third-party libraries and attributions are listed in [CREDITS.md](./CREDITS.md).

Contact
-------

For more information please contact Graham Jones at [graham@openseizuredetector.org.uk]
