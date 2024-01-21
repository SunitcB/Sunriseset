# Sunrise Sunset Localization App

This Android app fetches and displays sunrise and sunset times based on location. It allows dynamic localization in English and Chinese.

## Overview

The app uses the [Sunrise Sunset API](https://sunrise-sunset.org/api) to retrieve sunrise and sunset times based on geographical coordinates. The main feature is the ability to dynamically switch between current location and Chinese coordinates for localization.

## Features

- Fetches and displays sunrise and sunset times using the Sunrise Sunset API.
- Dynamic localization:
  - When the switch is **off**, the app fetches the current device location to generate sunrise and sunset times.
  - When the switch is **on**, the app uses predefined Chinese latitude and longitude for localization, providing sunrise and sunset times for China.

## How to Use

1. Launch the app on your Android device.
2. The main screen contains two TextViews to display sunrise and sunset times.
3. Use the switch (located at the top) to toggle between localization modes.
   - When the switch is **off**, the app fetches sunrise and sunset times based on the current device location.
   - When the switch is **on**, the app fetches sunrise and sunset times for China using predefined Chinese latitude and longitude.

## Code Details

The main logic is implemented in `PlanetINfoActivity.java`:
- The `languageSwitch` is a Switch widget that triggers the localization mode.
- The `fetchSunriseSunSetTimes` is responsible for calling `fetchTime` sunrise and sunset times from the API asynchronously using Coroutines.
- The `updateLocale` method handles the switch trigger and sets up the locale of the application and gets the required lattitude and longitude values and also switches the label text for the switch as well.

## Screenshots

![English Local](https://github.com/SunitcB/Sunriseset/blob/main/Screenshots/English.png?raw=true)
![Chinese Locale](https://github.com/SunitcB/Sunriseset/blob/main/Screenshots/Chinese.png?raw=true)

## Additional Notes

- The application requires the permission to access current location of the device. When the application starts, it prompts to allow access to get the location of the device.
- The `checkLocationPermission` is used to check whether the application has the access to the fetch the current location from the application to get the lattitude and longitude for fetching the sunrise and sunset times.
- The `requestLocationPermission` method is used to ask for the location access to the user.

