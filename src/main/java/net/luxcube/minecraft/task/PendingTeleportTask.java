package net.luxcube.minecraft.task;

import lombok.Setter;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.home.Home;
import net.luxcube.minecraft.vo.HomeVO;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public class PendingTeleportTask implements Runnable {

  private final Location baseLocation;
  private final Home targetLocation;

  private final Player player;

  private int remainingSeconds;

  @Setter
  private BukkitTask bukkitTask;

  public PendingTeleportTask(@NotNull Player player, @NotNull Home home, int remainingSeconds) {
    this.player = player;
    this.targetLocation = home;
    this.remainingSeconds = remainingSeconds;
    this.baseLocation = player.getLocation();
  }

  @Override
  public void run() {
    HomeVO homeVO = HomePlugin.getInstance()
            .getHomeVO();

    Location currentLocation = player.getLocation();
    if (!currentLocation.getWorld().equals(baseLocation.getWorld())
      || currentLocation.distanceSquared(baseLocation) > 1) {
      String message = homeVO.getMessage("move-cancel-teleport");

      player.sendMessage(message);
      player.sendActionBar(message);

      bukkitTask.cancel();
      return;
    }

    if (remainingSeconds <= 0) {
      String message = homeVO.getMessage("teleport-success");

      player.sendMessage(message);

      player.teleport(targetLocation.getLocation());
      bukkitTask.cancel();
      return;
    }

    String message = homeVO.getMessage("teleporting");
    message = message.replace("%seconds%", String.valueOf(remainingSeconds));

    player.sendMessage(message);
    player.sendActionBar(message);


    remainingSeconds--;
  }
}
