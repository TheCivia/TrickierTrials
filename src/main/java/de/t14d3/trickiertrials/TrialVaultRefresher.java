package de.t14d3.trickiertrials;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Lets a player loot the same ominous vault again after trial-vault-reset-time.
 * Uses the Paper Vault API (rewarded players + TileState PDC) instead of NMS and reflection,
 * so it survives Minecraft updates and the stored times are saved with the chunk.
 */
public class TrialVaultRefresher implements Listener {

    private final JavaPlugin plugin;
    private long trialVaultResetTime;

    public TrialVaultRefresher(JavaPlugin plugin, Long trialVaultResetTime) {
        this.plugin = plugin;
        this.trialVaultResetTime = trialVaultResetTime;
    }

    @EventHandler
    public void onVaultInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || trialVaultResetTime == -1L || block.getType() != Material.VAULT) {
            return;
        }

        // Only ominous vaults get the cooldown, normal ones keep vanilla behaviour
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) || !vaultData.isOminous()) {
            return;
        }

        // Snapshot state: changes are written back (and the chunk marked dirty) by update()
        if (!(block.getState() instanceof Vault vault)) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        NamespacedKey key = new NamespacedKey(plugin, uuid.toString());
        PersistentDataContainer dataContainer = vault.getPersistentDataContainer();
        long timestamp = System.currentTimeMillis();

        if (!vault.hasRewardedPlayer(uuid)) {
            // Not looted yet: remember the time. If the player opens the vault now, this is the start of the cooldown;
            // if not (no key), it is overwritten on the next attempt.
            dataContainer.set(key, PersistentDataType.LONG, timestamp);
            vault.update();
            return;
        }

        Long stored = dataContainer.get(key, PersistentDataType.LONG);
        if (stored == null) {
            // Looted before this plugin tracked it (or old data): start the cooldown now instead of
            // resetting the vault immediately, so missing data can never hand out a free reset.
            dataContainer.set(key, PersistentDataType.LONG, timestamp);
            vault.update();
            stored = timestamp;
        }

        long remaining = stored + trialVaultResetTime - timestamp;

        if (remaining <= 0) {
            // Cooldown over: the player may loot this vault again
            vault.removeRewardedPlayer(uuid);
            dataContainer.set(key, PersistentDataType.LONG, timestamp);
            vault.update();
            return;
        }

        // Still on cooldown: block and tell the player
        long seconds = remaining / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        TagResolver[] tagResolver = new TagResolver[]{
                Placeholder.unparsed("time", String.format("%d:%02d:%02d", hours, minutes, secs))
        };

        var message = plugin.getConfig().getString("vault-locked-message");
        if (message != null) {
            var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
            event.getPlayer().sendMessage(mm.deserialize(message, tagResolver));
        }
        event.setCancelled(true);
    }
}
