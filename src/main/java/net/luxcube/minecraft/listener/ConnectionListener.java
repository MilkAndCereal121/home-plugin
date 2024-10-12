package net.luxcube.minecraft.listener;

import lombok.RequiredArgsConstructor;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.adapter.YamlAdapter;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.entity.home.Home;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@RequiredArgsConstructor
public class ConnectionListener implements Listener {

  private final HomePlugin homePlugin;

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAsyncPlayerPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
    UUID uniqueId = event.getUniqueId();

    if (
      homePlugin.getHomeService()
        .getHomePlayer(uniqueId) != null
    ) {
      return;
    }

    HomePlayer homePlayer;

    File file = new File(homePlugin.getHomesFile(), uniqueId.toString() + ".yml");
    if (file.exists()) {
      FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);
      homePlayer = YamlAdapter.constructPlayer(
        uniqueId,
        fileConfiguration,
        homePlugin.getHomesPermission()
          .size()
      );
    } else {
      homePlayer = new HomePlayer(
        uniqueId,
        Arrays.asList(new Home[homePlugin.getHomesPermission().size()])
      );
    }

    homePlugin.getHomeService()
      .put(homePlayer);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
    Player player = event.getPlayer();
    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return;
    }

    File file = new File(homePlugin.getHomesFile(), player.getUniqueId() + ".yml");
    FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

    YamlAdapter.parsePlayer(
      homePlayer,
      fileConfiguration
    );

    try {
      fileConfiguration.save(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
