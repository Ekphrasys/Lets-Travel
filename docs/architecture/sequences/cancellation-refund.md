# Séquence — Annulation

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant FE as Angular
    participant GW as Gateway
    participant TRAVEL as travel-service
    participant PAY as payment-service

    U->>FE: Annule réservation
    FE->>GW: DELETE /api/bookings/{id}<br/>Authorization: Bearer
    GW->>TRAVEL: Forward

    TRAVEL->>TRAVEL: Vérifie ownership (userId du JWT)

    alt Booking CONFIRMED
        TRAVEL->>PAY: POST /api/payments/{id}/refund
        PAY->>PAY: status → REFUNDED
        PAY-->>TRAVEL: 200
        TRAVEL->>TRAVEL: booking → CANCELLED
        TRAVEL->>TRAVEL: seats_available + 1
    else Booking PENDING
        TRAVEL->>TRAVEL: booking → CANCELLED
    end

    TRAVEL-->>FE: 200
```

## Règles

| Statut initial | Action | Statut final |
|----------------|--------|--------------|
| PENDING | Annulation directe | CANCELLED |
| CONFIRMED | Remboursement + annulation | CANCELLED |
| CANCELLED | Rejet | — |
