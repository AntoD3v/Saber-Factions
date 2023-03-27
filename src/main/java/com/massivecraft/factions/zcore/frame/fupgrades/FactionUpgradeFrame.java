package com.massivecraft.factions.zcore.frame.fupgrades;

import com.cryptomorin.xseries.XMaterial;
import com.massivecraft.factions.*;
import com.massivecraft.factions.iface.EconomyParticipator;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.util.CC;
import com.massivecraft.factions.util.SaberGUI;
import com.massivecraft.factions.util.serializable.InventoryItem;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @Author: Driftay
 * @Date: 2/2/2023 4:45 AM
 */
public class FactionUpgradeFrame extends SaberGUI {

    private Faction faction;

    public FactionUpgradeFrame(Player player, Faction faction) {
        super(player, CC.translate(FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig()
                .getString("fupgrades.MainMenu.Title").replace("{faction}", faction.getTag())), FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.Rows", 5) * 9);
        this.faction = faction;
    }

    @Override
    public void redraw() {
        ItemStack dummy = buildDummyItem();
        FPlayer fme = FPlayers.getInstance().getByPlayer(player);
        for (int x = 0; x <= this.size - 1; ++x) {
            this.setItem(x, new InventoryItem(dummy));
        }

        for (UpgradeType upgradeType : UpgradeType.values()) {
            if (upgradeType.getSlot() != -1) {
                this.setItem(upgradeType.getSlot(), new InventoryItem(upgradeType.buildAsset(faction)).click(ClickType.LEFT, () -> {
                    if (faction.getUpgrade(upgradeType) >= upgradeType.getMaxLevel()) return;

                    int cost = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu." + upgradeType + ".Cost.level-" + (faction.getUpgrade(upgradeType) + 1));


                    if (FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getBoolean("fupgrades.usePointsAsCurrency")) {
                        if (faction.getPoints() >= cost) {
                            faction.setPoints(faction.getPoints() - cost);
                            fme.msg(TL.COMMAND_UPGRADES_POINTS_TAKEN, cost, faction.getPoints());
                            handleTransaction(fme, upgradeType);
                            faction.setUpgrade(upgradeType, faction.getUpgrade(upgradeType) + 1);
                            redraw();
                        } else {
                            fme.getPlayer().closeInventory();
                            fme.msg(TL.COMMAND_UPGRADES_NOT_ENOUGH_POINTS);
                        }
                    } else {

                        EconomyParticipator payee;

                        if (Conf.bankEnabled && FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getBoolean("fupgrades.factionPaysForUpgradeCost", false)) {
                            payee = faction;
                        } else {
                            payee = fme;
                        }

                        if (Econ.modifyMoney(payee, -cost, TextUtil.parse(TL.UPGRADE_TOUPGRADE.toString(), upgradeType), TextUtil.parse(TL.UPGRADE_FORUPGRADE.toString(), upgradeType))) {
                            handleTransaction(fme, upgradeType);
                            faction.setUpgrade(upgradeType, faction.getUpgrade(upgradeType) + 1);
                            redraw();
                        } else if (fme.hasMoney(cost)) {
                            fme.takeMoney(cost);
                            handleTransaction(fme, upgradeType);
                            faction.setUpgrade(upgradeType, faction.getUpgrade(upgradeType) + 1);
                            redraw();
                        }
                    }
                }));
            }
        }
    }

    private void handleTransaction(FPlayer fme, UpgradeType value) {
        Faction fac = fme.getFaction();
        switch (value) {
            case CHEST:
                updateChests(fac);
                break;
            case POWER:
                updateFactionPowerBoost(fac);
                break;
            case TNT:
                updateTNT(fac);
                break;
            case WARP:
                updateWarps(fac);
                break;
            case SPAWNERCHUNKS:
                if (Conf.allowSpawnerChunksUpgrade) {
                    updateSpawnerChunks(fac);
                    break;
                }
        }
    }

    private void updateWarps(Faction faction) {
        int level = faction.getUpgrade(UpgradeType.WARP);
        int size = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.Warps.warp-limit.level-" + (level + 1));
        faction.setWarpsLimit(size);
    }

    private void updateSpawnerChunks(Faction faction) {
        int level = faction.getUpgrade(UpgradeType.SPAWNERCHUNKS);
        int size = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.SpawnerChunks.chunk-limit.level-" + (level + 1));
        faction.setAllowedSpawnerChunks(size);
    }

    private void updateTNT(Faction faction) {
        int level = faction.getUpgrade(UpgradeType.TNT);
        int size = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.TNT.tnt-limit.level-" + (level + 1));
        faction.setTntBankLimit(size);
    }

    private void updateChests(Faction faction) {
        String invName = CC.translate(FactionsPlugin.getInstance().getConfig().getString("fchest.Inventory-Title"));
        for (Player player : faction.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equalsIgnoreCase(invName)) player.closeInventory();
        }
        int level = faction.getUpgrade(UpgradeType.CHEST);
        int size = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getInt("fupgrades.MainMenu.Chest.Chest-Size.level-" + (level + 1));
        faction.setChestSize(size * 9);
    }

    private void updateFactionPowerBoost(Faction f) {
        double boost = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getDouble("fupgrades.MainMenu.Power.Power-Boost.level-" + (f.getUpgrade(UpgradeType.POWER) + 1));
        if (boost < 0.0) return;
        f.setPowerBoost(boost);
    }

    public Faction getFaction() {
        return faction;
    }

    private ItemStack buildDummyItem() {
        ConfigurationSection config = FactionsPlugin.getInstance().getFileManager().getUpgrades().getConfig().getConfigurationSection("fupgrades.MainMenu.DummyItem");
        ItemStack item = XMaterial.matchXMaterial(config.getString("Type")).get().parseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(CC.translate(config.getStringList("Lore")));
            meta.setDisplayName(CC.translate(config.getString("Name")));
            item.setItemMeta(meta);
        }
        return item;
    }
}