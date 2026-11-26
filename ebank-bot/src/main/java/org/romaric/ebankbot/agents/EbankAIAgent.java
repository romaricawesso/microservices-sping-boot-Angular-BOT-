package org.romaric.ebankbot.agents;

import org.romaric.ebankbot.mcp.McpServerConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.stereotype.Service;


@Service
public class EbankAIAgent {
    private static final Logger log = LoggerFactory.getLogger(EbankAIAgent.class);

    // Depuis Spring AI 2.1, la memoire n'a plus de conversation par defaut : l'identifiant doit etre fourni
    private static final String DEFAULT_CONVERSATION_ID = "default";

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
    private final McpServerConnections mcpServers;

    public EbankAIAgent(ChatClient.Builder chatClient, ChatMemory chatMemory, McpServerConnections mcpServers) {
        this.mcpServers = mcpServers;
        this.chatClient = chatClient
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public String chat(String query) {
        // a chaque question, on ne donne au modele que les outils des serveurs MCP joignables
        McpServerConnections.Status status = mcpServers.check();
        try {
            return chatClient.prompt()
                    .system(systemPrompt(status))
                    .user(query)
                    .toolCallbacks(new SyncMcpToolCallbackProvider(status.available()))
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, DEFAULT_CONVERSATION_ID))
                    .call()
                    .content();
        } catch (Exception e) {
            // un service peut tomber entre la verification et l'appel de l'outil
            log.error("Erreur pendant la reponse du bot", e);
            return "Desole, un service de la banque est momentanement indisponible. Reessayez dans quelques instants.";
        }
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
