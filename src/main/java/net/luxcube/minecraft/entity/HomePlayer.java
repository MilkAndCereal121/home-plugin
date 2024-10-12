package net.luxcube.minecraft.entity;

import lombok.Getter;
import lombok.Setter;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.home.Home;
import net.luxcube.minecraft.entity.home.HomeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class HomePlayer {

  private final UUID uniqueId;

  private final List<Home> homes;

  @Setter
  private Home teamHome = null;

  public HomePlayer(
    @NotNull UUID uniqueId,
    @NotNull List<Home> homes
  ) {
    this.uniqueId = uniqueId;
    this.homes = homes;
  }

  @Nullable
  public Player getBukkitPlayer() {
    return Bukkit.getPlayer(uniqueId);
  }

  @Nullable
  public Home getHome(int index) {
    if (index < 0 || index >= homes.size()) {
      return null;
    }

    return homes.get(index);
  }

  @NotNull
  public List<Home> getHomes() {
    return Collections.unmodifiableList(homes);
  }

  public boolean isFull(@NotNull HomePlugin homePlugin) {
    Player player = getBukkitPlayer();
    if (player == null) {
      return true;
    }

    int size = 0;
    for (@NotNull String permission : homePlugin.getHomesPermission()) {
      if (player.hasPermission(permission)) {
        size++;
      }
    }

    return homes.stream()
      .filter(home -> home != null)
      .count() >= size;
  }

  public void appendHome(int index, @NotNull Home home) {
    homes.set(index, home);
  }

  public void removeHome(int index) {
    homes.set(index, null);
  }

  public int getFirstEmptyIndex() {
    for (int i = 0; i < homes.size(); i++) {
      if (homes.get(i) == null) {
        return i;
      }
    }

    return -1;
  }
}
