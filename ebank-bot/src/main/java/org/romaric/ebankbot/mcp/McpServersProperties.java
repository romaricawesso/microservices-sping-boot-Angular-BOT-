package org.romaric.ebankbot.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

// Serveurs MCP utilises par le bot, ex : ebank.mcp.servers.customer-service=http://localhost:8086
@ConfigurationProperties(prefix = "ebank.mcp")
public record McpServersProperties(Map<String, String> servers, Duration requestTimeout) {

    public McpServersProperties {
        servers = servers == null ? Map.of() : servers;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
    }
}
