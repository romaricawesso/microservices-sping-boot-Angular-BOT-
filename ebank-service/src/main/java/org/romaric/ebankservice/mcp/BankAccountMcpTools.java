package org.romaric.ebankservice.mcp;

import org.romaric.ebankservice.entities.BankAccount;
import org.romaric.ebankservice.services.EbankService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

// Outils exposes aux agents IA via le serveur MCP (endpoint /mcp)
// Volontairement limite a la lecture et a la creation : pas de suppression par un LLM
@Component
public class BankAccountMcpTools {

    private final EbankService ebankService;

    public BankAccountMcpTools(EbankService ebankService) {
        this.ebankService = ebankService;
    }

    @McpTool(name = "getAllBankAccounts", description = "Retourne tous les comptes bancaires avec leur solde, leur type et leur client")
    public List<BankAccount> getAllBankAccounts() {
        return ebankService.getAllBankAccounts();
    }

    @McpTool(name = "getBankAccountById", description = "Retourne un compte bancaire a partir de son identifiant (UUID)")
    public BankAccount getBankAccountById(
            @McpToolParam(description = "Identifiant du compte (UUID)", required = true) String id) {
        return ebankService.getBankAccountById(id);
    }

    @McpTool(name = "getBankAccountsByCustomerId", description = "Retourne les comptes bancaires d'un client")
    public List<BankAccount> getBankAccountsByCustomerId(
            @McpToolParam(description = "Identifiant du client", required = true) long customerId) {
        return ebankService.getBankAccountsByCustomerId(customerId);
    }

    @McpTool(name = "createBankAccount", description = "Ouvre un nouveau compte bancaire pour un client existant")
    public BankAccount createBankAccount(
            @McpToolParam(description = "Identifiant du client titulaire", required = true) long customerId,
            @McpToolParam(description = "Type de compte : CURRENT, SAVING ou BUSINESS", required = true) String type,
            @McpToolParam(description = "Solde initial, positif ou nul", required = true) double balance) {
        if (!type.matches("CURRENT|SAVING|BUSINESS")) {
            throw new IllegalArgumentException("le type doit etre CURRENT, SAVING ou BUSINESS");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("le solde ne peut pas etre negatif");
        }
        return ebankService.save(BankAccount.builder()
                .customerId(customerId)
                .type(type)
                .balance(balance)
                .build());
    }
}
