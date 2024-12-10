package com.github.lsetzl;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepRecorderExtension extends ControllerExtension {
    protected StepRecorderExtension(final StepRecorderExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    private final List<Double> STEP_SIZES = Arrays.asList(0.125, 0.5 / 3, 0.25, 0.5, 1.0, 2.0, 4.0);

    private final int OCTAVE_KEYS = 12;

    Preferences preferences;
    SettableRangedValue velocity;
    SettableRangedValue scaling;

    private PinnableCursorClip cursorClip;
    private double stepSize = 0.25;
    private double previewStepSize = 0.25;
    private int step = 0;
    private final List<Integer> pressingKeys = new ArrayList<>();
    private int duration = 0;

    @Override
    public void init() {
        final ControllerHost host = getHost();

        preferences = host.getPreferences();
        velocity = preferences.getNumberSetting("Velocity", "Velocity", 0, 125, 5, "", 100);
        scaling = preferences.getNumberSetting("Scaling", "Scaling", 0, 1, 0.05, "", 0.9);

        cursorClip = host.createCursorTrack(0, 0).createLauncherCursorClip(1024, 128);
        cursorClip.setStepSize(stepSize);

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
            clearStepsAtTime();
        } else if (isReset(message)) {
            resetTimeRange();
        } else if (isRewind(message)) {
            rewindTimeRange();
        } else if (isForward(message)) {
            forwardTimeRange();
        } else if (isDecreaseStepSize(message)) {
            decreaseStepSize();
        } else if (isIncreaseStepSize(message)) {
            increaseStepSize();
        } else if (message.isNoteOn()) {
            addNote(message);
        } else if (message.isNoteOff() && pressingKeys.contains(message.getData1())) {
            finishNote(message);
        }
    }

    private void finishNote(ShortMidiMessage message) {
        pressingKeys.remove(Integer.valueOf(message.getData1()));
        if (pressingKeys.isEmpty()) {
            step += duration;
            duration = 0;
            setTimeRange();
        }
    }

    private void addNote(ShortMidiMessage message) {
        if (duration == 0) duration = 1;
        pressingKeys.add(message.getData1());
        setNoteSteps();
    }

    private void clearStepsAtTime() {
        cursorClip.clearStepsAtX(0, step);
    }

    private void resetTimeRange() {
        pressingKeys.clear();
        step = 0;
        duration = 0;
        setTimeRange();
    }

    private void forwardTimeRange() {
        if (!pressingKeys.isEmpty()) {
            duration += 1;
            setNoteSteps();
        } else {
            step += 1;
            setTimeRange();
        }
    }

    private void rewindTimeRange() {
        if (!pressingKeys.isEmpty()) {
            if (duration > 1) {
                duration -= 1;
                setNoteSteps();
            }
        } else {
            if (step > 0) step -= 1;
            setTimeRange();
        }
    }

    private void decreaseStepSize() {
        changeStepSize(-1);
    }

    private void increaseStepSize() {
        changeStepSize(1);
    }

    private void changeStepSize(int addIndex) {
        int index = STEP_SIZES.indexOf(stepSize) + addIndex;
        if (index < 0 || index >= STEP_SIZES.size()) return;

        previewStepSize = stepSize;
        stepSize = STEP_SIZES.get(index);
        step = (int) (step * previewStepSize / stepSize);
        duration = (int) (duration * previewStepSize / stepSize);
        cursorClip.setStepSize(stepSize);
        setTimeRange();
    }

    private void setTimeRange() {
        int range = 1;
        if (duration > 0) {
            range = duration;
        }
        cursorClip.setStep(0, step, 0, 0, stepSize * range);
        cursorClip.clearStep(step, 0);
    }

    private void setNoteSteps() {
        pressingKeys.forEach(key -> {
            cursorClip.clearStep(step, key);
            cursorClip.setStep(0, step, key, (int) velocity.getRaw(), stepSize * duration * scaling.getRaw());
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

    private boolean isDecreaseStepSize(ShortMidiMessage message) {
        return message.isNoteOn() && message.getData1() % OCTAVE_KEYS == 8;
    }

    private boolean isIncreaseStepSize(ShortMidiMessage message) {
        return message.isNoteOn() && message.getData1() % OCTAVE_KEYS == 10;
    }
}
