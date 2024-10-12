package net.luxcube.minecraft.view;

import me.saiintbrisson.minecraft.PaginatedView;
import me.saiintbrisson.minecraft.PaginatedViewSlotContext;
import me.saiintbrisson.minecraft.ViewContext;
import me.saiintbrisson.minecraft.ViewItem;
import net.luxcube.minecraft.HomePlugin;
import net.luxcube.minecraft.entity.HomePlayer;
import net.luxcube.minecraft.entity.home.Home;
import net.luxcube.minecraft.util.ItemBuilder;
import net.luxcube.minecraft.util.ViewUtils;
import net.luxcube.minecraft.vo.HomeVO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.luxcube.minecraft.util.Colors.translateHex;

public class ListPlayerHomesView extends PaginatedView<Home> {

  private final HomePlugin homePlugin;
  private final HomeVO.InventoryInfo inventoryInfo;

  public ListPlayerHomesView(@NotNull HomePlugin homePlugin, @NotNull HomeVO.InventoryInfo inventoryInfo) {
    super(inventoryInfo.getRows(), translateHex(inventoryInfo.getTitle()));

    ViewUtils.cancelAllDefaultActions(this);

    this.homePlugin = homePlugin;
    this.inventoryInfo = inventoryInfo;

    setLayout(inventoryInfo.getLayout());

    for (int homeSlot : (java.util.List<Integer>) inventoryInfo.getExtra().get("home-slots")) {
      slot(homeSlot)
              .onUpdate(update -> update.setItem(buildDefaultHomeSlot(update, homeSlot)))
              .onRender(render -> render.setItem(buildDefaultHomeSlot(render, homeSlot)))
              .onClick(click -> clickHomeDefault(click, homeSlot));
    }

    for (int dyeSlot : (java.util.List<Integer>) inventoryInfo.getExtra().get("dye-slots")) {
      slot(dyeSlot)
              .onUpdate(update -> update.setItem(buildDefaultDyeSlot(update, dyeSlot)))
              .onRender(render -> render.setItem(buildDefaultDyeSlot(render, dyeSlot)))
              .onClick(click -> clickDeleteHomeDefault(click, dyeSlot));
    }

    if (homePlugin.isHasDonutTeams()) {
      // Assuming TEAM_HOME_SLOT and TEAM_HOME_DYE_SLOT are in extra
      if (inventoryInfo.getExtra().containsKey("team-home-slot")) {
        slot((int) inventoryInfo.getExtra().get("team-home-slot"))
                .onUpdate(update -> update.setItem(buildTeamHomeSlot(update)))
                .onRender(render -> render.setItem(buildTeamHomeSlot(render)))
                .onClick(this::clickTeamDefault);
      }

      if (inventoryInfo.getExtra().containsKey("team-home-dye-slot")) {
        slot((int) inventoryInfo.getExtra().get("team-home-dye-slot"))
                .onUpdate(update -> update.setItem(buildTeamDyeSlot(update)))
                .onRender(render -> render.setItem(buildTeamDyeSlot(render)))
                .onClick(this::clickTeamDefault);
      }
    }

    // Dynamically set previous and next page items using inventoryInfo icons
    setPreviousPageItem((paginatedViewContext, viewItem) -> {
      ItemStack previousPageItem = inventoryInfo.getIcons().get("previous-page").getItemStack();
      viewItem.onRender(render -> render.setItem(paginatedViewContext.hasPreviousPage() ? previousPageItem : null))
              .onClick(click -> paginatedViewContext.switchToPreviousPage());
    });

    setNextPageItem((paginatedViewContext, viewItem) -> {
      ItemStack nextPageItem = inventoryInfo.getIcons().get("next-page").getItemStack();
      viewItem.onRender(render -> render.setItem(paginatedViewContext.hasNextPage() ? nextPageItem : null))
              .onClick(click -> paginatedViewContext.switchToNextPage());
    });
  }

  @Override
  protected void onItemRender(
          @NotNull PaginatedViewSlotContext<Home> paginatedViewSlotContext,
          @NotNull ViewItem viewItem,
          @NotNull Home home
  ) {
    int index = (int) paginatedViewSlotContext.getIndex();

    HomePlayer homePlayer = getHomePlayer(paginatedViewSlotContext);
    if (homePlayer == null) {
      viewItem.withItem(inventoryInfo.getIcons().get("no-home-set").getItemStack())
              .onClick(click -> clickToCreate(paginatedViewSlotContext, index));
      return;
    }

    if (homePlayer.getHome(index) == null) {
      viewItem.withItem(inventoryInfo.getIcons().get("no-home-set").getItemStack())
              .onClick(click -> clickToCreate(paginatedViewSlotContext, index));
      return;
    }

    viewItem.withItem(buildIndexedHome(index))
            .onUpdate(update -> update.setItem(buildIndexedHome(index)))
            .onClick(click -> {
              Player player = paginatedViewSlotContext.getPlayer();
              player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
              Bukkit.dispatchCommand(player, "home " + index);
            }).closeOnClick();
  }

