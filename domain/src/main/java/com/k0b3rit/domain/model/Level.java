package com.k0b3rit.domain.model;

public enum Level {
    LEVEL_1(1),
    LEVEL_50(50),
    LEVEL_200(200),
    ;

    private final int level;

    Level(int level) {

        this.level = level;
    }

    public String getLevel() {
        return String.valueOf(level);
    }
}
