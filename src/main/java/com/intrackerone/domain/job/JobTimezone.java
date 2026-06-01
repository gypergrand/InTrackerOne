package com.intrackerone.domain.job;

public enum JobTimezone {
    UTC("UTC", "UTC+0"),
    PST("PST", "UTC-8"),
    MST("MST", "UTC-7"),
    CST_US("CST (US)", "UTC-6"),
    EST("EST", "UTC-5"),
    CET("CET", "UTC+1"),
    EET("EET", "UTC+2"),
    EMEA("EMEA", "UTC+2"),
    GMT("GMT", "UTC+0"),
    WEST("WEST", "UTC+1"),
    MSK("MSK", "UTC+3"),
    IST("IST", "UTC+5:30"),
    CST_CHINA("CST (China)", "UTC+8"),
    SGT("SGT", "UTC+8"),
    JST("JST", "UTC+9"),
    KST("KST", "UTC+9"),
    AEST("AEST", "UTC+10"),
    AEDT("AEDT", "UTC+11"),
    NZST("NZST", "UTC+12"),
    NZDT("NZDT", "UTC+13"),
    OTHER("Other", null);

    private final String displayLabel;
    private final String utcOffset;

    JobTimezone(String displayLabel, String utcOffset) {
        this.displayLabel = displayLabel;
        this.utcOffset = utcOffset;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getUtcOffset() {
        return utcOffset;
    }
}
