# Segna Punti Padel Lite

Versione nativa e leggera del segnapunti per dispositivi datati
(es. Mediacom M-MP720M, Android 4.1, 1024x600, 512 MB di RAM).

- Modulo: `app-lite` (applicationId `it.pv.segnapuntipadel.lite`, versione 1.1 / versionCode 3).
- Android minimo: 4.1 (API 16). Tema Holo nativo, nessuna libreria AndroidX, Compose o coroutine.
- Funziona completamente offline e **non richiede alcun permesso**.
- Le regole di punteggio sono le stesse dell'app principale: il calcolo è eseguito dalla libreria
  condivisa `:scoring` (`ApplyMatchAction` / `ScoringEngine`), senza modifiche.

## Installazione e aggiornamento dell'APK

1. Generare l'APK con `.\gradlew.bat :app-lite:assembleDebug`
   (`app-lite\build\outputs\apk\debug\app-lite-debug.apk`).
2. Copiare l'APK sul tablet (cavo USB, scheda SD) oppure installarlo con
   `adb install -r app-lite-debug.apk`.
3. Sul tablet abilitare *Impostazioni → Sicurezza → Origini sconosciute* e aprire il file APK.

La versione 1.1 (versionCode 3) ha lo stesso applicationId e va firmata con la **stessa chiave**
(debug) delle versioni precedenti: si installa quindi sopra come aggiornamento, senza
disinstallare. Se la firma è diversa Android rifiuta l'aggiornamento: in quel caso disinstallare
prima la versione precedente.

## Utilizzo

### Impostazione partita
- Nomi dei quattro giocatori (facoltativi: se vuoti compaiono "Giocatore 1 / Giocatore 2").
- Regole, con valori predefiniti 3 set, 10 game massimi per set, 1 vantaggio pari prima del killer point.
- Validazione identica all'app principale: numeri interi obbligatori, set e vantaggi maggiori di zero,
  game massimi per set almeno 6. Non ci sono altri limiti massimi.
- Ad ogni apertura dell'app i campi ripartono vuoti/predefiniti. Solo scegliendo "Nuova configurazione"
  a fine partita (app ancora aperta) i campi sono precompilati con la partita appena giocata.

### Tabellone (orizzontale, alto contrasto)
- Due grandi aree di tocco, una per squadra (A verde a sinistra, B rossa a destra).
- **Primo tocco**: sceglie la squadra al servizio, senza assegnare punti.
- **Tocco**: punto alla squadra. **Pressione lunga**: corregge l'ultimo punto di quella squadra
  (la scelta iniziale del servizio non viene mai annullata).
- Punteggio come nell'app principale: 0/15/30/40, vantaggio `1`/`0`, "Parità raggiunte: N" dopo
  ogni vantaggio annullato, `k` per il killer point. Nessun tie-break.
- Barra colorata sotto il punteggio della squadra al servizio.
- In alto: set corrente ("Set N di M"), cronometro, indicazioni di stato; sotto, la cronologia dei
  set **giocati** (completati + set in corso evidenziato in giallo), scorrevole se lunga.
- Le dimensioni dei testi si adattano allo spazio effettivo dello schermo (nomi lunghi e la scritta
  "Parità raggiunte" vengono ridotti per non essere tagliati; i nomi troppo lunghi terminano con "…").
- Segnali acustici: 1 beep punto (anche la scelta del servizio), 2 beep fine set, 3 beep fine partita.
- Lo schermo resta acceso mentre il tabellone è visibile.
- Vengono sempre giocati tutti i set configurati; la partita può terminare in parità.
- A fine partita: **Nuova partita** (stessa configurazione), **Nuova configurazione** oppure **Esci**.
- Il riquadro di fine partita si chiude con Indietro o toccando fuori: il risultato resta visibile e si
  può correggere l'ultimo punto con una pressione lunga (la partita riprende se non è più terminata).
  Un tocco sul tabellone a partita terminata riapre il riquadro.
- Il tasto Indietro dal tabellone torna all'impostazione; se la partita è in corso chiede prima conferma,
  perché il punteggio andrebbe perso.

### Telecomando Bluetooth (es. AB Shutter3)

- Accoppiare il telecomando dalle impostazioni Bluetooth di Android; l'app non richiede permessi.
- Sul tabellone: tasto **VOLUME_UP** = punto alla **Squadra A**, tasto **VOLUME_DOWN** = punto
  alla **Squadra B**. Si comporta come il tocco sul pannello (il primo comando sceglie il servizio).
- **Un punto per pressione**: tenendo premuto un tasto le ripetizioni sono ignorate; per un nuovo
  punto bisogna rilasciare e premere di nuovo.
- La pressione lunga sul telecomando **non** corregge: la correzione resta la pressione lunga sullo schermo.
- Solo i dispositivi esterni assegnano punti: i tasti volume del tablet continuano a regolare il volume.
  Su Android precedenti al 10 il riconoscimento usa un metodo di sistema nascosto; se non fosse
  leggibile, ogni tasto volume fisico verrebbe trattato come telecomando (errore nel log `SegnaPuntiLite`).
- Nella schermata di impostazione i tasti del telecomando non hanno effetto sul punteggio.
- Codici e ripetizioni dei tasti sono stati rilevati sul Samsung A22 (Android 13) con il
  Test telecomando dell'app principale; sullo SmartPad accoppiamento e ricezione dei tasti vanno provati.

## Dati della partita (solo memoria di sessione)

Come richiesto da `docs/Specifiche-002.md`, punteggi e limiti di set, game e "vantaggio pari"
**non vengono salvati in memoria persistente** (né SharedPreferences né file): ad ogni apertura
dell'app l'utente li reinserisce.

- La partita vive solo nella memoria dell'app mentre è aperta.
- Se Android ricrea la schermata (es. app in background e processo liberato dal sistema per mancanza di
  memoria, poi riaperta dalle app recenti) la partita viene ripristinata dallo stato temporaneo
  dell'Activity (`onSaveInstanceState`), gestito dal sistema e non scritto dall'app su disco.
  Se lo stato non è valido compare un messaggio e l'errore viene registrato nel log (tag `SegnaPuntiLite`).
- Chiudendo l'app (Indietro dall'impostazione, "Esci", rimozione dalle app recenti) o riavviando il
  tablet, la partita è persa e al successivo avvio si riparte dai valori predefiniti.
- Il cronometro usa un orologio monotono e, come nell'app principale, continua a contare
  anche in background e a fine partita. Viene azzerato quando si avvia una nuova partita.

## Differenze rispetto all'app principale

- Una sola Activity nativa con layout classici; nessuna libreria esterna oltre a `:scoring`.
- La cronologia mostra solo i set completati e quello in corso (non i set futuri a 0-0).
- A fine partita è disponibile anche "Nuova configurazione"; il riquadro finale si può chiudere per
  correggere il punteggio.
- Abbandonare una partita in corso con Indietro richiede conferma.
- Se una correzione ricostruisce la partita, le azioni diventate inefficaci vengono scartate
  (il punteggio risultante è identico al ricalcolo dell'app principale).
- Nessuna icona personalizzata: viene usata l'icona predefinita di Android.