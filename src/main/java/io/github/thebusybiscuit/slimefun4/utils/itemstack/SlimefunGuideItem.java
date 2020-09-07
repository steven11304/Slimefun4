package io.github.thebusybiscuit.slimefun4.utils.itemstack;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.guide.CheatSheetSlimefunGuide;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedList;
import java.util.List;

/**
 * This is just a helper {@link ItemStack} class for the {@link SlimefunGuide} {@link ItemStack}.
 *
 * @author TheBusyBiscuit
 * @see SlimefunGuide
 * @see SlimefunGuideImplementation
 */
public class SlimefunGuideItem extends ItemStack {

    public SlimefunGuideItem(SlimefunGuideImplementation implementation, String name) {
        super(Material.ENCHANTED_BOOK);

        ItemMeta meta = getItemMeta();

        meta.setDisplayName(ChatColors.color(name));

        List<String> lore = new LinkedList<>();

        lore.add(implementation instanceof CheatSheetSlimefunGuide ? "&4&l僅限管理員使用" : "");
        lore.add(ChatColors.color("&e右鍵 &8\u21E8 &7瀏覽物品"));
        lore.add(ChatColors.color("&eShift + 右鍵 &8\u21E8 &7打開 設置 / 關於"));

        meta.setLore(lore);
        SlimefunPlugin.getItemTextureService().setTexture(meta, "SLIMEFUN_GUIDE");

        setItemMeta(meta);
    }

}
