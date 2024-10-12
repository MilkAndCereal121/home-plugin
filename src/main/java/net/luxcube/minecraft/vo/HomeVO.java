package net.luxcube.minecraft.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.luxcube.minecraft.adapter.ItemStackAdapter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static net.luxcube.minecraft.util.Colors.translateHex;

@Getter
@AllArgsConstructor
public class HomeVO {

  private final static String MESSAGE_NOT_FOUND = "&cMessage could not be found, please contact an administrator.";

  public static HomeVO construct(@NotNull FileConfiguration config) {
    ConfigurationSection confirmHomeSection = config.getConfigurationSection("confirm-home-delete-inventory"),
            listPlayerHomesSection = config.getConfigurationSection("list-player-homes-inventory"),
            messageSection = config.getConfigurationSection("messages");

    if (confirmHomeSection == null || listPlayerHomesSection == null || messageSection == null) {
      throw new RuntimeException("Failed to retrieve configuration correctly.");
    }

    InventoryInfo confirmHome = InventoryInfo.fromSection(confirmHomeSection),
            listPlayerHomes = InventoryInfo.fromSection(
                    listPlayerHomesSection,
                    "dye-slots",
                    "home-slots",
                    "team-home-dye-slot",
                    "team-home-slot"
            );

    Map<String, String> messages = new HashMap<>();
    for (String messageKey : messageSection.getKeys(false)) {
      String coloredValue = translateHex(messageSection.getString(messageKey));
      messages.put(messageKey, coloredValue);
    }

    return new HomeVO(
            confirmHome,
            listPlayerHomes,
            messages
    );
  }

  private InventoryInfo confirmHomeDeleteInfo,
    listPlayerHomeInfo;

  private Map<String, String> messages;

  public String getMessage(@NotNull String path) {
    return translateHex(messages.getOrDefault(path, MESSAGE_NOT_FOUND));
  }

  @Data
  public static class InventoryInfo {

    public static InventoryInfo fromSection(@NotNull ConfigurationSection section, @NotNull String... extra) {

      String title = translateHex(section.getString("name"));
      int rows = section.getInt("rows", -1);

      String[] layout = section.getStringList("layout")
              .toArray(new String[0]);

      ConfigurationSection icons = section.getConfigurationSection("icons");
      checkArgument(icons != null, "Icons section cannot be null in configuration.");

      Map<String, IconInfo> iconInfo = new HashMap<>();

      for (String iconId : icons.getKeys(false)) {
        ConfigurationSection iconSection = icons.getConfigurationSection(iconId);
        if (iconSection == null) {
          continue;
        }

        int slot = icons.getInt(iconId + ".slot", -1);
        ItemStack itemStack = ItemStackAdapter.toItemStack(iconSection);

        iconInfo.put(iconId,
                new IconInfo(itemStack, slot));
      }

      List<String> fakeLayout = new ArrayList<>();
      if (layout.length == 0) {
        for (int i=0; i<rows; i++) {
          fakeLayout.add("XXXXXXXXX");
        }

        layout = fakeLayout.toArray(new String[0]);
      }

      Map<String, Object> extraMap = new HashMap<>();
      for (String path : extra) {
        extraMap.put(path, section.get(path));
      }

      return new InventoryInfo(
              title,
              layout,
              iconInfo,
              extraMap
      );
    }

    private final String title;
    private final String[] layout;

    private final Map<String, IconInfo> icons;
    private final Map<String, Object> extra;

    public int getRows() {
      return layout.length;
    }

  }

  @Data
  public static class IconInfo {

    private final ItemStack itemStack;
    private final int slot;

  }


}
