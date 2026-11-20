# Multitrack audio looper

Layer audio clips on a shared sample clock so a new track joins the existing loop on a bar boundary.

## First acceptance test

At 48 kHz and 120 BPM in 4/4, one bar spans 96,000 frames. Queue a second one-bar track halfway through the first. Its first frame should land at frame 96,000 and remain aligned over 32 wraps.

Tap **Play beat**, then **Queue bass layer**. Both tracks use original synthesized
PCM. The mixer schedules the bass at the next unrendered bar boundary and shows
the exact frame number. A single foreground AudioTrack streams the resulting mix.
The displayed frame refers to the render timeline, which leads audible output by
the audio buffer latency. Stop resets the sample clock.

Use JDK 17 or 21 and Android SDK Platform 35 with Build Tools 34.0.0. Run
`gradlew.bat :app:testDebugUnitTest :app:assembleDebug` on Windows or
`./gradlew :app:testDebugUnitTest :app:assembleDebug` on macOS or Linux.
The debug APK supports API 31 and newer.
