# Research Sources

Research snapshot: **2026-08-31**.

Use official platform documentation for technical/policy decisions. Competitor listings are product research, not authoritative Android behavior documentation.

## Android / Google Play

- Android `AudioManager`: https://developer.android.com/reference/android/media/AudioManager
- Android `NotificationManager` / DND: https://developer.android.com/reference/android/app/NotificationManager
- Foreground service restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Compose architecture/UDF: https://developer.android.com/develop/ui/compose/architecture
- Compose testing: https://developer.android.com/develop/ui/compose/testing/testing-cheatsheet
- Adaptive apps guidance: https://developer.android.com/develop/adaptive-apps
- Material 3 Adaptive releases: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- DataStore: https://developer.android.com/jetpack/androidx/releases/datastore
- Baseline Profiles / performance: https://developer.android.com/develop/ui/compose/performance/baseline-profiles
- Google Play target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Google Play foreground service requirements: https://support.google.com/googleplay/android-developer/answer/13392821
- AccessibilityService policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Android Vitals overview: https://support.google.com/googleplay/android-developer/answer/9859174

## Build / supply chain

- Gradle dependency verification: https://docs.gradle.org/current/userguide/dependency_verification.html
- GitHub CodeQL: https://docs.github.com/en/code-security/code-scanning/introduction-to-code-scanning/about-code-scanning-with-codeql
- GitHub dependency review: https://docs.github.com/en/code-security/supply-chain-security/understanding-your-software-supply-chain/about-dependency-review

## Representative Play Store market research

Search/verify listings again before major market claims because install counts and app behavior change.

- Volume Lock: https://play.google.com/store/apps/details?id=volumelock.vlocker
- Native Volume: https://play.google.com/store/apps/details?id=com.iprototypes.volume
- Sound Profile: https://play.google.com/store/apps/details?id=Orion.Soft

Other category searches performed during product discovery covered custom volume panels, audio-profile apps, phone-diagnostic apps, and emerging priority-call utilities.

## Research rules

- Date every benchmark refresh.
- Do not copy competitor code/assets/branding.
- Distinguish Play listing claims from independently verified behavior.
- Prefer Android/Google official docs for API and policy decisions.
- If platform behavior changes, update affected ADRs/product claims, not only this source list.
