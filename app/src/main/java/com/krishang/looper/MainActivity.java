package com.krishang.looper;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Process;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final short[] beat = makeClip(false);
    private final short[] bass = makeClip(true);
    private Playback playback;
    private TextView status;
    private Button transport;
    private Button layer;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        var layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            var bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            view.setPadding(padding + bars.left, padding + bars.top,
                    padding + bars.right, padding + bars.bottom);
            return insets;
        });
        var title = new TextView(this);
        title.setText("Multitrack Looper");
        title.setTextSize(28);
        layout.addView(title);
        var timing = new TextView(this);
        timing.setText("120 BPM · 4/4 · 48 kHz");
        layout.addView(timing);
        status = new TextView(this);
        layout.addView(status);
        transport = new Button(this);
        transport.setOnClickListener(view -> {
            if (playback == null) start();
            else stop("Stopped");
        });
        layout.addView(transport);
        layer = new Button(this);
        layer.setText("Queue bass layer");
        layer.setOnClickListener(view -> {
            if (playback == null) return;
            long boundary = playback.mixer.queue(bass);
            status.setText("Bass starts at frame " + boundary + " (bar "
                    + (boundary / LoopMixer.BAR_FRAMES + 1) + ")");
            layer.setEnabled(false);
        });
        layout.addView(layer);
        setContentView(layout);
        stop("Ready");
    }

    private void start() {
        playback = new Playback(new LoopMixer(beat));
        transport.setText("Stop");
        layer.setEnabled(true);
        status.setText("Playing beat");
        new Thread(playback, "loop-audio").start();
    }

    private void stop(String message) {
        if (playback != null) playback.running = false;
        playback = null;
        transport.setText("Play beat");
        layer.setEnabled(false);
        status.setText(message);
    }

    @Override protected void onStop() {
        stop("Stopped");
        super.onStop();
    }

    private final class Playback implements Runnable {
        private final LoopMixer mixer;
        private volatile boolean running = true;

        Playback(LoopMixer mixer) { this.mixer = mixer; }

        @Override public void run() {
            AudioTrack track = null;
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                int minimum = AudioTrack.getMinBufferSize(LoopMixer.SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (minimum <= 0) throw new IllegalStateException("PCM output unavailable");
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(LoopMixer.SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(minimum)
                        .build();
                if (!running) return;
                track.play();
                short[] block = new short[480];
                while (running) {
                    mixer.render(block);
                    int offset = 0;
                    while (running && offset < block.length) {
                        int count = track.write(block, offset, block.length - offset,
                                AudioTrack.WRITE_BLOCKING);
                        if (count <= 0) throw new IllegalStateException("PCM write failed: " + count);
                        offset += count;
                    }
                }
            } catch (RuntimeException failure) {
                android.util.Log.e("MultitrackLooper", "Audio output failed", failure);
                runOnUiThread(() -> {
                    if (playback == this) stop("Audio output unavailable");
                });
            } finally {
                if (track != null) track.release();
            }
        }
    }

    private static short[] makeClip(boolean bass) {
        var samples = new short[LoopMixer.BAR_FRAMES];
        int beatFrames = LoopMixer.BAR_FRAMES / 4;
        for (int i = 0; i < samples.length; i++) {
            double time = (i % beatFrames) / (double) LoopMixer.SAMPLE_RATE;
            double frequency = bass ? (i / beatFrames % 2 == 0 ? 110 : 82.41) : 65;
            double envelope = Math.min(time * 200, 1) * Math.exp(-(bass ? 10 : 28) * time);
            double fade = Math.min((0.5 - time) * 200, 1);
            samples[i] = (short) (6500 * envelope * fade * Math.sin(2 * Math.PI * frequency * time));
        }
        return samples;
    }
}
