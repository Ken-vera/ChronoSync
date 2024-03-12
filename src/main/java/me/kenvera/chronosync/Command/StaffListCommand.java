package me.kenvera.chronosync.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;
import me.kenvera.chronosync.Object.RedisPlayer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class StaffListCommand {
    public static BrigadierCommand createBrigadierCommand(final ChronoSync plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("stafflist")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .executes(ctx -> {
                    CommandSource source = ctx.getSource();
                    Set<RedisPlayer> redisPlayers = plugin.getSyncManager().getPlayerDataCache();
                    List<RedisPlayer> staffPlayers = redisPlayers.stream()
                            .filter(redisPlayer -> {
                                Player player = redisPlayer.getPlayer().get();
                                return player != null && player.hasPermission("velocity.staff");
                            })
                            .collect(Collectors.toList());

                    source.sendMessage(Component.text("\n§6Online Staff[s]: ")
                            .append(Component.text(staffPlayers.size())));

                    for (RedisPlayer staffMember : staffPlayers) {
                        UUID uuid = UUID.fromString(staffMember.getUuid());
                        User user = plugin.getLuckPerms().getUserManager().getUser(uuid);
                        long onlineTime = staffMember.getJoinTime();
                        long currentTime = System.currentTimeMillis();

                        long onlineSession = currentTime - onlineTime;
                        long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(onlineSession);
                        long hours = totalMinutes / 60;
                        long minutes = totalMinutes % 60;
                        String formattedTime = "";

                        if (hours > 0) {
                            formattedTime += hours + "h ";
                        }
                        formattedTime += minutes + "m";

                        assert user != null;
                        CachedMetaData metaData = user.getCachedData().getMetaData();
                        String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");

                        source.sendMessage(Component.text(prefix + " ")
                                .append(Component.text(staffMember.getUsername()))
                                .append(Component.text(" §7- "))
                                .append(Component.text("§7" + staffMember.getServer()))
                                .append(Component.text("§7 " + formattedTime)));
                    }
                    source.sendMessage(Component.text(""));

                    return Command.SINGLE_SUCCESS;
                })
                .build();
        return new BrigadierCommand(node);
    }
}

