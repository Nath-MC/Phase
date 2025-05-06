package com.purpynaxx.phase.modules.impl;

public enum Category {
    VISUALS;

    public String getFriendlyName() {
        String name = this.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
