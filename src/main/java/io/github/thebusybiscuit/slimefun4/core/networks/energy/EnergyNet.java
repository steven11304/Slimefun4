package io.github.thebusybiscuit.slimefun4.core.networks.energy;

import io.github.thebusybiscuit.cscorelib2.math.DoubleHandler;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.holograms.SimpleHologram;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

/**
 * The {@link EnergyNet} is an implementation of {@link Network} that deals with
 * electrical energy being send from and to nodes.
 *
 * @author meiamsome
 * @author TheBusyBiscuit
 *
 * @see Network
 * @see EnergyNetComponent
 * @see EnergyNetProvider
 * @see EnergyNetComponentType
 *
 */
public class EnergyNet extends Network {

    private static final int RANGE = 6;

    private final Map<Location, EnergyNetProvider> generators = new HashMap<>();
    private final Map<Location, EnergyNetComponent> capacitors = new HashMap<>();
    private final Map<Location, EnergyNetComponent> consumers = new HashMap<>();

    protected EnergyNet(Location l) {
        super(SlimefunPlugin.getNetworkManager(), l);
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public NetworkComponent classifyLocation(Location l) {
        if (regulator.equals(l)) {
            return NetworkComponent.REGULATOR;
        }

        EnergyNetComponent component = getComponent(l);

        if (component == null) {
            return null;
        } else {
            switch (component.getEnergyComponentType()) {
                case CAPACITOR:
                    return NetworkComponent.CONNECTOR;
                case CONSUMER:
                case GENERATOR:
                    return NetworkComponent.TERMINUS;
                default:
                    return null;
            }
        }
    }

    @Override
    public void onClassificationChange(Location l, NetworkComponent from, NetworkComponent to) {
        if (from == NetworkComponent.TERMINUS) {
            generators.remove(l);
            consumers.remove(l);
        }

        EnergyNetComponent component = getComponent(l);

        if (component != null) {
            switch (component.getEnergyComponentType()) {
                case CAPACITOR:
                    capacitors.put(l, component);
                    break;
                case CONSUMER:
                    consumers.put(l, component);
                    break;
                case GENERATOR:
                    if (component instanceof EnergyNetProvider) {
                        generators.put(l, (EnergyNetProvider) component);
                    } else if (component instanceof SlimefunItem) {
                        ((SlimefunItem) component).warn("This Item is marked as a GENERATOR but does not implement the interface EnergyNetProvider!");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void tick(Block b) {
        AtomicLong timestamp = new AtomicLong(SlimefunPlugin.getProfiler().newEntry());

        if (!regulator.equals(b.getLocation())) {
            SimpleHologram.update(b, "&4檢測到附近有其他調節器");
            SlimefunPlugin.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
            return;
        }

        super.tick();

        if (connectorNodes.isEmpty() && terminusNodes.isEmpty()) {
            SimpleHologram.update(b, "&4找不到能源網絡");
        } else {
            int supply = tickAllGenerators(timestamp::getAndAdd) + tickAllCapacitors();
            int remainingEnergy = supply;
            int demand = 0;

            for (Map.Entry<Location, EnergyNetComponent> entry : consumers.entrySet()) {
                Location loc = entry.getKey();
                EnergyNetComponent component = entry.getValue();
                int capacity = component.getCapacity();
                int charge = component.getCharge(loc);

                if (charge < capacity) {
                    int availableSpace = capacity - charge;
                    demand += availableSpace;

                    if (remainingEnergy > 0) {
                        if (remainingEnergy > availableSpace) {
                            component.setCharge(loc, capacity);
                            remainingEnergy -= availableSpace;
                        } else {
                            component.setCharge(loc, charge + remainingEnergy);
                            remainingEnergy = 0;
                        }
                    }
                }
            }

            storeRemainingEnergy(remainingEnergy);
            updateHologram(b, supply, demand);
        }

        // We have subtracted the timings from Generators, so they do not show up twice.
        SlimefunPlugin.getProfiler().closeEntry(b.getLocation(), SlimefunItems.ENERGY_REGULATOR.getItem(), timestamp.get());
    }

    private void storeRemainingEnergy(int remainingEnergy) {
        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            Location loc = entry.getKey();
            EnergyNetComponent component = entry.getValue();

            if (remainingEnergy > 0) {
                int capacity = component.getCapacity();

                if (remainingEnergy > capacity) {
                    component.setCharge(loc, capacity);
                    remainingEnergy -= capacity;
                } else {
                    component.setCharge(loc, remainingEnergy);
                    remainingEnergy = 0;
                }
            } else {
                component.setCharge(loc, 0);
            }
        }

        for (Map.Entry<Location, EnergyNetProvider> entry : generators.entrySet()) {
            Location loc = entry.getKey();
            EnergyNetProvider component = entry.getValue();
            int capacity = component.getCapacity();

            if (remainingEnergy > 0) {
                if (remainingEnergy > capacity) {
                    component.setCharge(loc, capacity);
                    remainingEnergy -= capacity;
                } else {
                    component.setCharge(loc, remainingEnergy);
                    remainingEnergy = 0;
                }
            } else {
                component.setCharge(loc, 0);
            }
        }
    }

    private int tickAllGenerators(LongConsumer timings) {
        Set<Location> explodedBlocks = new HashSet<>();
        int supply = 0;

        for (Map.Entry<Location, EnergyNetProvider> entry : generators.entrySet()) {
            long timestamp = SlimefunPlugin.getProfiler().newEntry();
            Location loc = entry.getKey();
            EnergyNetProvider provider = entry.getValue();
            SlimefunItem item = (SlimefunItem) provider;

            try {
                Config config = BlockStorage.getLocationInfo(loc);
                int energy = provider.getGeneratedOutput(loc, config);

                if (provider.isChargeable()) {
                    energy += provider.getCharge(loc);
                }

                if (provider.willExplode(loc, config)) {
                    explodedBlocks.add(loc);
                    BlockStorage.clearBlockInfo(loc);

                    Slimefun.runSync(() -> {
                        loc.getBlock().setType(Material.LAVA);
                        loc.getWorld().createExplosion(loc, 0F, false);
                    });
                } else {
                    supply += energy;
                }
            } catch (Exception | LinkageError t) {
                explodedBlocks.add(loc);
                new ErrorReport<>(t, loc, item);
            }

            long time = SlimefunPlugin.getProfiler().closeEntry(loc, item, timestamp);
            timings.accept(time);
        }

        // Remove all generators which have exploded
        generators.keySet().removeAll(explodedBlocks);
        return supply;
    }

    private int tickAllCapacitors() {
        int supply = 0;

        for (Map.Entry<Location, EnergyNetComponent> entry : capacitors.entrySet()) {
            supply += entry.getValue().getCharge(entry.getKey());
        }

        return supply;
    }

    private void updateHologram(Block b, double supply, double demand) {
        if (demand > supply) {
            String netLoss = DoubleHandler.getFancyDouble(Math.abs(supply - demand));
            SimpleHologram.update(b, "&4&l- &c" + netLoss + " &7J &e\u26A1");
        } else {
            String netGain = DoubleHandler.getFancyDouble(supply - demand);
            SimpleHologram.update(b, "&2&l+ &a" + netGain + " &7J &e\u26A1");
        }
    }

    private static EnergyNetComponent getComponent(Location l) {
        SlimefunItem item = BlockStorage.check(l);

        if (item instanceof EnergyNetComponent) {
            return ((EnergyNetComponent) item);
        }

        return null;
    }

    /**
     * This attempts to get an {@link EnergyNet} from a given {@link Location}.
     * If no suitable {@link EnergyNet} could be found, a new one will be created.
     *
     * @param l The target {@link Location}
     * @return The {@link EnergyNet} at that {@link Location}, or a new one
     */
    public static EnergyNet getNetworkFromLocationOrCreate(Location l) {
        Optional<EnergyNet> energyNetwork = SlimefunPlugin.getNetworkManager().getNetworkFromLocation(l, EnergyNet.class);

        if (energyNetwork.isPresent()) {
            return energyNetwork.get();
        } else {
            EnergyNet network = new EnergyNet(l);
            SlimefunPlugin.getNetworkManager().registerNetwork(network);
            return network;
        }
    }
}
