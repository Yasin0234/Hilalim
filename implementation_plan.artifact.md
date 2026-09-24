# Hilalim Project Improvements Plan

This plan aims to implement location-based prayer time updates, a countdown progress bar, and fix the Qibla finder bug.

## User Review Required

> [!IMPORTANT]
> The location selection will be added to the tools page. When a new location is selected, the app will immediately update the prayer times on the home screen.

## Proposed Changes

### 1. Prayer Times & Progress Bar (HomeFragment)
- Implement progress bar update logic in `HomeFragment.java`.
- Calculate the percentage of time elapsed between the previous and next prayer.
- Ensure the countdown and progress bar update every second.

### 2. Location Selection (AraclarFragment & WelcomeActivity)
- Extract location selection logic or create a shared mechanism to update location from `AraclarFragment`.
- Add a click listener to `btn_location` in `AraclarFragment.java`.
- When location changes, notify other fragments or ensure they refresh data.

### 3. Qibla Finder Fix (KibleFragment)
- Correct the rotation logic in `KibleFragment.java` to ensure the compass points correctly towards Mecca.
- Improve sensor handling to be more stable.
- Handle device rotation (portrait/landscape) if necessary.

## Proposed Changes Detail

### [Component] Prayer Times (Home)
#### [MODIFY] [HomeFragment.java](file:///D:/Hilalim/app/src/main/java/com/korkutsoftware/hilalim/HomeFragment.java)
- Update `updateCountdown()` to calculate and set `prayer_progress`.
- Handle the case where the current time is after Yatsi (next is Imsak next day).

### [Component] Tools & Location
#### [MODIFY] [AraclarFragment.java](file:///D:/Hilalim/app/src/main/java/com/korkutsoftware/hilalim/AraclarFragment.java)
- Implement `btn_location` click listener.
- Show a dialog or navigate to a location picker.
- Update `SharedPreferences` and trigger a refresh.

### [Component] Qibla Finder
#### [MODIFY] [KibleFragment.java](file:///D:/Hilalim/app/src/main/java/com/korkutsoftware/hilalim/KibleFragment.java)
- Fix the `RotateAnimation` logic.
- Ensure `qiblaDegree` is calculated correctly and used appropriately relative to `azimuth`.

## Verification Plan

### Automated Tests
- N/A (Mostly UI and sensor-based)

### Manual Verification
- Change location from Tools and verify Home fragment updates times and countdown.
- Observe the progress bar filling up as time passes.
- Verify Qibla needle points correctly (relative to a known compass if possible).
