# E-Bank – Architecture microservices Spring Boot + Chatbot IA

Application bancaire construite en microservices avec **Spring Boot 4**, **Spring Cloud 2026** et **Spring AI** (chatbot propulsé par Ollama).

## Architecture

```
                    ┌──────────────────────┐
                    │  discovery-service   │  Eureka – :8761
                    └──────────┬───────────┘
                               │ enregistrement
     ┌─────────────────────────┼──────────────────────────┐
     │                         │                          │
┌────┴────────────┐   ┌────────┴─────────┐   ┌────────────┴─────┐
│ gateway-service │──▶│ customer-service │◀──│  ebank-service   │
│     :8080       │   │      :8086       │   │      :8087       │
└────────┬────────┘   └──────────────────┘   └──────────────────┘
         │                                     OpenFeign + Resilience4j
         └──────────────────────────────────────────▶ ebank-service

┌──────────────────┐
│    ebank-bot     │  Spring AI + Ollama – :8088
└──────────────────┘  client MCP ──▶ customer-service /mcp + ebank-service /mcp
```

| Module | Port | Rôle |
|---|---|---|
| `discovery-service` | 8761 | Annuaire de services (Eureka Server) |
| `gateway-service` | 8080 | Point d'entrée unique (Spring Cloud Gateway), routage `lb://` via Eureka |
| `customer-service` | 8086 | Gestion des clients (JPA + H2 en mémoire) |
| `ebank-service` | 8087 | Gestion des comptes bancaires, appelle `customer-service` via OpenFeign avec circuit breaker Resilience4j |
| `ebank-bot` | 8088 | Chatbot bancaire (Spring AI, Ollama, mémoire de conversation) |

## Prérequis

- Java 21
- Maven (ou les wrappers `mvnw` fournis dans chaque module)
- [Ollama](https://ollama.com) en local pour `ebank-bot` (`ollama signin` si vous utilisez un modèle cloud, ou `ollama pull llama3.1:8b` pour un modèle local)

## Configuration

Les fichiers `application.properties` et `application.yaml` ne sont **pas versionnés**. Chaque module fournit un fichier `.example` à copier :

```bash
for d in discovery-service gateway-service customer-service ebank-service ebank-bot; do
  cp $d/src/main/resources/application.properties.example $d/src/main/resources/application.properties
done
# routes de la gateway
cp gateway-service/src/main/resources/application.yaml.example gateway-service/src/main/resources/application.yaml
```

Adaptez ensuite les valeurs si besoin (ports, URL Eureka, modèle Ollama…).

## Lancement

Démarrer les services **dans cet ordre** (un terminal par service) :

```bash
cd discovery-service && ./mvnw spring-boot:run
cd customer-service  && ./mvnw spring-boot:run
cd ebank-service     && ./mvnw spring-boot:run
cd gateway-service   && ./mvnw spring-boot:run
cd ebank-bot         && ./mvnw spring-boot:run
```

Tableau de bord Eureka : http://localhost:8761

## API

Via la gateway (`http://localhost:8080`) :

### Clients – `/customers`

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/customers` | Liste des clients |
| GET | `/customers/{id}` | Détail d'un client |
| POST | `/customers` | Créer un client |
| PUT | `/customers/{id}` | Modifier un client |
| DELETE | `/customers/{id}` | Supprimer un client |

### Comptes – `/accounts`

| Méthode | Endpoint | Description |
|---|---|---|
| GET | `/accounts` | Liste des comptes |
| GET | `/accounts/{id}` | Détail d'un compte |
| GET | `/accounts/customer/{customerId}` | Comptes d'un client |
| POST | `/accounts` | Créer un compte |
| PUT | `/accounts/{id}` | Modifier un compte |
| DELETE | `/accounts/{id}` | Supprimer un compte |

### Chatbot

```
GET http://localhost:8088/chat?query=Bonjour
```

## MCP (Model Context Protocol)

`customer-service` et `ebank-service` sont aussi des **serveurs MCP** (Spring AI, transport Streamable HTTP) :
leurs opérations sont exposées comme outils pour les agents IA. `ebank-bot` est **client MCP** : le chatbot
découvre ces outils au démarrage et les appelle pour répondre avec les vraies données de la banque.

| Serveur | Endpoint | Outils |
|---|---|---|
| customer-service | `http://localhost:8086/mcp` | `getAllCustomers`, `getCustomerById`, `createCustomer` |
| ebank-service | `http://localhost:8087/mcp` | `getAllBankAccounts`, `getBankAccountById`, `getBankAccountsByCustomerId`, `createBankAccount` |

Les outils se limitent volontairement à la lecture et à la création (pas de modification ni de suppression par le LLM).
`customer-service` et `ebank-service` doivent être démarrés **avant** `ebank-bot`, sinon le client MCP ne peut pas s'y connecter.

Exemple : `GET http://localhost:8088/chat?query=Quels sont les comptes du client 1 ?`

Les serveurs peuvent aussi être testés avec [MCP Inspector](https://github.com/modelcontextprotocol/inspector) (`npx @modelcontextprotocol/inspector`, transport *Streamable HTTP*).

## Résilience

`ebank-service` protège ses appels vers `customer-service` avec un circuit breaker Resilience4j :
le circuit s'ouvre si 50 % des 10 derniers appels échouent, reste ouvert 10 s, et un timeout de 3 s déclenche le fallback.
Une réponse 404 (client introuvable) n'est pas comptée comme une panne.

## Console H2

- customer-service : http://localhost:8086/h2-console (JDBC URL `jdbc:h2:mem:customers-db`)
- ebank-service : http://localhost:8087/h2-console (JDBC URL `jdbc:h2:mem:accounts-db`)
