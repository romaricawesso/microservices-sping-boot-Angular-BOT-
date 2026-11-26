package org.romaric.ebankbot.discord;

import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.romaric.ebankbot.agents.EbankAIAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Bot Discord : transmet les messages a EbankAIAgent et renvoie sa reponse.
// Repond aux messages prives et aux messages d'un salon qui mentionnent le bot.
// Actif seulement si discord.bot.token est renseigne (sinon l'application demarre sans lui).
@Component
@ConditionalOnProperty(prefix = "discord.bot", name = "token")
public class DiscordBot extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);

    // limite de Discord pour un message
    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final EbankAIAgent ebankAIAgent;
    private final String token;
    // le modele peut mettre une minute a repondre : on ne bloque pas le thread d'evenements de JDA
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private JDA jda;

    public DiscordBot(EbankAIAgent ebankAIAgent, @Value("${discord.bot.token}") String token) {
        this.ebankAIAgent = ebankAIAgent;
        this.token = token;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        // MESSAGE_CONTENT doit aussi etre active dans le Developer Portal (Bot > Privileged Gateway Intents)
        try {
            jda = JDABuilder.createLight(token, EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT))
                    .addEventListeners(this)
                    .build();
            log.info("Bot Discord demarre");
        } catch (Exception e) {
            // un token invalide ne doit pas empecher le reste du bot (/chat) de fonctionner
            log.error("Bot Discord non demarre, verifiez discord.bot.token : {}", e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        Message message = event.getMessage();
        boolean isPrivate = event.isFromType(ChannelType.PRIVATE);
        boolean isMentioned = message.getMentions().isMentioned(event.getJDA().getSelfUser());
        if (!isPrivate && !isMentioned) {
            return;
        }
        // retire la mention du bot pour ne garder que la question
        String query = message.getContentRaw()
                .replaceAll("<@!?" + event.getJDA().getSelfUser().getId() + ">", "")
                .trim();
        if (query.isEmpty()) {
            return;
        }

        event.getChannel().sendTyping().queue();
        executor.submit(() -> {
            String answer = ebankAIAgent.chat(query);
            for (int start = 0; start < answer.length(); start += MAX_MESSAGE_LENGTH) {
                message.reply(answer.substring(start, Math.min(answer.length(), start + MAX_MESSAGE_LENGTH))).queue();
            }
        });
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
        if (jda != null) {
            jda.shutdown();
        }
    }
}