  @Override
  protected void onRender(@NotNull ViewContext context) {
    HomePlayer homePlayer = getHomePlayer(context);
    if (homePlayer == null) {
      return;
    }

    context.paginated().setSource(homePlayer.getHomes());
  }

  private HomePlayer getHomePlayer(@NotNull ViewContext viewContext) {
    return viewContext.get("home_player");
  }

  private void clickToCreate(@NotNull ViewContext viewContext, int index) {
    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    Bukkit.dispatchCommand(player, "sethome " + index);
    viewContext.update();
  }

  private void clickTeamDefault(@NotNull ViewContext viewContext) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return;
    }

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    Home home = homePlayer.getTeamHome();
    if (home == null) {
      Bukkit.dispatchCommand(player, "team sethome");
    } else {
      Bukkit.dispatchCommand(player, "team home");
    }

    viewContext.update();
  }

  private void clickHomeDefault(@NotNull ViewContext viewContext, int slot) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return;
    }

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    if (homePlayer.isFull(homePlugin)) {
      return;
    }

    int index = slot - 12;
    String permission = homePlugin.getHomePermission(index);
    if (permission != null && !player.hasPermission(permission)) {
      return;
    }

    Bukkit.dispatchCommand(viewContext.getPlayer(), "sethome " + index);
  }

  private void clickDeleteHomeDefault(@NotNull ViewContext viewContext, int slot) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return;
    }

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    int index = slot - 21;
    if (homePlayer.getHome(index) == null) {
      if (homePlayer.isFull(homePlugin)) {
        return;
      }

      String permission = homePlugin.getHomePermission(index);
      if (permission != null && !player.hasPermission(permission)) {
        return;
      }

      Bukkit.dispatchCommand(viewContext.getPlayer(), "sethome " + index);
      viewContext.update();
      return;
    }

    viewContext.open(
            ConfirmHomeDeleteView.class,
            Map.of(
                    "home_player", homePlayer,
                    "home_index", index
            )
    );
  }

  private ItemStack buildDefaultDyeSlot(@NotNull ViewContext viewContext, int slot) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return inventoryInfo.getIcons().get("no-home-dye").getItemStack();
    }

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    int index = slot - 21;
    if (homePlayer.isFull(homePlugin)) {
      String permission = homePlugin.getHomePermission(index);
      if (permission != null && !player.hasPermission(permission)) {
        return inventoryInfo.getIcons().get("no-permission-dye").getItemStack();
      }
    }

    return homePlayer.getHome(index) == null ? inventoryInfo.getIcons().get("no-home-dye").getItemStack() : buildDyeHome(index);
  }

  private ItemStack buildDefaultHomeSlot(@NotNull ViewContext viewContext, int slot) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return inventoryInfo.getIcons().get("no-home-set").getItemStack();
    }

    Player player = viewContext.getPlayer();
    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

    int index = slot - 12;
    if (homePlayer.isFull(homePlugin)) {
      String permission = homePlugin.getHomePermission(index);
      if (permission != null && !player.hasPermission(permission)) {
        return inventoryInfo.getIcons().get("no-permission-home").getItemStack();
      }
    }

    return homePlayer.getHome(index) == null ? inventoryInfo.getIcons().get("no-home-set").getItemStack() : buildIndexedHome(index);
  }

  private ItemStack buildTeamHomeSlot(@NotNull ViewContext viewContext) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return inventoryInfo.getIcons().get("no-team-home").getItemStack();
    }

    if (!homePlugin.getDonutTeamsHook().hasTeam(viewContext.getPlayer())) {
      return inventoryInfo.getIcons().get("no-team-home").getItemStack();
    }

    Home home = homePlayer.getTeamHome();
    if (home == null) {
      return inventoryInfo.getIcons().get("team-no-home").getItemStack();
    }

    return inventoryInfo.getIcons().get("team-home").getItemStack();
  }

  private ItemStack buildTeamDyeSlot(@NotNull ViewContext viewContext) {
    HomePlayer homePlayer = getHomePlayer(viewContext);
    if (homePlayer == null) {
      return inventoryInfo.getIcons().get("no-team-home-dye").getItemStack();
    }

    if (!homePlugin.getDonutTeamsHook().hasTeam(viewContext.getPlayer())) {
      return inventoryInfo.getIcons().get("no-team-home-dye").getItemStack();
    }

    Home home = homePlayer.getTeamHome();
    if (home == null) {
      return inventoryInfo.getIcons().get("team-no-home-dye").getItemStack();
    }

    return inventoryInfo.getIcons().get("team-home-dye").getItemStack();
  }

  private ItemStack buildIndexedHome(int index) {
    return new ItemBuilder(inventoryInfo.getIcons().get("indexed-home").getItemStack().clone())
            .placeholders(Map.of("%index%", String.valueOf((index + 1))))
            .result();
  }

  private ItemStack buildDyeHome(int index) {
    return new ItemBuilder(inventoryInfo.getIcons().get("dye-home").getItemStack().clone())
            .placeholders(Map.of("%index%", String.valueOf((index + 1))))
            .result();
  }
}
