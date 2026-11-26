package org.romaric.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;


@Service
public class EbankAIAgent {

    private final ChatClient chatClient;

    // toolCallbackProvider : outils MCP decouverts aupres de customer-service et ebank-service
    public EbankAIAgent(ChatClient.Builder chatClient, ChatMemory chatMemory, ToolCallbackProvider toolCallbackProvider) {
        this.chatClient = chatClient
                .defaultSystem("""
                        Tu es l'assistant de la banque E-Bank.
                        Utilise les outils a ta disposition pour consulter ou creer des clients et des comptes bancaires.
                        N'invente jamais de donnees : si un outil ne renvoie rien, dis-le.
                        Reponds en francais.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    // Depuis Spring AI 2.1, la memoire n'a plus de conversation par defaut : l'identifiant doit etre fourni
    private static final String DEFAULT_CONVERSATION_ID = "default";

    public String chat(String query) {
        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, DEFAULT_CONVERSATION_ID))
                .call()
                .content();
    }
}
