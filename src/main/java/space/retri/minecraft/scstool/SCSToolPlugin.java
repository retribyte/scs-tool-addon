package space.retri.minecraft.scstool;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.API.SimpleClaimSystemAPI;
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Types.Claim;

public final class SCSToolPlugin extends JavaPlugin implements Listener {
    private SimpleClaimSystem scsInstance;
    private SimpleClaimSystemAPI scs;
    private static Material TOOL = Material.GOLDEN_AXE;

    @Override
    public void onEnable() {
        // retrieve the SimpleClaimSystem plugin instance from the server plugin manager
        scsInstance = (SimpleClaimSystem) getServer().getPluginManager().getPlugin("SimpleClaimSystem");
        SimpleClaimSystemAPI_Provider.initialize(scsInstance);
        scs = SimpleClaimSystemAPI_Provider.getAPI();
        getLogger().info("SCSTool enabled");
        getServer().getPluginManager().registerEvents(this, this);

        // Load tool material from config
        saveDefaultConfig();
        String toolName = getConfig().getString("tool");
        try {
            TOOL = Material.valueOf(toolName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid tool material in config: " + toolName + ". Using default tool (GOLDEN_AXE).");
            TOOL = Material.GOLDEN_AXE;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SCSTool disabled");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        getLogger().info("Player " + event.getPlayer().getName() + " has interacted using " + event.getItem());
        if (event.getItem() != null && event.getItem().getType() == TOOL) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.getPlayer().performCommand("claim see");
                return;
            }

            if (event.getPlayer().isSneaking()) {
                return;
            }

            Chunk chunk = null;
            Block block = event.getClickedBlock();
            if (block == null) {
                getLogger().info("No block clicked, using player location");
                chunk = event.getPlayer().getLocation().getChunk();
                if (chunk == null) {
                    getLogger().info("No chunk found for player location.");
                    return;
                }
            } else {
                chunk = block.getChunk();
            }
            Claim claim = scs.getClaimAtChunk(chunk);

            if (claim == null) {

                // check if an adjacent chunk is claimed by this player
                Claim adjacentClaim = null;
                int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
                for (int[] o : offsets) {
                    Chunk adjChunk = chunk.getWorld().getChunkAt(chunk.getX() + o[0], chunk.getZ() + o[1]);
                    if (adjChunk != null) {
                        adjacentClaim = scs.getClaimAtChunk(adjChunk);
                        if (adjacentClaim != null) {
                            // We will add this chunk to the adjacent claim if the player owns it
                            if (adjacentClaim.getOwner().equals(event.getPlayer().getName())) {
                                getLogger().info("Adding chunk to adjacent claim \"" + adjacentClaim.getName() + "\" for user " + event.getPlayer().getName());
                                event.getPlayer().performCommand("claim addchunk " + adjacentClaim.getName());
                                return;
                            }
                        } else {
                            continue;
                        }
                    }
                }

                getLogger().info("Trying to claim for user " + event.getPlayer().getName());
                event.getPlayer().performCommand("claim");
            } 
            else if (claim.getOwner().equals(event.getPlayer().getName())) {
                // Open claim settings
                getLogger().info("Opening settings for user " + event.getPlayer().getName());
                event.getPlayer().performCommand("claim settings");
            } 
            else {
                // Idk
                getLogger().info("User " + event.getPlayer().getName() + " interacted with a claim they do not own.");
                event.getPlayer().performCommand("claim see");
            }
        } else {
            return;
        }
    }

    
    @EventHandler
    public void onPlayerInteractWithPlayer(PlayerInteractEntityEvent event) {
        if (event.getPlayer().isSneaking() || !(event.getRightClicked() instanceof Player) || event.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }

        if (event instanceof PlayerInteractAtEntityEvent) {
            // Ignore - can double-fire from some clients
            return;
        }
        
        getLogger().info("Player " + event.getPlayer().getName() + " has interacted with " + event.getRightClicked().getName() + " using " + event.getPlayer().getInventory().getItemInMainHand());

        if (event.getPlayer().getInventory().getItemInMainHand() != null && event.getPlayer().getInventory().getItemInMainHand().getType() == TOOL) {
            getLogger().info("Adding player " + event.getPlayer().getName() + " to claim of " + ((Player) event.getRightClicked()).getName());
            event.getPlayer().performCommand("claim add " + ((Player) event.getRightClicked()).getName());
        }
    }
}
