# Segna punti Padel - specifiche 001

## Idea

Implementare un app android molto semplice nella grafica con lo scopo di segnare i punti durante una partita di padel. L'app deve essere semplice e intuitiva, con un'interfaccia chiara per inserire i punti e visualizzare il punteggio in tempo reale.

## Tecnologie

- L'applicazione è un'app Android nativa.
- Il linguaggio principale è Kotlin.
- I layout dell'interfaccia utente sarà realizzato con XML.

## Caratteristiche principali

- Interfaccia utente semplice e intuitiva.
- Possibilità di segnare punti per due squadre (A e B).
- Visualizzazione del punteggio in tempo reale.
- Possibilità di resettare/correggere il punteggio.
- Possibilità di personalizzare i nomi dei giocatori.
- Possibilità di mostrare il tempo di gioco della partita.
- Possibilità di impostare un limite di set per le partite.
- Possibilità di impostare un limite di game il set.
- Possibilità di impostare un limite di tie-break il set.

## Interfaccia grafica

L'interfaccia grafica dell'app sarà semplice e intuitiva, con pulsanti grandi per segnare i punti e visualizzare il punteggio in tempo reale. Sarà possibile personalizzare i nomi dei giocatori e visualizzare il tempo di gioco della partita.
La rotazione dello schermo deve essere disabilitata e la visualizzazione è sempre orizontale.

## Flusso

1. L'utente apre l'app e visualizza la schermata principale con i campi per defininire i quattro nomi delle due squadre, "A" e "B".
2. Nella schermata successiva l'utente può impostare il limite di set (default 3), game (default 10) e numero massimo di "vantaggio pari" dopo il quale si gioca il "killer point" (default 1).
3. Nella stessa schermata l'utente può iniziare tramite apposito pulsante "Avvia partita"
4. Nella schermata successiva (Schermata di gioco) vengono mostrati: punteggio corrente, situazione game/set, tempo di gioco, i due pulsanti di incremento punteggio (uno per squadra A e uno per squadra B).
5. Nella schemata di gioca, l'utente può correggere il punteggio con un tocco prolungato (Tap and hold ) sul punteggio specifico da correggere.
6. Quando una squadra vince un set, viene mostrato un messaggio di notifica e il punteggio viene resettato per il nuovo set.
7. Quando una squadra vince la partita, viene mostrato un messaggio di notifica e l'utente può scegliere se resettare il punteggio per iniziare una nuova partita o uscire dall'app.

## Funzionalità future

L'incremento del punteggio di una squadra piuttosto che l'altra verrà eseguito tramite dispositivo bluetooth remoto. In questo modo, il punteggio verrà aggiornato automaticamente senza dover toccare lo smartphone.
Tutte le caratteristiche rimangono invariate a prescindere che l'incremento del punteggio avvenga con "tap" sullo schermo del cellulare o tramite segnale BT da dispositivo remoto.
