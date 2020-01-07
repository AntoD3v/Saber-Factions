package com.massivecraft.factions.cmd.wild;


import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class CmdWild extends FCommand {
    public static HashMap<Player, Integer> waitingTeleport;
    public static HashMap<Player, String> teleportRange;
    public static HashSet<Player> teleporting;
    public CmdWild() {
        super();
        this.aliases.addAll(Aliases.wild);
        this.requirements = new CommandRequirements.Builder(Permission.WILD)
                .playerOnly()
                .build();
         waitingTeleport = new HashMap<>();
        teleporting = new HashSet<>();
        teleportRange = new HashMap<>();
        startWild();
    }
    @Override
    public void perform(CommandContext context) {
        if (!waitingTeleport.containsKey(context.player)) {
            context.player.openInventory(new WildGUI(context.player, context.fPlayer).getInventory());
        } else {context.fPlayer.msg(TL.COMMAND_WILD_WAIT);}
    }

    public void startWild() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(FactionsPlugin.instance, () -> {
            for (Player p : waitingTeleport.keySet()) {
                int i = waitingTeleport.get(p) - 1;
                if (i > 0) {
                    if (i != 1) {
                        p.sendMessage(TL.COMMAND_WILD_WAIT.format((i + " Seconds")));
                    } else {
                        p.sendMessage(TL.COMMAND_WILD_WAIT.format((i + " Second")));
                    }
                    waitingTeleport.replace(p, i);
                } else {
                    p.sendMessage(TL.COMMAND_WILD_SUCCESS.toString());
                    waitingTeleport.remove(p);
                    attemptTeleport(p);
                }
            }
        }, 0L, 20L);
    }
    public void attemptTeleport(Player p) {
        boolean success = false;
        int tries = 0;
        ConfigurationSection c = FactionsPlugin.getInstance().getConfig().getConfigurationSection("Wild.Zones." + teleportRange.get(p));
        while (tries < 5) {
            int x = new Random().nextInt((c.getInt("Range.MaxX") - c.getInt("Range.MinX")) + 1) + c.getInt("Range.MinX");
            int z = new Random().nextInt((c.getInt("Range.MaxZ") - c.getInt("Range.MinZ")) + 1) + c.getInt("Range.MinZ");
            if (Board.getInstance().getFactionAt(new FLocation(p.getWorld().getName(), x, z)).isWilderness()) {
                success = true;
                FLocation loc = new FLocation(p.getWorld().getName(), x, z);
                teleportRange.remove(p);
                if (!FPlayers.getInstance().getByPlayer(p).takeMoney(c.getInt("Cost"))) {
                    p.sendMessage(TL.GENERIC_NOTENOUGHMONEY.toString());
                    return;
                }
                teleportPlayer(p, loc);
                break;
            }
            tries++;
        }
        if (!success) {p.sendMessage(TL.COMMAND_WILD_FAILED.toString());}
    }
    public void teleportPlayer(Player p, FLocation loc) {
        Location finalLoc;
        if (FactionsPlugin.getInstance().getConfig().getBoolean("Wild.Arrival.SpawnAbove")) {
            finalLoc = new Location(p.getWorld(), loc.getX(), p.getWorld().getHighestBlockYAt(Math.round(loc.getX()), Math.round(loc.getZ())) + FactionsPlugin.getInstance().getConfig().getInt("Wild.Arrival.SpawnAboveBlocks", 1), loc.getZ());
        } else {finalLoc = new Location(p.getWorld(), loc.getX(), p.getWorld().getHighestBlockYAt(Math.round(loc.getX()), Math.round(loc.getZ())), loc.getZ());}
        p.teleport(finalLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        setTeleporting(p);
        applyEffects(p);
    }
    public void applyEffects(Player p) {
        for (String s : FactionsPlugin.getInstance().getConfig().getStringList("Wild.Arrival.Effects")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.getByName(s), 40, 1));
        }
    }
    public void setTeleporting(Player p) {
        teleporting.add(p);
        Bukkit.getScheduler().scheduleSyncDelayedTask(FactionsPlugin.instance, () -> teleporting.remove(p), FactionsPlugin.getInstance().getConfig().getInt("Wild.Arrival.FallDamageWindow") * 20);
    }
    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_WILD_DESCRIPTION;
    }
}