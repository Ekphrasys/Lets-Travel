# Séquence — Réservation et Paiement

> Flux 100 % synchrone — une seule requête HTTP côté client

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant FE as Angular
    participant GW as Gateway
    participant TRAVEL as travel-service
    participant PAY as payment-service

    U->>FE: Choisit un voyage
    FE->>GW: POST /api/bookings { tripId }<br/>Authorization: Bearer
    GW->>TRAVEL: Forward

    TRAVEL->>TRAVEL: Vérifie places disponibles
    TRAVEL->>TRAVEL: INSERT booking (PENDING)

    TRAVEL->>PAY: POST /api/payments { bookingId, amount, userId }
    PAY->>PAY: Mock paiement

    alt Paiement réussi
        PAY-->>TRAVEL: 201 { paymentId, status: COMPLETED }
        TRAVEL->>TRAVEL: booking → CONFIRMED
        TRAVEL->>TRAVEL: seats_available - 1
        TRAVEL-->>FE: 201 { booking, status: CONFIRMED }
    else Paiement échoué
        PAY-->>TRAVEL: 201 { status: FAILED }
        TRAVEL->>TRAVEL: booking → CANCELLED
        TRAVEL-->>FE: 422 Paiement refusé
    end
```

## Statuts

```
PENDING ──OK──► CONFIRMED
   │
   └──KO──► CANCELLED
```

## Mock paiement

Le payment-service accepte tout montant > 0.  
Pour tester l'échec en dev : envoyer `amount = 0`.
