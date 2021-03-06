package io.github.thebusybiscuit.slimefun4.implementation.resources;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

abstract class SlimefunResource implements GEOResource {

    private final NamespacedKey key;
    private final String defaultName;
    private final ItemStack item;
    private final int maxDeviation;
    private final boolean geoMiner;

    SlimefunResource(String key, String defaultName, ItemStack item, int maxDeviation, boolean geoMiner) {
        Validate.notNull(key, "NamespacedKey cannot be null!");
        Validate.notNull(defaultName, "The default name cannot be null!");
        Validate.notNull(item, "item cannot be null!");

        this.key = new NamespacedKey(SlimefunPlugin.instance(), key);
        this.defaultName = defaultName;
        this.item = item;
        this.maxDeviation = maxDeviation;
        this.geoMiner = geoMiner;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public String getName() {
        return defaultName;
    }

    @Override
    public ItemStack getItem() {
        return item.clone();
    }

    @Override
    public int getMaxDeviation() {
        return maxDeviation;
    }

    @Override
    public boolean isObtainableFromGEOMiner() {
        return geoMiner;
    }

}