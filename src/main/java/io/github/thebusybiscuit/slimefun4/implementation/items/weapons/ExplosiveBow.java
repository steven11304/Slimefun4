package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * The {@link ExplosiveBow} is a {@link SlimefunBow} which creates a fake explosion when it hits
 * a {@link LivingEntity}. Any nearby {@link LivingEntity LivingEntities} get pushed away and
 * take a little bit of damage, similar to an "Area of Effect" damage.
 *
 * @author TheBusyBiscuit
 * @author Linox
 * @see SlimefunBow
 */
public class ExplosiveBow extends SlimefunBow {

    private final ItemSetting<Integer> range = new ItemSetting<Integer>("explosion-range", 3) {

        @Override
        public boolean validateInput(Integer input) {
            return super.validateInput(input) && input > 0;
        }

    };

    public ExplosiveBow(Category category, SlimefunItemStack item, ItemStack[] recipe) {
        super(category, item, recipe);

        addItemSetting(range);
    }

    @Override
    public BowShootHandler onShoot() {
        return (e, target) -> {
            target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation(), 1);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

            Collection<Entity> entites = target.getWorld().getNearbyEntities(target.getLocation(), range.getValue(), range.getValue(), range.getValue(), entity -> entity instanceof LivingEntity && entity.isValid());
            for (Entity nearby : entites) {
                LivingEntity entity = (LivingEntity) nearby;

                Vector distanceVector = entity.getLocation().toVector().subtract(target.getLocation().toVector());
                distanceVector.setY(distanceVector.getY() + 0.6);

                Vector velocity = entity.getVelocity();

                double distanceSquared = distanceVector.lengthSquared();
                double damage = calculateDamage(distanceSquared, e.getDamage());

                Vector knockback = velocity.add(distanceVector.normalize().multiply((int) (e.getDamage() / damage)));
                entity.setVelocity(knockback);

                if (!entity.getUniqueId().equals(target.getUniqueId())) {
                    EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(e.getDamager(), entity, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, damage);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        entity.damage(event.getDamage());
                    }
                }
            }
        };
    }

    private double calculateDamage(double distanceSquared, double originalDamage) {
        if (distanceSquared <= 0.05) {
            return originalDamage;
        }

        double damage = originalDamage * (1 - (distanceSquared / (range.getValue() * range.getValue())));
        return Math.min(Math.max(1, damage), originalDamage);
    }

}