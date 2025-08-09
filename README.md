# Multitrack audio looper

Layer audio clips on a shared sample clock so a new track joins the existing loop on a bar boundary.

## First acceptance test

At 48 kHz and 120 BPM in 4/4, one bar spans 96,000 frames. Queue a second one-bar track halfway through the first. Its first frame should land at frame 96,000 and remain aligned over 32 wraps.