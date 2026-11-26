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
- [Ollama](https://ollama.com) en local pour `ebank-bot`, avec le modèle `qwen2.5:7b` : `ollama pull qwen2.5:7b`
  (vérifier avec `ollama list`)

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

Démarrer les services **dans cet ordre** (un terminal par service, ou la classe `*Application` de chaque module avec ▶ dans IntelliJ) :

```bash
cd discovery-service && ./mvnw spring-boot:run   # 8761
cd customer-service  && ./mvnw spring-boot:run   # 8086
cd ebank-service     && ./mvnw spring-boot:run   # 8087
cd gateway-service   && ./mvnw spring-boot:run   # 8080
cd ebank-bot         && ./mvnw spring-boot:run   # 8088
```

Attendre **~30 secondes** après le démarrage : c'est le temps que les services se découvrent via Eureka
(avant, des clients peuvent apparaître « Not available »).

Tableau de bord Eureka : http://localhost:8761 (tous les services doivent être « UP »)

> Si un service refuse de démarrer avec `Port XXXX was already in use`, une instance tourne déjà :
> arrêtez-la (bouton ■ dans IntelliJ) ou trouvez-la avec `netstat -ano | findstr :XXXX` puis `taskkill /PID <pid> /F`.

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
leurs opérations sont exposées comme outils pour les agents IA. `ebank-bot` est **client MCP** : à chaque question,
le chatbot récupère les outils des serveurs joignables et les appelle pour répondre avec les vraies données de la banque.

| Serveur | Endpoint | Outils |
|---|---|---|
| customer-service | `http://localhost:8086/mcp` | `getAllCustomers`, `getCustomerById`, `createCustomer` |
| ebank-service | `http://localhost:8087/mcp` | `getAllBankAccounts`, `getBankAccountById`, `getBankAccountsByCustomerId`, `createBankAccount` |

Les outils se limitent volontairement à la lecture et à la création (pas de modification ni de suppression par le LLM).

Les connexions MCP sont ouvertes **à la demande** (`McpServerConnections`), pas au démarrage :
- `ebank-bot` démarre même si `customer-service` ou `ebank-service` est arrêté ;
- les outils d'un service arrêté sont ignorés et le bot répond que l'information est indisponible (sans inventer) ;
- quand le service revient, le bot s'y reconnecte tout seul, sans redémarrage.

Les serveurs MCP se configurent dans `ebank-bot/src/main/resources/application.properties` :

```properties
ebank.mcp.servers.customer-service=http://localhost:8086
ebank.mcp.servers.ebank-service=http://localhost:8087
```

## Tester l'application

### 1. Le chatbot (le plus simple)

Ouvrir directement dans le navigateur :

```
http://localhost:8088/chat?query=Liste tous les clients
http://localhost:8088/chat?query=Quels sont les comptes du client 1 ?
http://localhost:8088/chat?query=Crée un client Karim avec l'email karim@gmail.com
http://localhost:8088/chat?query=Ouvre un compte SAVING de 1000 pour le client 2
```

- Avec `qwen2.5:7b` en local, compter **20 s à 1 min** par réponse.
- Le bot garde l'historique de la conversation (« et son solde total ? » fonctionne). Pour repartir de zéro, redémarrer `ebank-bot`.
- Un modèle 7B peut se tromper d'outil sur une question elliptique (« et ceux du client 2 ? ») :
  préférer des questions complètes (« Quels sont les comptes du client 2 ? »).

### 2. Vérifier que le bot dit vrai

Comparer ses réponses avec l'API REST :

- http://localhost:8080/customers : les clients, via la gateway
- http://localhost:8080/accounts/customer/1 : les comptes du client 1
- http://localhost:8087/swagger-ui.html : Swagger d'`ebank-service`, pour tester POST/PUT/DELETE (idem sur le port `8086` pour `customer-service`)
- http://localhost:8761 : Eureka, tous les services doivent être « UP »

Les données de test sont regénérées à chaque démarrage (H2 en mémoire) : les soldes changent d'un lancement à l'autre.

### 3. Les serveurs MCP directement (optionnel)

Avec Node.js installé :

```bash
npx @modelcontextprotocol/inspector
```

Dans la page qui s'ouvre : transport **Streamable HTTP**, URL `http://localhost:8086/mcp` (ou `8087`), puis **Connect**.
L'onglet **Tools** liste les outils et permet de les appeler à la main, sans passer par le modèle.

### 4. Voir quels outils le modèle appelle

Pour vérifier que le bot interroge vraiment les services au lieu d'inventer, ajouter dans
`ebank-bot/src/main/resources/application.properties` :

```properties
logging.level.org.springframework.ai.model.tool=DEBUG
```

Les logs du bot affichent alors `Executing tool call: getBankAccountsByCustomerId`, etc.
Une réponse sans cette ligne signifie que le modèle a répondu sans consulter les données.

### 5. Tester la résilience du bot

1. Arrêter `ebank-service`, puis demander « Quels sont les comptes du client 1 ? » : le bot répond que l'information
   est indisponible, et ses logs affichent `Serveur MCP ebank-service indisponible`.
2. Redémarrer `ebank-service`, attendre qu'il soit démarré, et reposer la question : le bot se reconnecte
   (`Connecte au serveur MCP ebank-service`) et donne les vrais comptes, sans redémarrage du bot.

## Résilience

`ebank-service` protège ses appels vers `customer-service` avec un circuit breaker Resilience4j :
le circuit s'ouvre si 50 % des 10 derniers appels échouent, reste ouvert 10 s, et un timeout de 3 s déclenche le fallback.
Une réponse 404 (client introuvable) n'est pas comptée comme une panne.

## Console H2

- customer-service : http://localhost:8086/h2-console (JDBC URL `jdbc:h2:mem:customers-db`)
- ebank-service : http://localhost:8087/h2-console (JDBC URL `jdbc:h2:mem:accounts-db`)
