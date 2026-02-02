package de.t14d3.trickiertrials;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.VaultBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

public class TrialVaultRefresher implements Listener {

    private final JavaPlugin plugin;
    private long trialVaultResetTime;

    public TrialVaultRefresher(JavaPlugin plugin, Long trialVaultResetTime) {
        this.plugin = plugin;
        this.trialVaultResetTime = trialVaultResetTime;
    }

    @EventHandler
    public void onVaultInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || trialVaultResetTime == -1L) {
            return;
        }
        if (event.getClickedBlock().getType() == Material.VAULT) {

            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            BlockPos blockPos = new BlockPos(x, y, z);
            ServerLevel serverLevel = ((CraftWorld) event.getClickedBlock().getWorld()).getHandle();
            BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);

            if (!(blockEntity instanceof VaultBlockEntity)) {
                return;
            }
            VaultBlockEntity vaultBlockEntity = (VaultBlockEntity) blockEntity;

            // Check if Ominous - Ominous Vaults have the OMINOUS property set to true
            try {
                if (!vaultBlockEntity.getBlockState().getValue(VaultBlock.OMINOUS)) {
                     // Not ominous - ignore (allow vanilla behavior without cooldown)
                     return;
                }
            } catch (Exception ignored) {
                // Determine checking safety, fallback if necessary
            }

            VaultServerData vaultServerData = vaultBlockEntity.getServerData();

            NamespacedKey key = new NamespacedKey(plugin, event.getPlayer().getUniqueId().toString());
            PersistentDataContainer dataContainer = blockEntity.persistentDataContainer;
            long timestamp = System.currentTimeMillis();

            try {
                // Reflexe pro získání seznamu hráčů (rewardedPlayers)
                // Hledáme pole typu Set<UUID>
                Field f = null;
                for (Field field : VaultServerData.class.getDeclaredFields()) {
                    if (field.getType().equals(Set.class)) {
                        // checks generic type if possible or assumes it's the right set
                        f = field;
                        break;
                    }
                }
                if (f == null) {
                    // Fallback to "e" or specific name if known
                     f = VaultServerData.class.getDeclaredField("e");
                }

                f.setAccessible(true);
                Set<UUID> players = (Set<UUID>) f.get(vaultServerData);

                if (!players.contains(event.getPlayer().getUniqueId())) {
                    // Hráč tento vault ještě nevybral (není v seznamu rewarded)
                    // Povolíme interakci a ULOŽÍME čas. Pokud vault otevře, tento čas bude sloužit jako start cooldownu
                    // Pokud neotevře (nemá klíč), čas se přepíše při příštím pokusu, dokud neotevře.
                    dataContainer.set(key, PersistentDataType.LONG, timestamp);
                    return;
                }

                // Hráč JE v seznamu -> už vault vybral. Kontrolujeme cooldown.

                if (!dataContainer.has(key, PersistentDataType.LONG)) {
                     // Je v seznamu, ale nemá timestamp? (Divný stav, asi reload nebo stará data)
                     // Nastavíme time a blokujeme, nebo resetujeme?
                     // Bezpečnější: Resetovat, aby se to srovnalo.
                     players.remove(event.getPlayer().getUniqueId());
                     return;
                }

                long stored = dataContainer.get(key, PersistentDataType.LONG);
                long readyAt = stored + trialVaultResetTime;
                long remaining = readyAt - timestamp;

                if (remaining <= 0) {
                     // Čas vypršel -> Resetujeme vault pro hráče
                     players.remove(event.getPlayer().getUniqueId());
                     // Aktualizujeme timestamp pro "nový" cyklus (pokud teď otevře)
                     dataContainer.set(key, PersistentDataType.LONG, timestamp);
                } else {
                     // Čas nevypršel -> Blokujeme a píšeme zprávu
                    long seconds = remaining / 1000;
                    long hours = seconds / 3600;
                    long minutes = (seconds % 3600) / 60;
                    long secs = seconds % 60;

                    TagResolver[] tagResolver = new TagResolver[]{
                            Placeholder.parsed("time", String.format("%d:%02d:%02d", hours, minutes, secs))
                    };

                    var message = plugin.getConfig().getString("vault-locked-message");
                    var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
                    var component = mm.deserialize(message, tagResolver);

                    event.getPlayer().sendMessage(component);
                    event.setCancelled(true);
                }

            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }
}
