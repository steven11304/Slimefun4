package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.FarmerShoes;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This {@link Listener} is responsible for handling all boots provided by
 * Slimefun, such as the {@link StomperBoots} or any {@link SlimefunArmorPiece} that
 * is a pair of boots and needs to listen to an {@link EntityDamageEvent}.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 */
public class SlimefunBootsListener implements Listener {

    public SlimefunBootsListener(SlimefunPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (e.getCause() == DamageCause.FALL) {
                onFallDamage(e);
            } else if (e instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) e;

                if (event.getDamager() instanceof EnderPearl) {
                    onEnderPearlDamage(e);
                }
            }
        }
    }

    private void onFallDamage(EntityDamageEvent e) {
        Player p = (Player) e.getEntity();
        SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

        if (boots != null) {
            // Check if the boots were researched
            if (!Slimefun.hasUnlocked(p, boots, true)) {
                return;
            }

            if (boots instanceof StomperBoots) {
                e.setCancelled(true);
                ((StomperBoots) boots).stomp(e);
            } else if (boots.getID().equals("SLIME_BOOTS") || boots.getID().equals("SLIME_STEEL_BOOTS")) {
                e.setCancelled(true);
            }
        }
    }

    private void onEnderPearlDamage(EntityDamageEvent e) {
        Player p = (Player) e.getEntity();
        SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

        if (boots != null && boots.getID().equals("ENDER_BOOTS") && Slimefun.hasUnlocked(p, boots, true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTrample(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
            Block b = e.getClickedBlock();

            if (b != null && b.getType() == Material.FARMLAND) {
                Player p = e.getPlayer();
                SlimefunItem boots = SlimefunItem.getByItem(p.getInventory().getBoots());

                if (boots instanceof FarmerShoes && Slimefun.hasUnlocked(p, boots, true)) {
                    e.setCancelled(true);
                }
            }
        }
    }
}