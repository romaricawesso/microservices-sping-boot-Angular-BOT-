package org.romaric.ebankbot.agents;

import org.romaric.ebankbot.mcp.McpServerConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
public class EbankAIAgent {
    private static final Logger log = LoggerFactory.getLogger(EbankAIAgent.class);

    private static final String SYSTEM_PROMPT = """
            Tu es l'assistant de la banque E-Bank.
            Utilise les outils a ta disposition pour consulter ou creer des clients et des comptes bancaires.
            Verifie toujours les donnees avec les outils avant de repondre, a chaque question :
            les donnees changent et un service indisponible plus tot peut etre revenu.
            Ne reprends jamais une reponse precedente de la conversation sans rappeler les outils.
            N'invente jamais de donnees : si un outil ne renvoie rien, dis-le.
            Reponds en francais.
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final McpServerConnections mcpServers;

    public EbankAIAgent(ChatClient.Builder chatClient, ChatMemory chatMemory, McpServerConnections mcpServers) {
        this.chatMemory = chatMemory;
        this.mcpServers = mcpServers;
        this.chatClient = chatClient
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    // conversationId : une conversation par canal et par utilisateur (ex. "telegram-123", "discord-456", "web-<uuid>"),
    // pour que les historiques ne se melangent pas
    public String chat(String conversationId, String query) {
        // a chaque question, on ne donne au modele que les outils des serveurs MCP joignables
        McpServerConnections.Status status = mcpServers.check();
        try {
            return chatClient.prompt()
                    .system(systemPrompt(status))
                    .user(query)
                    .toolCallbacks(new SyncMcpToolCallbackProvider(status.available()))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .content();
        } catch (Exception e) {
            // un service peut tomber entre la verification et l'appel de l'outil
            log.error("Erreur pendant la reponse du bot", e);
            return "Desole, un service de la banque est momentanement indisponible. Reessayez dans quelques instants.";
        }
    }

    // meme chose que chat(), mais la reponse arrive morceau par morceau, au fur et a mesure que le modele l'ecrit
    public Flux<String> chatStream(String conversationId, String query) {
        McpServerConnections.Status status = mcpServers.check();
        return chatClient.prompt()
                .system(systemPrompt(status))
                .user(query)
                .toolCallbacks(new SyncMcpToolCallbackProvider(status.available()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("Erreur pendant la reponse du bot (stream)", e);
                    return Flux.just("Desole, un service de la banque est momentanement indisponible. Reessayez dans quelques instants.");
                });
    }

    // efface l'historique d'une conversation (commande /reset)
    public void reset(String conversationId) {
        chatMemory.clear(conversationId);
    }

    // Sans cette precision, le modele invente des donnees quand l'outil d'un service arrete est absent
    private String systemPrompt(McpServerConnections.Status status) {
        if (status.unavailable().isEmpty()) {
            return SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT + """

                IMPORTANT : ces services sont actuellement indisponibles : %s.
                Tu n'as pas acces a leurs donnees. Si la question en a besoin, reponds que l'information
                est momentanement indisponible et n'invente aucun chiffre ni aucun compte.
                """.formatted(String.join(", ", status.unavailable()));
    }
}
