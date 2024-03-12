package me.kenvera.chronosync.Command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.chronosync.ChronoSync;
import me.kenvera.chronosync.Object.RedisPlayer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PingCommand {
    public static BrigadierCommand createBrigadierCommand(final ChronoSync plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("ping")
                .requires(src -> src.getPermissionValue("velocity.command.ping") == Tristate.TRUE)
                .executes(ctx -> {
                    CommandSource source = ctx.getSource();
                    if (source instanceof Player) {
                        Player player = (Player) source;
                        long ping = player.getPing();
                        String connection = "unknown";

                        if (player.getVirtualHost().isPresent()) {
                            connection = player.getVirtualHost().get().getHostName();
                        }
                        source.sendMessage(Component.text("\n§a§lPING | §fPlayer §a" + player.getUsername() + " §7<" + ((ping >= 60 && ping <= 100) ? "§e" : (ping > 100) ? "§c" : "§a") + ping + "ms§7>\n§f§lConnection | §a" + connection + "\n"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
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
                            String player = StringArgumentType.getString(ctx, "player");
                            if (source instanceof Player) {
                                Optional<RedisPlayer> redisPlayerOptional = plugin.getSyncManager().getPlayerData(player);
                                if (redisPlayerOptional.isPresent()) {
                                    RedisPlayer redisPlayer = redisPlayerOptional.get();
                                    long ping = redisPlayer.getPing();
                                    String connection = "unknown";

                                    if (!redisPlayer.getVirtualhost().isEmpty()) {
                                        connection = redisPlayer.getVirtualhost();
                                    }
                                    source.sendMessage(Component.text("\n§a§lPING | §fPlayer §a" + redisPlayer.getUsername() + " §7<" + ((ping >= 60 && ping <= 100) ? "§e" : (ping > 100) ? "§c" : "§a") + ping + "ms§7>\n§f§lConnection | §a" + connection + "\n"));
                                } else {
                                    source.sendMessage(Component.text("§cPlayer " + player + " data is not available!"));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
