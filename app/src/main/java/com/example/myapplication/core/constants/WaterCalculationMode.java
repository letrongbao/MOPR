package com.example.myapplication.core.constants;

import androidx.annotation.Nullable;

public final class WaterCalculationMode {

    public static final String ROOM = "room";
    public static final String PER_PERSON = "per_person";
    public static final String METER = "meter";

    // Legacy values found in old persisted data.
    public static final String LEGACY_PERSON = "nguoi";
    public static final String LEGACY_METER = "dong_ho";

    private WaterCalculationMode() {
    }

    public static boolean isPerPerson(@Nullable String mode) {
        return PER_PERSON.equals(mode) || LEGACY_PERSON.equals(mode);
    }

    public static boolean isMeter(@Nullable String mode) {
        return METER.equals(mode) || LEGACY_METER.equals(mode);
    }
}
