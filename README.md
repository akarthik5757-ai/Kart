# Anbu Tour & Travels — Android (Kotlin + Jetpack Compose + Firebase)

This starter project implements:
- Email/password sign-up and login with Firebase Authentication
- Customer booking form and indicative fare estimate
- Firestore booking storage and live booking list
- Admin view of all bookings and status updates
- Owner dashboard counters for total, pending, and confirmed bookings
- Booking search and status filters
- Customer cancellation of their own pending booking (secured by Firestore rules)
- Vehicle-specific sample rates, including Tempo Traveller; round-trip fare multiplier
- Validation for phone number and positive distance
- Sample tour packages and owner/customer profile screens
- Blue-and-yellow styling inspired by the supplied reference

## Requirements
- Android Studio (a current stable version)
- JDK 17
- An Android emulator or Android device
- A Google/Firebase account

## 1. Create Firebase project
1. Open https://console.firebase.google.com/ and create a project.
2. Add an Android app with package name exactly: `com.anbutravels.app`
3. Download `google-services.json`.
4. Put that file at `app/google-services.json` (same folder as `app/build.gradle.kts`).
5. In Firebase Console → Authentication → Sign-in method, enable **Email/Password**.
6. In Firestore Database, create a database. Start in production mode and publish the rules in `firestore.rules`.

Do not commit `google-services.json` to a public repository if your project policies prohibit it. Firebase client config is not a replacement for database security rules.

## 2. Open and run
1. Extract the ZIP.
2. In Android Studio, choose **Open** and select the `AnbuTravelsAndroid` folder.
3. Let Gradle sync. If prompted, install Android SDK Platform 35 and use JDK 17.
4. Add your `google-services.json` file to the `app/` folder.
5. Press Run on an emulator or a connected Android device.

## 3. Make an owner/admin account
For security, the app does not allow users to select the admin role during sign-up.

1. Sign up in the app with the owner's email address.
2. Firebase Console → Firestore Database → `users` collection → open the document whose ID is the owner's Firebase Authentication UID.
3. Change its `role` field from `customer` to `admin` (string value).
4. Sign out and sign in again. The owner will see all bookings and status controls.

Only trusted project owners should be allowed to edit this role. Never add a public admin-registration button.

## 4. Firestore collections

### users/{uid}
```json
{
  "name": "K. Anbu",
  "phone": "+91 76049 05989",
  "email": "owner@example.com",
  "role": "customer",
  "createdAt": 1760000000000
}
```

### bookings/{bookingId}
```json
{
  "userId": "Firebase Authentication UID",
  "customerName": "Customer name",
  "phone": "Mobile number",
  "pickup": "Perambalur",
  "drop": "Trichy",
  "dateTime": "20 Oct 2026, 12:00 PM",
  "tripType": "One Way",
  "vehicle": "Sedan (4 Seater)",
  "distanceKm": "95",
  "estimatedFare": 1340,
  "status": "Pending",
  "createdAt": 1760000000000
}
```

## 5. Important notes before launch
- Fare calculation is a **sample estimate**, not a connected route-distance service or a final quote. The demo uses ₹12/km for Sedan, ₹16/km for SUV and ₹22/km for Tempo Traveller, plus ₹200 base fare. Round trips double the distance component. Confirm rates with the business owner before publishing.
- Date/time is a text field in this starter version. Replace with a date/time picker for production.
- Live GPS tracking, driver assignment, push notifications, WhatsApp messaging, Google sign-in, and real payments are not implemented. They require additional permissions, SDKs, server-side logic, or payment-provider onboarding.
- For production, add phone verification, abuse protection, stronger field validation, App Check, monitoring, privacy policy, and tested Firestore rules.
- This starter stores booking data in Firestore; do not use permissive test rules in production.

## Troubleshooting
- **`google-services.json` missing**: add the Firebase config to `app/google-services.json`.
- **Login method disabled**: enable Email/Password in Firebase Authentication.
- **Booking permission denied**: publish the provided `firestore.rules`; confirm the user profile exists.
- **Owner sees customer UI**: check `users/{ownerUid}.role` is the exact string `admin`, then sign out and sign back in.
- **Gradle sync failure**: confirm JDK 17 and internet access; allow Android Studio to download dependencies.


## Build an APK with GitHub Actions (phone-friendly)
1. Upload the extracted project files to your GitHub repository, keeping the `.github/workflows/main.yml` file.
2. Open the repository's **Actions** tab and run **Build Anbu Travels Android App** (or push a commit to `main`).
3. After the workflow succeeds, open the run and download the `AnbuTravels-debug-apk` artifact.
4. For Firebase features to work in the installed app, add your own `app/google-services.json` from Firebase. The file is ignored by `.gitignore` to avoid accidentally publishing project configuration.

The workflow builds a debug APK. It does not create a signed Play Store release. A successful compile does not verify Firebase setup or Firestore rules.

## Correct repository structure and cloud build

Keep the project structure exactly as packaged: `settings.gradle.kts` and root `build.gradle.kts` at the repository root, and Android app files inside `app/`. Do not upload `MainActivity.kt` or `AndroidManifest.xml` by themselves at the repository root.

Two cloud build configurations are included:
- GitHub Actions: `.github/workflows/main.yml`
- Codemagic: `codemagic.yaml` (exact filename, no spaces)

The GitHub Actions workflow builds a debug APK and uploads it as the `AnbuTravels-debug-apk` artifact. Firebase features require your own `app/google-services.json`; the project is configured to build without it, but Firebase-backed features will not work until Firebase is configured.
