package org.romaric.ebankbot.telegram;

import jakarta.annotation.PreDestroy;
import org.romaric.ebankbot.agents.EbankAIAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Bot Telegram (long polling) : transmet les messages a EbankAIAgent et renvoie sa reponse.
// En prive il repond a tout ; dans un groupe, Telegram ne lui transmet par defaut que les messages
// qui le mentionnent ou lui repondent (privacy mode).
// Actif seulement si telegram.bot.token est renseigne (sinon l'application demarre sans lui).
@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "token")
public class   TelegramBot implements LongPollingSingleThreadUpdateConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    // limite de Telegram pour un message
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static final String WELCOME = """
            Bonjour, je suis l'assistant de la banque E-Bank.
            Posez-moi une question, par exemple : "Quels sont les comptes du client 1 ?"
            """;

    private final EbankAIAgent ebankAIAgent;
    private final TelegramBotsLongPollingApplication botsApplication;
    private final TelegramClient telegramClient;
    private final String token;
    // le modele peut mettre une minute a repondre : on ne bloque pas la reception des messages
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private String botUsername = "";

    public TelegramBot(EbankAIAgent ebankAIAgent,
                       TelegramBotsLongPollingApplication botsApplication,
                       @Value("${telegram.bot.token}") String token) {
        this.ebankAIAgent = ebankAIAgent;
        this.botsApplication = botsApplication;
        this.token = token;
        this.telegramClient = new OkHttpTelegramClient(token);
    }

    // On n'utilise pas l'enregistrement automatique du starter (SpringLongPollingBot) :
    // avec un token invalide, il ferait echouer le demarrage de tout ebank-bot
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        try {
            // verifie le token aupres de Telegram avant de lancer le long polling
            botUsername = telegramClient.execute(new GetMe()).getUserName();
            botsApplication.registerBot(token, this);
            log.info("Bot Telegram demarre : @{}", botUsername);
        } catch (Exception e) {
            // un token invalide ne doit pas empecher le reste du bot (/chat, Discord) de fonctionner
            log.error("Bot Telegram non demarre, verifiez telegram.bot.token : {}", e.getMessage());
        }
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Message message = update.getMessage();
        // dans un groupe, on retire la mention du bot pour ne garder que la question
        String query = message.getText().replace("@" + botUsername, "").trim();
        if (query.isEmpty()) {
            return;
        }
        if (query.equals("/start")) {
            send(message, WELCOME);
            return;
        }

        sendTyping(message.getChatId());
        executor.submit(() -> send(message, ebankAIAgent.chat(query)));
    }

    private void send(Message question, String answer) {
        for (int start = 0; start < answer.length(); start += MAX_MESSAGE_LENGTH) {
            try {
                telegramClient.execute(SendMessage.builder()
                        .chatId(question.getChatId())
                        .replyToMessageId(question.getMessageId())
                        .text(answer.substring(start, Math.min(answer.length(), start + MAX_MESSAGE_LENGTH)))
                        .build());
            } catch (TelegramApiException e) {
                log.error("Envoi du message Telegram impossible : {}", e.getMessage());
                return;
            }
        }
    }

    private void sendTyping(Long chatId) {
        try {
            telegramClient.execute(SendChatAction.builder().chatId(chatId).action("typing").build());
        } catch (TelegramApiException e) {
            log.warn("Indicateur de saisie Telegram impossible : {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}
