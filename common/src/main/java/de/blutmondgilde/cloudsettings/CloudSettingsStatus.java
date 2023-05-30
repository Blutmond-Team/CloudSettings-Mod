package de.blutmondgilde.cloudsettings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CloudSettingsStatus {
    BEFORE_START(false, false),
    INITIALIZED(true, false),
    FAILED(true, true);
    @Getter
    private final boolean initialized;
    @Getter
    private final boolean errored;
}
