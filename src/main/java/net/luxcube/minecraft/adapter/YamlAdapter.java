package net.luxcube.minecraft.adapter;

import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.entity.home.Home;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class YamlAdapter {

  @NotNull
  public static HomePlayer constructPlayer(
    @NotNull UUID uniqueId,
    @NotNull FileConfiguration fileConfiguration,
    int maxSize
  ) {
    List<Home> collection = Arrays.asList(new Home[maxSize]);

    ConfigurationSection homes = fileConfiguration.getConfigurationSection("homes");
    for (@NotNull String key : homes.getKeys(false)) {
      ConfigurationSection home = homes.getConfigurationSection(key);
      if (home == null) {
        continue;
      }

      int index;
      try {
        index = Integer.parseInt(key);
      } catch (NumberFormatException e) {
        continue;
      }

      collection.set(index, Home.constructModel(home));
    }

    return new HomePlayer(uniqueId, collection);
  }

  public static void parsePlayer(
    @NotNull HomePlayer homePlayer,
    @NotNull FileConfiguration fileConfiguration
  ) {
    ConfigurationSection homes = fileConfiguration.createSection("homes");

    List<Home> collection = homePlayer.getHomes();

    for (int i = 0; i < collection.size(); i++) {
      Home home = collection.get(i);
      if (home == null) {
        continue;
      }

      ConfigurationSection homeSection = homes.createSection(String.valueOf(i));

      homeSection.set("world", home.getWorldName());
      homeSection.set("x", home.getX());
      homeSection.set("y", home.getY());
      homeSection.set("z", home.getZ());
      homeSection.set("yaw", home.getYaw());
      homeSection.set("pitch", home.getPitch());
    }
  }


}
