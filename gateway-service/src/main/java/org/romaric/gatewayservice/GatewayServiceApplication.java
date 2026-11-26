package org.romaric.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    // Routage dynamique : une route est creee pour chaque service enregistre dans Eureka
    // ex : http://localhost:8080/CUSTOMER-SERVICE/customers
    @Bean
    DiscoveryClientRouteDefinitionLocator dynamicRoutes(ReactiveDiscoveryClient discoveryClient,
                                                        DiscoveryLocatorProperties properties) {
        return new DiscoveryClientRouteDefinitionLocator(discoveryClient, properties);
    }

}
