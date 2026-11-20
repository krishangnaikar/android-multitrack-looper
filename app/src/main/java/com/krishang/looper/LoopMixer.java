package com.krishang.looper;

import java.util.ArrayList;
import java.util.List;

final class LoopMixer {
    static final int SAMPLE_RATE = 48_000;
    static final int BAR_FRAMES = 96_000;
    private record Layer(short[] samples, long startFrame) {}

    private final int barFrames;
    private final List<Layer> layers = new ArrayList<>();
    private long nextFrame;

    LoopMixer(short[] base) {
        if (base.length == 0) throw new IllegalArgumentException("Empty loop");
        barFrames = base.length;
        layers.add(new Layer(base.clone(), 0));
    }

    synchronized long queue(short[] clip) {
        if (clip.length != barFrames) throw new IllegalArgumentException("Clip must span one bar");
        long wait = (barFrames - nextFrame % barFrames) % barFrames;
        long boundary = Math.addExact(nextFrame, wait);
        layers.add(new Layer(clip.clone(), boundary));
        return boundary;
    }

    synchronized void render(short[] output) {
        long end = Math.addExact(nextFrame, output.length);
        for (int i = 0; i < output.length; i++) {
            long frame = nextFrame + i;
            long sum = 0;
            int position = (int) (frame % barFrames);
            for (int layer = 0; layer < layers.size(); layer++) {
                Layer clip = layers.get(layer);
                if (frame >= clip.startFrame()) sum += clip.samples()[position];
            }
            output[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sum));
        }
        nextFrame = end;
    }
}
