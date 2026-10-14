# Segna punti Padel - specifiche 002

File di integrazione a Specifiche-001.md

## Interfaccia grafica

Durante l'esecuzione dell'app lo schermo deve rimanere sempre acceso.

## Punteggi e limiti
I punteggi e limiti di set, game e "vantaggio pari" NON devono essere salvati in memoria persistente (SharedPreferences) in modo che l'utente debba reinserirli ogni volta che apre l'app.
Il punteggio è quello del Padel che è lo stesso del Tennis, quindi 0, 15, 30, 40 e vantaggio. 
- L'utente può impostare il numero di set per ogni match (default 3).
- L'utente può impostare il numero massimo di game per ogni set (default 10).
- L'utente può impostare il numero massimo di "vantaggio pari" per ogni game dopo il quale si gioca il "killer point" (default 1).
- "killer point": la prima squadra che segna il punto vince il game.
- tie-break: se il set arriva a 5-5 si gioca il tie-break. Vince il set la squadra che arriva per prima a 7 punti con almeno 2 punti di vantaggio, cioè 7-5 o 5-7. Se il punteggio arriva a 6-6 (secondo tie-break) si continua a giocare fino a quando una delle due squadre non ha 2 punti di vantaggio cioè 8-6 o 6-8. 
Se il punteggio arriva a 7-7 (terzo tie-break) si gioca fino a 7-9 o 9-7 e così via fino al massimo numero di set impostato dall'utente (es. 10-9 o 9-10).
- Vince il match (o partita) la squadra che vince il maggior numero di set.
- Vince il set la squadra che arriva per prima a 6 game vinti con almeno 2 game di vantaggio.

Esempio punteggio di un game:
0 - 0
15 -0
30 - 0
30 - 15
30 - 30
40 - 30
40 - 40
Vantaggio A - 40
Vantaggio pari (max. 1)
killer point: la prima squadra che segna il punto vince il game.

Esempio punteggio finale di un set:
3-6

Esempio punteggio finale di un match:
3-6
6-4
7-5

### Chiarimenti sui punteggi e limiti

Chiarimento su tie-break e numero massimo di game per set.
La regola dice che il set lo vince la squadra che arriva per prima a 6 games vinti con almeno uno scarto di 2 game sull'altra squadra. Quindi, il primo tie-break si gioca quando entrambe le squadre vincono 5 games ciascuno.
5-5 --> si vince a 7 games con 2 di scarto: 7-5 o 5-7
6-6 --> si vince a 8 games con 2 di scarto: 8-6 o 6-8
7-7 --> si vince a 9 games con 2 di scarto: 9-7 o 7-9
8-8--> si vince a 10 games con 2 di scarto: 10-8 o 8-10
9-9--> si vince a 11 games con 2 di scarto: 11-9 o 9-11
...e così via fino all'infinito!
Siccome vorrei evitare una partita infinita impongo io un "limite massimo" di set per match o di tie-break.
Quindi vale la regola di cui sopra ma fino al raggiungimento del limite impostato dall'utente.
Esempio, se l'utente imposta numero massimo di set per match = 10 si potrebbe avere:
5-5
6-5
6-6
7-6
7-7
8-7
8-8
8-9
9-9
9-10
STOP! vince la squadra che ha vinto 10 set.

Chiarimento su "vantaggio pari" e killer point.
Il punteggio del Padel è simile a quello del Tennis:
0
15
30
40

quando le due squadre raggiungono il punteggi di 40-40 si dice "vantaggio pari" (0-0). Dal 40-40 in poi i punti si segnano così (esempio):
0-0 (vantaggio pari)
1-0 (vantaggio A)
0-1 (vantaggio B)
1-1 (vantaggio pari) --> 0-0
raggiunto il vantaggio pari (1-1) la regola dice che il punteggio ritorna a 0-0 e si ricompincia fino a quando una delle due squadre non raggiunge "vantaggio doppio", cioè non faccia due vantaggi consecutivi. Per esempio 2-0 o 2-1.
Siccome non voglio che si faccia un set infinito, bisogna dare la possibilità di impostare il "numero massimo di vantaggi pari", raggiunto tale limite si gioca il "killer point".
Esempio:
...
...
40-40 --> 0-0
1-0
1-1 (killer point)
1-2 --> vince B