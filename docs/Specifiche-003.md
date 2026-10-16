# Segna punti Padel - specifiche 003

File di integrazione a Specifiche-001.md e Specifiche-002.md

## Turno di battuta
Il turno di battuta deve essere alternato tra le due squadre.
La squadra che vince uno scambio o il sorteggio iniziale ha il diritto di scegliere se iniziare a battere o ricevere.
Dopo ogni game, il turno di battuta passa all'altra squadra.

### Implementazione
Dopo il "TAP" del pulsante "AVVIA PARTITA" (sul punteggio di 0-0 del primo game), il primo incremento di punteggio (sia da TAP su schermo che da segnale del dispositivo remoto) deve essere considerato come "assegnazione del primo turno di battuta".
In sostanza, al primo TAP (o segnale da dispositivo remoto) NON deve essere assegnato un punto, ma solo determinato quale squadra inizia a battere; dopo di che, il punteggio deve essere incrementato normalmente.
Dopo ogni game, il turno di battuta passa all'altra squadra.
Per "segnalare" quale squadra sta battendo (o servendo), il punteggio della squadra che sta battendo deve essere evidenziato con un tratto (linea spessa) orizontale sotto il rispettivo punteggio.