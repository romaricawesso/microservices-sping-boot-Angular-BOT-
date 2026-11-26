package org.romaric.ebankbot.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Connexions aux serveurs MCP (customer-service, ebank-service), ouvertes a la demande :
// - le bot demarre meme si un service est arrete
// - un serveur qui ne repond plus est ignore, et sa connexion est recreee quand il revient
//   (un client MCP dont la session a ete coupee n'est pas reutilisable)
@Component
@EnableConfigurationProperties(McpServersProperties.class)
public class McpServerConnections {
    private static final Logger log = LoggerFactory.getLogger(McpServerConnections.class);

    public record Status(List<McpSyncClient> available, List<String> unavailable) {
    }

    private final McpServersProperties properties;
    private final Map<String, McpSyncClient> clients = new HashMap<>();

    public McpServerConnections(McpServersProperties properties) {
        this.properties = properties;
    }

    public synchronized Status check() {
        List<McpSyncClient> available = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        properties.servers().forEach((name, url) -> {
            McpSyncClient client = clients.get(name);
            try {
                if (client == null) {
                    client = create(url);
                    client.initialize();
                    clients.put(name, client);
                    log.info("Connecte au serveur MCP {} ({})", name, url);
                } else {
                    client.ping();
                }
                available.add(client);
            } catch (Exception e) {
                log.warn("Serveur MCP {} indisponible ({}), ses outils sont ignores : {}", name, url, e.getMessage());
                clients.remove(name);
                closeQuietly(client);
                unavailable.add(name);
            }
        });
        return new Status(available, unavailable);
    }

    private McpSyncClient create(String url) {
        return McpClient.sync(HttpClientStreamableHttpTransport.builder(url).build())
                .clientInfo(new McpSchema.Implementation("ebank-bot", "1.0.0"))
                .requestTimeout(properties.requestTimeout())
                .initializationTimeout(properties.requestTimeout())
                .build();
    }

    private void closeQuietly(McpSyncClient client) {
        if (client == null) return;
        try {
            client.close();
        } catch (Exception ignored) {
            // connexion deja fermee
        }
    }

    @PreDestroy
    public synchronized void closeAll() {
        clients.values().forEach(this::closeQuietly);
        clients.clear();
    }
}
