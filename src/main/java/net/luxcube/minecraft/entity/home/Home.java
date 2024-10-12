package net.luxcube.minecraft.entity.home;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
public class Home {

  public static Home constructModel(@NotNull ConfigurationSection section) {
    String worldName = section.getString("world");
    checkArgument(worldName != null, "World name not found");

    int x = section.getInt("x"),
      y = section.getInt("y"),
      z = section.getInt("z");

    double yaw = section.getDouble("yaw");
    double pitch = section.getDouble("pitch");

    return new Home(
      worldName,
      x,
      y,
      z,
      yaw,
      pitch
    );
  }

  private final String worldName;
  private final int x, y, z;
  private final double yaw, pitch;

  public Home(
    @NotNull String worldName,
    int x,
    int y,
    int z,
    double yaw,
    double pitch
  ) {
    this.worldName = worldName;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  @Nullable
  public World getWorld() {
    return Bukkit.getWorld(worldName);
  }

  @NotNull
  public Location getLocation() {
    World world = getWorld();
    if (world == null) {
      throw new IllegalStateException("World not found: " + worldName);
    }

    return new Location(getWorld(), x, y, z, (float) yaw, (float) pitch);
  }



}
