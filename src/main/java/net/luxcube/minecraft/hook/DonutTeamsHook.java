package net.luxcube.minecraft.hook;

import net.luxcube.minecraft.TeamsPlugin;
import net.luxcube.minecraft.entity.member.TeamMember;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DonutTeamsHook {

  public boolean hasTeam(@NotNull Player player) {
    TeamMember teamMember = TeamsPlugin.getInstance()
      .getTeamService()
      .getTeamMember(player.getUniqueId());

    if (teamMember == null)
      return false;

    return teamMember.getTeamName() != null;
  }


}
