package org.romaric.ebankbot.controllers;
import org.romaric.ebankbot.agents.EbankAIAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class EbankChatbotController {

    private EbankAIAgent ebankAIAgent;

    public EbankChatbotController(EbankAIAgent ebankAIAgent) {
        this.ebankAIAgent = ebankAIAgent;
    }


    // conversationId : identifiant de la conversation (le front Angular en genere un par onglet) ;
    // sans lui, toutes les requetes partagent la conversation "web"
    @GetMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestParam(name = "query" ,defaultValue = "Bonjour") String query,
                       @RequestParam(name = "conversationId", defaultValue = "default") String conversationId){
        return ebankAIAgent.chat("web-" + conversationId, query);
    }

    // un morceau de reponse : {"content":"..."}
    public record ChatChunk(String content) {}

    // reponse en streaming : un objet JSON par ligne (NDJSON), envoye des que le modele l'ecrit.
    // - NDJSON plutot que SSE : SSE supprime l'espace en debut de morceau et colle les mots entre eux ;
    // - un objet plutot qu'une String : une String serait ecrite brute, sans guillemets ni separateur
    @GetMapping(value = "/chatStream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ChatChunk> chatStream(@RequestParam(name = "query" ,defaultValue = "Bonjour") String query,
                                      @RequestParam(name = "conversationId", defaultValue = "default") String conversationId){
        return ebankAIAgent.chatStream("web-" + conversationId, query).map(ChatChunk::new);
    }

    // efface l'historique de la conversation
    @DeleteMapping("/chat")
    public void reset(@RequestParam(name = "conversationId", defaultValue = "default") String conversationId){
        ebankAIAgent.reset("web-" + conversationId);
    }



}
