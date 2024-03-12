package me.kenvera.chronosync.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;
import me.kenvera.chronosync.Object.RedisPlayer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class FindCommand {
    public static BrigadierCommand createBrigadierCommand(final ChronoSync plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("find")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            List<String> suggestions = new ArrayList<>();
                            Player player = (Player) ctx.getSource();

                            plugin.getSyncManager().getPlayerDataCache()
                                    .stream()
                                    .map(RedisPlayer::getUsername)
                                    .filter(username -> !username.equalsIgnoreCase(player.getUsername()))
                                    .filter(username -> username.toLowerCase().startsWith(inputPart))
                                    .forEach(suggestions::add);

                            for (String suggestion : suggestions) {
                                if (inputParts.length == 3) {
                                    String input = inputParts[2];
                                    if (suggestion.toLowerCase().startsWith(input)) {
                                        builder.suggest(suggestion);
                                    }
                                } else if (inputParts.length <= 3) {
                                    builder.suggest(suggestion);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            String playerArg = StringArgumentType.getString(ctx, "player");

                            if (source instanceof Player || source instanceof ConsoleCommandSource) {
                                Optional<RedisPlayer> redisPlayerOptional = plugin.getSyncManager().getPlayerData(playerArg);
                                if (redisPlayerOptional.isPresent()) {
                                    RedisPlayer redisPlayer = redisPlayerOptional.get();
                                    long onlineSession = System.currentTimeMillis() - redisPlayer.getJoinTime();
                                    long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(onlineSession);
                                    long hours = totalMinutes / 60;
                                    long minutes = totalMinutes % 60;
                                    String formattedSession = "";

                                    if (hours > 0) {
                                        formattedSession += hours + "h ";
                                    }
                                    formattedSession += minutes + "m";

                                    source.sendMessage(Component.text("\n§aPlayer §f" + redisPlayer.getUsername() + " §ais online on §f" + redisPlayer.getServer() + " §7" + formattedSession));
                                    source.sendMessage(Component.text("§aUUID: §f" + redisPlayer.getUuid()));
                                    source.sendMessage(Component.text("§aIP: §f" + redisPlayer.getRemoteAddress() + " §7" + redisPlayer.getPing() + "ms"));
                                    source.sendMessage(Component.text("§aProxy: §f" + redisPlayer.getProxy()));
                                    source.sendMessage(Component.text("§aVHost: §f" + redisPlayer.getVirtualhost()));
                                    source.sendMessage(Component.text("§aOnline Mode: §f" + redisPlayer.isOnlineMode()));
                                    source.sendMessage(Component.text("§aClient Version: §f" + redisPlayer.getProtocolVersion()));
                                    source.sendMessage(Component.text("§aClient Brand: §f" + redisPlayer.getClientBrand()));
                                    source.sendMessage(Component.text("§aInherited Group: §f" + redisPlayer.getInheritedGroups().toString().substring(2, redisPlayer.getInheritedGroups().toString().length() - 2) + "\n"));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
