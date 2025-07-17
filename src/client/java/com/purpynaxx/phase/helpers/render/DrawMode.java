package com.purpynaxx.phase.helpers.render;

public enum DrawMode {
    FILL,
    OUTLINE,
    BOTH;

    public boolean isFill() {
        return this != OUTLINE; // this == FILL || this == BOTH
    }

    public boolean isOutline() {
        return this != FILL; // this == OUTLINE || this == BOTH
    }
}
