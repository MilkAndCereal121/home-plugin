package net.luxcube.minecraft.command;

import lombok.RequiredArgsConstructor;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Completer;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.entity.home.Home;
import net.luxcube.minecraft.entity.home.HomeType;
import net.luxcube.minecraft.task.PendingTeleportTask;
import net.luxcube.minecraft.util.WorldGuardUtil;
import net.luxcube.minecraft.view.ListPlayerHomesView;
import net.luxcube.minecraft.vo.HomeVO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HomeCommand {

  private final HomePlugin homePlugin;
  private final HomeVO homeVO;

  public HomeCommand(
          @NotNull HomePlugin homePlugin
  ) {
    this.homePlugin = homePlugin;
    this.homeVO = homePlugin.getHomeVO();
  }

  @Command(
    name = "home",
    target = CommandTarget.PLAYER,
    aliases = {"h", "homes"}
  )
  public void handleHomeCommand(@NotNull Context<Player> context, @Optional(def = "-1") int index) {
    Player player = context.getSender();

    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return;
    }

    if (index >= 0) {
      Home home = homePlayer.getHome(index);
      if (home == null) {
        return;
      }

      scheduleTeleport(player, home);
    } else {
      homePlugin.getViewFrame()
        .open(
          ListPlayerHomesView.class,
          player,
          Map.of(
            "home_player",
            homePlayer
          )
        );
    }
  }

  @Command(
    name = "sethome",
    target = CommandTarget.PLAYER
  )
  public void handleSetHomeCommand(@NotNull Context<Player> context, @Optional String index) {
    Player player = context.getSender();

    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return;
    }

    if (homePlayer.isFull(homePlugin)) {
      String message = homeVO.getMessage("home-reached-limit");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      return;
    }

    if (!WorldGuardUtil.canSetHome(player.getLocation(), player)) {
      String message = homeVO.getMessage("set-home-block-region");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      return;
    }

    int targetIndex = 0;
    if (index != null) {
      try {
        targetIndex = Integer.parseInt(index);
      } catch (NumberFormatException e) {
        String message = homeVO.getMessage("invalid-index");

        player.sendMessage(message);
        player.sendActionBar(message);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        return;
      }
    } else {
      targetIndex = homePlayer.getFirstEmptyIndex();
    }

    if (targetIndex == -1) {
      String message = homeVO.getMessage("home-reached-limit");

      player.sendMessage(message);
      player.sendActionBar(message);

      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      return;
    }

    Location location = player.getLocation();

    Home home = new Home(
      location.getWorld()
        .getName(),
      location.getBlockX(),
      location.getBlockY(),
      location.getBlockZ(),
      location.getYaw(),
      location.getPitch()
    );

    homePlayer.appendHome(targetIndex, home);

    String message = homeVO.getMessage("home-set");

    player.sendMessage(message);
    player.sendActionBar(message);

  }

  @Command(
    name = "delhome",
    target = CommandTarget.PLAYER
  )
  public void handleDelHomeCommand(@NotNull Context<Player> context, int index) {
    Player player = context.getSender();

    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return;
    }

    Home home = homePlayer.getHome(index);
    if (home == null) {
      String message = homeVO.getMessage("home-not-exist");

      player.sendMessage(message);
      player.sendActionBar(message);

      return;
    }

    homePlayer.removeHome(index);

    String message = homeVO.getMessage("home-deleted");

    player.sendMessage(message);
    player.sendActionBar(message);

  }

  @Completer(name = "delhome")
  public List<String> completeDelHomeCommand(@NotNull Context<CommandSender> context) {
    CommandSender sender = context.getSender();
    if (!(sender instanceof Player player)) {
      return Collections.emptyList();
    }

    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return Collections.emptyList();
    }

    if (context.argsCount() != 1) {
      return Collections.emptyList();
    }

    return availableIndexes(homePlayer);
  }

  @Completer(name = "home")
  public List<String> completeHomeCommand(@NotNull Context<CommandSender> context) {
    CommandSender sender = context.getSender();
    if (!(sender instanceof Player player)) {
      return Collections.emptyList();
    }

    HomePlayer homePlayer = homePlugin.getHomeService()
      .getHomePlayer(player.getUniqueId());

    if (homePlayer == null) {
      return Collections.emptyList();
    }

    if (context.argsCount() != 1) {
      return Collections.emptyList();
    }

    return availableIndexes(homePlayer);
  }

  private List<String> availableIndexes(@NotNull HomePlayer homePlayer) {
    List<String> availableIndexes = new ArrayList<>(homePlugin.getHomesPermission().size());
    for (int index = 0; index < homePlugin.getHomesPermission().size(); index++) {
      if (homePlayer.getHome(index) != null) {
        availableIndexes.add(String.valueOf(index));
      }
    }

    return availableIndexes;
  }

  private void scheduleTeleport(@NotNull Player from, @NotNull Home home) {
    PendingTeleportTask task = new PendingTeleportTask(from, home, homePlugin.getDelayToTeleport());
    task.setBukkitTask(
      Bukkit.getScheduler()
        .runTaskTimer(homePlugin, task, 0, 20)
    );
  }
}
