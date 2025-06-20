package com.teraim.fieldapp.loadermodule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, dynamic registry that provides fast, name-based access to a
 * collection of ConfigurationModule objects.
 *
 * This version returns null if a module is not found.
 */
public final class ModuleRegistry {

    private final Map<String, ConfigurationModule> moduleMap;

    /**
     * Creates a new, empty registry.
     */
    public ModuleRegistry() {
        this.moduleMap = new ConcurrentHashMap<>();
    }

    /**
     * Adds a list of modules to the registry.
     * If a module with the same file name already exists, it will be overwritten.
     *
     * @param modulesToAdd The list of modules to add.
     */
    public void add(List<ConfigurationModule> modulesToAdd) {
        if (modulesToAdd == null) {
            return;
        }
        for (ConfigurationModule module : modulesToAdd) {
            if (module != null && module.getFileName() != null) {
                moduleMap.put(module.getFileName(), module);
            }
        }
    }
    public void add(ConfigurationModule module) {
        moduleMap.put(module.getFileName(), module);
    }

    /**
     * Retrieves a module by its unique file name.
     *
     * @param moduleName The file name of the module to find.
     * @return The ConfigurationModule object if found, otherwise null.
     */
    public ConfigurationModule getModule(String moduleName) {
        // Return the object directly, or null if the key does not exist in the map.
        return moduleMap.get(moduleName);
    }

    /**
     * Checks if a module with the given name exists in the registry.
     *
     * @param moduleName The file name to check for.
     * @return true if a module with the given name exists, false otherwise.
     */
    public boolean hasModule(String moduleName) {
        return moduleMap.containsKey(moduleName);
    }

    /**
     * Returns a snapshot of all modules currently stored in the registry.
     *
     * @return An unmodifiable, point-in-time collection of all modules.
     */
    public Collection<ConfigurationModule> getAllModules() {
        return Collections.unmodifiableCollection(new ArrayList<>(moduleMap.values()));
    }

    /**
     * Clears all modules from the registry.
     */
    public void clear() {
        moduleMap.clear();
    }
}