package com.krishang.looper;

import static org.junit.Assert.*;
import org.junit.Test;

public class LoopMixerTest {
    @Test public void secondTrackJoinsAtFrame96000AndStaysAlignedFor32Wraps() {
        int bar = LoopMixer.BAR_FRAMES;
        short[] base = new short[bar];
        short[] second = new short[bar];
        base[0] = 100;
        second[0] = 200;
        second[bar - 1] = -300;
        var mixer = new LoopMixer(base);
        mixer.render(new short[bar / 2]);
        assertEquals(96_000, mixer.queue(second));

        short[] remainder = new short[bar / 2 + 1];
        mixer.render(remainder);
        assertEquals(0, remainder[remainder.length - 2]);
        assertEquals(300, remainder[remainder.length - 1]);

        short[] wrap = new short[bar];
        short[] expected = new short[bar];
        expected[bar - 2] = -300;
        expected[bar - 1] = 300;
        for (int i = 0; i < 32; i++) {
            mixer.render(wrap);
            assertArrayEquals("Wrap " + (i + 1), expected, wrap);
        }
    }

    @Test public void boundaryAlreadyAtCursorIsAvailableAndInputsAreCopied() {
        short[] base = {1, 2};
        short[] layer = {3, 4};
        var mixer = new LoopMixer(base);
        mixer.render(new short[2]);
        assertEquals(2, mixer.queue(layer));
        base[0] = layer[0] = 100;
        mixer.render(new short[0]);
        var result = new short[3];
        mixer.render(result);
        assertArrayEquals(new short[]{4, 6, 4}, result);
    }

    @Test public void mixerClipsAfterSummingAllLayers() {
        var mixer = new LoopMixer(new short[]{30_000, -30_000});
        mixer.queue(new short[]{20_000, -20_000});
        var result = new short[2];
        mixer.render(result);
        assertArrayEquals(new short[]{Short.MAX_VALUE, Short.MIN_VALUE}, result);
        mixer.queue(new short[]{-20_000, 20_000});
        mixer.render(result);
        assertArrayEquals(new short[]{30_000, -30_000}, result);
    }

    @Test public void rejectsEmptyAndMismatchedClips() {
        assertThrows(IllegalArgumentException.class, () -> new LoopMixer(new short[0]));
        var mixer = new LoopMixer(new short[]{1, 2});
        assertThrows(IllegalArgumentException.class, () -> mixer.queue(new short[]{3}));
        var result = new short[2];
        mixer.render(result);
        assertArrayEquals(new short[]{1, 2}, result);
    }
}
