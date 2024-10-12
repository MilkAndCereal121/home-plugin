package net.luxcube.minecraft;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import lombok.Getter;
import me.saiintbrisson.bukkit.command.BukkitFrame;
import me.saiintbrisson.minecraft.ViewFrame;
import net.luxcube.minecraft.adapter.YamlAdapter;
import net.luxcube.minecraft.command.HomeCommand;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.hook.DonutTeamsHook;
import net.luxcube.minecraft.listener.ConnectionListener;
import net.luxcube.minecraft.service.HomeService;
import net.luxcube.minecraft.util.License;
import net.luxcube.minecraft.view.ConfirmHomeDeleteView;
import net.luxcube.minecraft.view.ListPlayerHomesView;
import net.luxcube.minecraft.vo.HomeVO;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public class HomePlugin extends JavaPlugin {

  public static StateFlag ALLOWED_SET_HOME;

  public static HomePlugin getInstance() {
    return getPlugin(HomePlugin.class);
  }

  private HomeService homeService;
  private ViewFrame viewFrame;

  private File homesFile;

  private int delayToTeleport;

  private List<String> homesPermission;

  private boolean hasDonutTeams;
  private DonutTeamsHook donutTeamsHook;

  private HomeVO homeVO;

  @Override
  public void onLoad() {
    saveDefaultConfig();

    homesFile = new File(getDataFolder(), "homes");
    if (!homesFile.exists()) {
      homesFile.mkdirs();
    }

    FileConfiguration fileConfiguration = getConfig();
    checkNotNull(fileConfiguration, "The configuration file is null");

    homeVO = HomeVO.construct(fileConfiguration);

    ConfigurationSection settings = fileConfiguration.getConfigurationSection("settings");
    checkNotNull(settings, "The settings section is null");

    delayToTeleport = settings.getInt("delay-to-teleport-in-seconds");

    ConfigurationSection permissions = fileConfiguration.getConfigurationSection("home-permissions");
    checkNotNull(permissions, "The home permissions section is null");

    homesPermission = new ArrayList<>();
    for (@NotNull String key : permissions.getKeys(false)) {
      int index = -1;
      try {
        index = Integer.parseInt(key);
      } catch (NumberFormatException e) {
        checkArgument(false, "The key must be a number");
      }

      String permission = permissions.getString(key);
      checkNotNull(permission, "The permission is null");

      checkArgument(!permission.isEmpty(), "The permission is empty");

      homesPermission.add(index -1, permission);
    }

    FlagRegistry registry = WorldGuard.getInstance()
      .getFlagRegistry();
    try {
      // create a flag with the name "my-custom-flag", defaulting to true
      StateFlag flag = new StateFlag("allowed-set-home", true);
      registry.register(flag);
      ALLOWED_SET_HOME = flag; // only set our field if there was no error
    } catch (FlagConflictException e) {
      // some other plugin registered a flag by the same name already.
      // you can use the existing flag, but this may cause conflicts - be sure to check type
      Flag<?> existing = registry.get("allowed-set-home");
      if (existing instanceof StateFlag) {
        ALLOWED_SET_HOME = (StateFlag) existing;
      }
    }
  }

  @Override
  public void onEnable() {

    if (!License.isLicenseValid(getConfig().getString("license-key", "null"))) {
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    homeService = new HomeService();

    BukkitFrame bukkitFrame = new BukkitFrame(this);
    bukkitFrame.registerCommands(
      new HomeCommand(this)
    );

    // This needs to run before view frame register.
    hasDonutTeams = Bukkit.getPluginManager().isPluginEnabled("DonutTeams");
    if (hasDonutTeams)
      donutTeamsHook = new DonutTeamsHook();

    viewFrame = ViewFrame.of(
      this,
      new ListPlayerHomesView(this, homeVO.getListPlayerHomeInfo()),
      new ConfirmHomeDeleteView(this, homeVO.getConfirmHomeDeleteInfo())
    ).register();

    Bukkit.getPluginManager()
      .registerEvents(new ConnectionListener(this), this);

  }

  @Override
  public void onDisable() {
    for (@NotNull HomePlayer homePlayer : homeService.getHomePlayers()) {
      File file = new File(homesFile, homePlayer.getUniqueId() + ".yml");
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

  @Nullable
  public String getHomePermission(int index) {
    if (index < 0 || index >= homesPermission.size()) {
      return null;
    }

    return homesPermission.get(index);
  }
}
