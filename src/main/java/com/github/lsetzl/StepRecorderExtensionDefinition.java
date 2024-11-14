package com.github.lsetzl;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class StepRecorderExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("4d28e56b-ab63-4b5d-adb5-39bf6e837d53");

    public StepRecorderExtensionDefinition() {
    }

    @Override
    public String getName() {
        return "StepRecorder";
    }

    @Override
    public String getAuthor() {
        return "Setz";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return "Generic";
    }

    @Override
    public String getHardwareModel() {
        return "StepRecorder";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 19;
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 0;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList list, final PlatformType platformType) {
    }

    @Override
    public StepRecorderExtension createInstance(final ControllerHost host) {
        return new StepRecorderExtension(this, host);
    }
}
