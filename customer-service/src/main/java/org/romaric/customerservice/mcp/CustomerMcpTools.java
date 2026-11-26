package org.romaric.customerservice.mcp;

import org.romaric.customerservice.entities.Customer;
import org.romaric.customerservice.service.CustomerService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

// Outils exposes aux agents IA via le serveur MCP (endpoint /mcp)
// Volontairement limite a la lecture et a la creation : pas de suppression par un LLM

@Component
public class CustomerMcpTools {

    private final CustomerService customerService;

    public CustomerMcpTools(CustomerService customerService) {
        this.customerService = customerService;
    }

    @McpTool(name = "getAllCustomers", description = "Retourne la liste de tous les clients de la banque (id, nom, email)")
    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomer();
    }

    @McpTool(name = "getCustomerById", description = "Retourne un client a partir de son identifiant")
    public Customer getCustomerById(
            @McpToolParam(description = "Identifiant du client", required = true) Long id) {
        return customerService.findCustomerById(id);
    }

    @McpTool(name = "createCustomer", description = "Cree un nouveau client et retourne le client cree avec son identifiant")
    public Customer createCustomer(
            @McpToolParam(description = "Nom complet du client", required = true) String name,
            @McpToolParam(description = "Adresse email du client", required = true) String email) {
        return customerService.saveCustomer(Customer.builder().name(name).email(email).build());
    }
}
