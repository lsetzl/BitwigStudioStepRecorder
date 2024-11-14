package com.github.lsetzl;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.PinnableCursorClip;

import java.util.ArrayList;
import java.util.List;

public class StepRecorderExtension extends ControllerExtension {
    protected StepRecorderExtension(final StepRecorderExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    private final int OCTAVE_KEYS = 12;
    private final int VELOCITY = 100;
    private final int DIVISION = 16;
    private final double SCALING = 0.9;

    private PinnableCursorClip cursorClip;
    private int step = 0;
    private final List<Integer> pressedKeys = new ArrayList<>();
    private int duration = 0;

    @Override
    public void init() {
        final ControllerHost host = getHost();

        CursorTrack cursorTrack = host.createCursorTrack(0, 0);
        cursorClip = cursorTrack.createLauncherCursorClip(1024, 128);
        cursorClip.setStepSize(getStepSize());

        final MidiIn midiIn = host.getMidiInPort(0);
        midiIn.setMidiCallback((ShortMidiMessageReceivedCallback) this::onMidiIn);

        host.showPopupNotification("StepRecorder Initialized");
    }

    @Override
    public void exit() {
        getHost().showPopupNotification("StepRecorder Exited");
    }

    @Override
    public void flush() {
    }

    private void onMidiIn(final ShortMidiMessage message) {
        final ControllerHost host = getHost();
        host.println(message.toString());

        if (isClear(message)) {
            cursorClip.clearStepsAtX(0, step);
        } else if (isReset(message)) {
            pressedKeys.clear();
            step = 0;
            duration = 0;
            setTimeRange();
        } else if (isRewind(message)) {
            if (!pressedKeys.isEmpty()) {
                if (duration > 1) {
                    duration -= 1;
                    setKeyingNoteSteps();
                }
            } else {
                if (step > 0) step -= 1;
                setTimeRange();
            }
        } else if (isForward(message)) {
            if (!pressedKeys.isEmpty()) {
                duration += 1;
                setKeyingNoteSteps();
            } else {
                step += 1;
                setTimeRange();
            }
        } else if (message.isNoteOn()) {
            if (duration == 0) duration = 1;
            pressedKeys.add(message.getData1());
            setKeyingNoteSteps();
        } else if (message.isNoteOff() && pressedKeys.contains(message.getData1())) {
            pressedKeys.remove(Integer.valueOf(message.getData1()));
            if (pressedKeys.isEmpty()) {
                step += duration;
                duration = 0;
                setTimeRange();
            }
        }
    }

    private double getStepSize() {
        return 1.0 / DIVISION;
    }

    private void setTimeRange() {
        int range = 1;
        if (duration > 0) {
            range = duration;
        }
        cursorClip.setStep(0, step, 0, 0, getStepSize() * range);
        cursorClip.clearStep(step, 0);
    }

    private void setKeyingNoteSteps() {
        pressedKeys.forEach(key -> {
            cursorClip.clearStep(step, key);
            cursorClip.setStep(0, step, key, VELOCITY, getStepSize() * duration * SCALING);
        });
        setTimeRange();
    }

    private boolean isClear(ShortMidiMessage message) {
        return message.isPitchBend() && message.getData1() == 0 && message.getData2() == 0;
    }

    private boolean isReset(ShortMidiMessage message) {
        return message.isPitchBend() && message.getData1() == 127 && message.getData2() == 127;
    }

    private boolean isRewind(ShortMidiMessage message) {
        return message.isNoteOn() && message.getData1() % OCTAVE_KEYS == 1;
    }

    private boolean isForward(ShortMidiMessage message) {
        return message.isNoteOn() && message.getData1() % OCTAVE_KEYS == 3;
    }
}