package com.purpynaxx.phase.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CyclingSetting<T extends Enum<T>> extends Setting<T> {

    private final List<T> enumValues = new ArrayList<>();
    private int currentIndex;

    public CyclingSetting(String name, String description, Class<T> enumClass) {
        super(name, description, enumClass.getEnumConstants()[0]);
        this.enumValues.addAll(Arrays.asList(enumClass.getEnumConstants()));
        this.currentIndex = 0;
    }

    public void onClick() {
        int nextIndex = (currentIndex + 1) % enumValues.size();
        this.setValue(this.enumValues.get(nextIndex));
        this.currentIndex = nextIndex;
    }
}