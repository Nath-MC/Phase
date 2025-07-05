package com.purpynaxx.phase.modules;

import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

public class Categories {

    private static final Categories INSTANCE = new Categories();

    private final Set<Category> categories = new HashSet<>();

    private final Map<Category, Set<Module>> modulesByCategoryMap = new HashMap<>();

    private Categories() {}

    public static Categories getInstance() {
        return INSTANCE;
    }

    public @UnmodifiableView Set<Category> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public int getCategoryCount() {
        return this.categories.size();
    }

    public @UnmodifiableView Set<Module> getModulesIn(Category category) {
        return Collections.unmodifiableSet(modulesByCategoryMap.getOrDefault(category, Collections.emptySet()));
    }

    public void submit(Module module) {
        Category category = module.getCategory();
        this.categories.add(category);
        this.modulesByCategoryMap.putIfAbsent(category, new HashSet<>());
        this.modulesByCategoryMap.get(category).add(module);
    }

}
