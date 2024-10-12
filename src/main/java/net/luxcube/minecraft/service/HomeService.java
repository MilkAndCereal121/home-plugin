package net.luxcube.minecraft.service;

import net.luxcube.minecraft.entity.HomePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class HomeService {

  private final Map<UUID, HomePlayer> homePlayers = new Hashtable<>();

  @NotNull
  public HomePlayer getHomePlayer(@NotNull UUID uniqueId) {
    return homePlayers.get(uniqueId);
  }

  public void put(@NotNull HomePlayer homePlayer) {
    homePlayers.put(homePlayer.getUniqueId(), homePlayer);
  }

  public void remove(@NotNull HomePlayer homePlayer) {
    homePlayers.remove(homePlayer.getUniqueId());
  }

  public void remove(@NotNull UUID uniqueId) {
    homePlayers.remove(uniqueId);
  }

  public boolean contains(@NotNull UUID uniqueId) {
    return homePlayers.containsKey(uniqueId);
  }

  public Collection<HomePlayer> getHomePlayers() {
    return homePlayers.values();
  }
}
