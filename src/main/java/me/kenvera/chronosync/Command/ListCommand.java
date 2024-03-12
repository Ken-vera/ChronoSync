package me.kenvera.chronosync.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import me.kenvera.chronosync.ChronoSync;
import me.kenvera.chronosync.Object.RedisPlayer;
import net.kyori.adventure.text.Component;

import java.util.*;

public final class ListCommand {
    public static BrigadierCommand createBrigadierCommand(final ChronoSync plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("vlist")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .executes(ctx -> {
                    CommandSource source = ctx.getSource();
                    Set<RedisPlayer> redisPlayers = plugin.getSyncManager().getPlayerDataCache();
                    Map<String, List<RedisPlayer>> serverMap = new HashMap<>();
                    for (RedisPlayer redisPlayer : redisPlayers) {
                        if (serverMap.containsKey(redisPlayer.getServer())) {
                            serverMap.get(redisPlayer.getServer()).add(redisPlayer);
                        } else {
                            serverMap.put(redisPlayer.getServer(), new ArrayList<>());
                            serverMap.get(redisPlayer.getServer()).add(redisPlayer);
                        }
                    }

                    source.sendMessage(Component.text("\n§aOnline " + (redisPlayers.size() == 1 ? "Player : §f" : "Players : §f"))
                            .append(Component.text(redisPlayers.size())));
                    for (String server : serverMap.keySet()) {
                        source.sendMessage(Component.text(" §a" + server + " : §f" + serverMap.get(server).size() + (serverMap.get(server).size() == 1 ? " player" : " players")));
                    }

                    source.sendMessage(Component.text(""));

                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("all");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            String subCommand = StringArgumentType.getString(ctx, "subcommand");

                            if (subCommand.equalsIgnoreCase("all")) {
                                Set<RedisPlayer> redisPlayers = plugin.getSyncManager().getPlayerDataCache();
                                Map<String, List<RedisPlayer>> serverMap = new HashMap<>();
                                for (RedisPlayer redisPlayer : redisPlayers) {
                                    if (serverMap.containsKey(redisPlayer.getServer())) {
                                        serverMap.get(redisPlayer.getServer()).add(redisPlayer);
                                    } else {
                                        serverMap.put(redisPlayer.getServer(), new ArrayList<>());
                                        serverMap.get(redisPlayer.getServer()).add(redisPlayer);
                                    }
                                }

                                source.sendMessage(Component.text("\n§aOnline " + (redisPlayers.size() == 1 ? "Player : §f" : "Players : §f"))
                                        .append(Component.text(redisPlayers.size())));
                                for (String server : serverMap.keySet()) {
                                    List<String> playerList = new ArrayList<>();
                                    for (RedisPlayer redisPlayer : serverMap.get(server)) {
                                        playerList.add(redisPlayer.getUsername().replaceAll("players::", ""));
                                    }
                                    source.sendMessage(Component.text(" §a" + server + " §7(" + serverMap.get(server).size() + (serverMap.get(server).size() == 1 ? " player) : §f" : " players) : §f") + playerList.toString().substring(1, playerList.toString().length() - 1)));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
