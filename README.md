# USIU Student Registration App (APT 3060)

Android app (Java, XML layouts) that lets first-year students register from their phones, **including offline**, using **Cloud Firestore**, and pay fees through **M-Pesa STK Push**.

## What is in the app

| Screen | What it does |
|---|---|
| 1. Registration | Full names, ID No, Reg No, Gender (radio buttons), Course + Department (drop-down spinners) |
| 2. Library | Pick a book (auto-fills ISBN, author, title) or type your own, choose borrow/return dates, enter ID No |
| 3. Hostel | Location, room type, sharable / not sharable, ID No |
| 4. Payment | Enter phone + amount, M-Pesa PIN prompt pops up, payment is confirmed, **registration is confirmed only after payment succeeds** |
| Summary and print | Print or save-as-PDF a summary for one student, or for all registered students (menu at top-right, or button on screen 1) |

A coloured banner at the top of every screen shows the connection state:
green = online and synced, orange = uploading, red = offline (with the number of items waiting to upload).

## 1. Prerequisites

- Android Studio (Koala or newer) with JDK 17 (bundled)
- A Firebase account and a Daraja (Safaricom) developer account, both free

## 2. Open the project correctly

Open the folder that **directly contains `settings.gradle`** (File > Open). If you unzipped and see a folder inside a folder, open the inner one. Wait for the Gradle sync to finish (bottom status bar) before doing anything else.

You do **not** need the Firebase Assistant in Android Studio (Tools > Firebase). The project is already wired up; you only need to add `google-services.json` (next section). If the assistant says "Could not find the Android Application module", it simply means Gradle has not synced successfully yet; ignore it and use the manual steps below.

## 3. Firebase setup (5 minutes, in the browser)

1. Go to <https://console.firebase.google.com> and create a project.
2. **Add an Android app** to it with package name `ke.ac.usiu.registration`.
3. Open **Build > Firestore Database > Create database**. Pick a location (it cannot be changed later) and **start in test mode**.
4. On the **Rules** tab, paste the contents of `firestore.rules` and click Publish. These rules are open, for coursework only (see Limitations).
5. Go to Project settings > Your apps, download **`google-services.json`** and put it in the `app/` folder (next to `app/build.gradle`).
6. Sync Gradle again and run.

## 4. M-Pesa (Daraja) setup

1. Create an account at <https://developer.safaricom.co.ke>, create an app, and enable **Lipa Na M-Pesa Sandbox**.
2. Copy the lines from `local.properties.example` into your `local.properties` (same folder as `settings.gradle`) and fill in your **consumer key** and **consumer secret**.
3. The sandbox shortcode `174379` and the sandbox passkey in the example file are the public test values from the Daraja documentation.

> **Important for your demo:** the Daraja *sandbox* does not send a real PIN prompt to a real phone. You test it with the simulator on the Daraja portal. To make a real prompt pop up on a real phone you need **production** credentials (a Paybill or Till with Go Live approval). Ask your lecturer whether a production shortcode is provided, then set `MPESA_ENV=production`, plus the real shortcode, passkey, key and secret.

## 5. Demo script for the marking scheme

**Database - data pushed when online, NOT pushed when offline (5 marks)**

1. Open the Firebase console > Firestore Database > **Data** tab in a browser and keep it visible.
2. Turn on **airplane mode**. The banner turns red: "Offline".
3. Register a student (screen 1), add a book (screen 2), pick a hostel (screen 3). Each save shows "saved on your phone, will upload when you are back online" and the banner counts the waiting items. **Nothing appears in the console.**
4. Turn airplane mode off. The banner turns orange then green, "uploaded to the server" toasts appear, and the `students`, `library` and `hostels` collections now appear in the console.

**M-Pesa (5 marks):** screen 4, online, enter phone number and amount (e.g. 1), tap Pay, enter the PIN, and watch the status change to "CONFIRMED". Check a document appears in `confirmations`.

**Printing:** menu > Summary and print > *Show and print my summary* (one student) and *Print all registered students*. Choose "Save as PDF" if no printer is available.

## 6. How it works (for your report)

- **Offline support:** Cloud Firestore keeps a local cache by default on Android. Writes are stored on the phone and queued; Firestore uploads them automatically when the connection returns. `FirebaseHelper` completes a write only when the *server* acknowledges it, which drives the banner and the "uploaded" toasts. `SyncStatus` watches the phone's connectivity.
- **Registration confirmed only after payment:** personal data goes to `students/{idNo}`. The document `confirmations/{idNo}` is written **only** when M-Pesa reports success, and every report shows "NOT CONFIRMED - payment pending" until it exists. Payment needs internet, so it is blocked with a clear message when offline.
- **M-Pesa flow:** OAuth token -> STK Push (PIN prompt) -> poll the STK Push Query API every 6 seconds until success/failure. No callback server is needed.
- **Collections:**
  ```
  students/{idNo}                personal details
  library/{autoId}               book requests (field studentIdNo)
  hostels/{idNo}                 hostel choice
  payments/{checkoutRequestId}   every M-Pesa attempt (PENDING/SUCCESS/FAILED, field studentIdNo)
  confirmations/{idNo}           exists only after successful payment
  ```
- **Reports offline:** when offline, reports are read from Firestore's local cache, so a student can still view/print what they entered on that phone.
- **Printing:** HTML is rendered in a WebView and sent to Android's `PrintManager`.

## 7. Limitations (mention these honestly in your presentation)

- **Security:** the Firestore rules are open (test mode also expires after 30 days) and the M-Pesa keys are inside the APK. That is fine for a coursework demo, but a real system needs Firebase Authentication, restrictive rules, and a backend server that holds the Daraja secrets.
- **"Print all students"** reads every student record, so any phone running this build can do it. In production this would be an admin-only feature.
- The "waiting to upload" counter resets if the app is killed while offline; the data itself is **not** lost, Firestore still uploads it.
- Course, department, hostel and book lists are samples; edit `res/values/arrays.xml` and the `BOOKS` array in `LibraryActivity.java`.
- This project was generated without an Android SDK, so it has been cross-checked (resource ids, strings, styles, manifest) but **not compiled or run on a device**.
