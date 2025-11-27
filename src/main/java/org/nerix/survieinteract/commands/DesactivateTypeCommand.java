package org.nerix.survieinteract.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DesactivateTypeCommand {

    // Liste unique d’events valides
    private static final List<String> VALID_EVENTS =
            List.of("follow", "sub", "bits", "raid", "points");


    // Instance utilisable comme ArgumentType<String>
    private static final ArgumentType<String> VALID = new EventTypeArgument();

    // ======== COMMANDE ========
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
                literal("deactivate")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(argument("event", VALID)
                                .executes(ctx -> {
                                    String event = ctx.getArgument("event", String.class);

                                    ConfigManager.setEventEnabled(event, false);

                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("L'event '" + event + "' est maintenant désactivé."),
                                            true
                                    );

                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                literal("activate")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(argument("event", VALID)
                                .executes(ctx -> {
                                    String event = ctx.getArgument("event", String.class);

                                    ConfigManager.setEventEnabled(event, true);

                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("L'event '" + event + "' est maintenant activé."),
                                            true
                                    );

                                    return 1;
                                })
                        )
        );
    }




    // ======== ARGUMENT TYPE ========
    public static class EventTypeArgument implements ArgumentType<String> {

        @Override
        public String parse(com.mojang.brigadier.StringReader reader) throws CommandSyntaxException {
            String value = reader.readUnquotedString().toLowerCase();
            if (!VALID_EVENTS.contains(value)) {
                throw new SimpleCommandExceptionType(Text.literal("Type d'event invalide : " + value)).create();
            }
            return value;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(
                CommandContext<S> context,
                SuggestionsBuilder builder) {

            return CommandSource.suggestMatching(VALID_EVENTS, builder);
        }

        @Override
        public Collection<String> getExamples() {
            return VALID_EVENTS;
        }
    }

}
