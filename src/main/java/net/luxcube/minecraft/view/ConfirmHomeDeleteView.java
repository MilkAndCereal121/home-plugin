package net.luxcube.minecraft.view;

import lombok.val;
import me.saiintbrisson.minecraft.View;
import me.saiintbrisson.minecraft.ViewContext;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.util.ItemBuilder;
import net.luxcube.minecraft.util.ViewUtils;
import net.luxcube.minecraft.vo.HomeVO;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.luxcube.minecraft.util.Colors.*;

public class ConfirmHomeDeleteView extends View {

  private final HomePlugin homePlugin;
  private final HomeVO homeVO;

  public ConfirmHomeDeleteView(@NotNull HomePlugin homePlugin, @NotNull HomeVO.InventoryInfo inventoryInfo) {
    super(inventoryInfo.getRows(), translateHex(inventoryInfo.getTitle()));

    this.homePlugin = homePlugin;
    this.homeVO = homePlugin.getHomeVO();

    ViewUtils.cancelAllDefaultActions(this);

    HomeVO.IconInfo iconInfo = inventoryInfo.getIcons().get("decline");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
      .onClick(click -> handleDeclineDelete(click));

    iconInfo = inventoryInfo.getIcons().get("accept");
    slot(iconInfo.getSlot(), iconInfo.getItemStack())
      .onClick(click -> handleConfirmDelete(click));

    iconInfo = inventoryInfo.getIcons().get("home");
    slot(iconInfo.getSlot())
      .onRender(render -> render.setItem(
              buildHomeIcon(render, inventoryInfo.getIcons().get("home")))
      );
  }

  @Override
  protected void onRender(@NotNull ViewContext context) {
    context.set("opened_at", System.currentTimeMillis());
  }

  private int getIndex(@NotNull ViewContext viewContext) {
    return viewContext.get("home_index", () -> -1);
  }

  @Nullable
  private HomePlayer getHomePlayer(@NotNull ViewContext viewContext) {
    return viewContext.get("home_player");
  }

  @NotNull
  private long getOpenedAt(@NotNull ViewContext viewContext) {
    return viewContext.get("opened_at", System::currentTimeMillis);
  }

  private boolean isExpired(@NotNull ViewContext viewContext) {
    long openedAt = getOpenedAt(viewContext);
    return System.currentTimeMillis() - openedAt > 1000;
  }

  private ItemStack buildHomeIcon(@NotNull ViewContext viewContext, @NotNull HomeVO.IconInfo iconInfo) {
    int index = getIndex(viewContext);

    return new ItemBuilder(iconInfo.getItemStack().clone())
            .placeholders(Map.of("%index%", String.valueOf((index + 1))))
            .result();
//    return new ItemBuilder(Material.BLUE_DYE)
//      .name(translateHex("#397ff7ʜᴏᴍᴇ " + (index + 1)))
//      .result();
  }

  private void handleDeclineDelete(@NotNull ViewContext viewContext) {
    Player player = viewContext.getPlayer();
    viewContext.close();

    homePlugin.getViewFrame()
      .open(
        ListPlayerHomesView.class,
        player,
        Map.of(
          "home_player", getHomePlayer(viewContext)
        )
      );
  }

  private void handleConfirmDelete(@NotNull ViewContext viewContext) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return;
    }

    int index = getIndex(viewContext);
    if (index == -1) {
      return;
    }

    Player player = viewContext.getPlayer();

    val home = homePlayer.getHome(index);
    if (home == null) {
      String message = homeVO.getMessage("home-not-found");

      player.sendMessage(message);
      return;
    }

    if (!isExpired(viewContext)) {
      String message = homeVO.getMessage("home-confirm");

      player.sendMessage(message);
      return;
    }

    homePlayer.removeHome(index);
    homePlugin.getViewFrame()
      .open(
        ListPlayerHomesView.class,
        player,
        Map.of(
          "home_player", getHomePlayer(viewContext)
        )
      );
  }

}
