import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpClient, HttpDownloadProgressEvent, HttpErrorResponse, HttpEventType} from '@angular/common/http';
import {AsyncPipe} from '@angular/common';
import {catchError, Observable, of, Subscription} from 'rxjs';
import {apiErrorMessage} from '../model/api-error';

// ebank-bot via la gateway (routage Eureka) : la gateway ajoute les en-tetes CORS
const BOT_URL = "http://localhost:8080/EBANK-BOT/chat";
const BOT_STREAM_URL = "http://localhost:8080/EBANK-BOT/chatStream";

@Component({
  imports: [
    FormsModule,
    AsyncPipe
  ],
  selector: 'app-bot-ui',
  styleUrl: './bot-ui.css',
  templateUrl: './bot-ui.html',
})
export class BotUi {
  query: string = "";
  http = inject(HttpClient);
  response$!: Observable<string>;
  // une conversation par onglet : le bot ne melange pas cet historique avec celui des autres utilisateurs
  private conversationId = crypto.randomUUID();

  // reponse en streaming : le texte grandit au fur et a mesure que le bot l'ecrit
  streamResponse = signal<string>("");
  streaming = signal<boolean>(false);
  private streamSubscription?: Subscription;

  askAgent() {
    if (!this.query.trim()) {
      return;
    }
    // /chat renvoie du texte brut, pas du JSON
    const params = {query: this.query, conversationId: this.conversationId};
    this.response$ = this.http.get(BOT_URL, {params, responseType: 'text'}).pipe(
      catchError((err: HttpErrorResponse) => of("Erreur : " + apiErrorMessage(err)))
    );
  }

  askAgentStream() {
    if (!this.query.trim()) {
      return;
    }
    this.streamSubscription?.unsubscribe();
    this.streamResponse.set("");
    this.streaming.set(true);

    const params = {query: this.query, conversationId: this.conversationId};
    // reportProgress + observe 'events' : Angular emet le texte deja recu (partialText) a chaque morceau.
    // Necessite provideHttpClient(withFetch()) dans app.config.ts
    this.streamSubscription = this.http.get(BOT_STREAM_URL, {
      params,
      responseType: 'text',
      observe: 'events',
      reportProgress: true
    }).subscribe({
      next: event => {
        if (event.type === HttpEventType.DownloadProgress) {
          this.streamResponse.set(parseNdjson((event as HttpDownloadProgressEvent).partialText ?? ""));
        } else if (event.type === HttpEventType.Response) {
          this.streamResponse.set(parseNdjson(event.body ?? ""));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.streamResponse.set("Erreur : " + apiErrorMessage(err));
        this.streaming.set(false);
      },
      complete: () => this.streaming.set(false)
    });
  }
}

// Le serveur envoie un objet JSON par ligne (NDJSON) : {"content":"Voici"}\n{"content":" la"}\n...
// On recolle les lignes completes ; la derniere peut etre incomplete, on l'ignore jusqu'au prochain morceau
function parseNdjson(text: string): string {
  return text.split("\n")
    .filter(line => line.trim())
    .map(line => {
      try {
        return (JSON.parse(line) as {content: string}).content ?? "";
      } catch {
        return "";
      }
    })
    .join("");
}
