# Communication inter-services

> Version finale — REST synchrone uniquement

---

## 1. Principe

Toute communication client ↔ backend passe par **HTTPS REST**.  
Les microservices communiquent entre eux en **HTTP** sur le réseau Docker interne (non exposé).

```
Angular (https://localhost:4200)
    → Gateway (https://localhost:8080)
        → Microservice HTTP interne (via Eureka)
            → PostgreSQL / Neo4j
```

---

## 2. Routage Gateway

| Route | Service |
|-------|---------|
| `/api/auth/**` | auth-service |
| `/api/users/**` | user-service |
| `/api/travels/**` | travel-service |
| `/api/bookings/**` | travel-service |
| `/api/payments/**` | payment-service |

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/api/auth/**
        - id: users
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/users/**
        - id: travels
          uri: lb://TRAVEL-SERVICE
          predicates:
            - Path=/api/travels/**, /api/bookings/**
        - id: payments
          uri: lb://PAYMENT-SERVICE
          predicates:
            - Path=/api/payments/**
```

---

## 3. Appels inter-services

| Appelant | Appelé | Quand |
|----------|--------|-------|
| auth-service | user-service | Inscription → créer le profil |
| auth-service | user-service | Login → récupérer le rôle pour le JWT |
| travel-service | payment-service | Réservation → initier paiement |
| travel-service | payment-service | Annulation → rembourser |

**Client :** `WebClient` Spring + `@LoadBalanced`, résolution Eureka.

**Header propagé :** `Authorization: Bearer {JWT}`

---

## 4. Format des réponses

Réponses Spring Boot standard — pas d'enveloppe custom.

**Succès :**
```json
{
  "id": "uuid",
  "title": "Paris → Tokyo",
  "price": 650.00
}
```

**Erreur :**
```json
{
  "code": "NOT_FOUND",
  "message": "Voyage introuvable"
}
```

| Code HTTP | Usage |
|-----------|---------|
| 200 | OK |
| 201 | Créé |
| 400 | Données invalides |
| 401 | Non connecté |
| 403 | Pas les droits |
| 404 | Introuvable |
| 409 | Conflit (email déjà pris) |
| 500 | Erreur serveur |

---

## 5. Flux réservation (synchrone)

```
1. travel-service crée booking (status: PENDING)
2. travel-service appelle POST /api/payments
3. payment-service simule le paiement → COMPLETED ou FAILED
4. travel-service met booking → CONFIRMED ou CANCELLED
```

Pas de timeout complexe, pas de saga — tout dans la même requête HTTP.

---

## 6. Réseau Docker (Phase 2)

- Réseau interne : `travel-network`
- Ports exposés vers l'hôte :
  - Gateway : `8080`
  - Eureka : `8761`
  - Angular : `4200`
  - PostgreSQL, Neo4j : **non exposés**

---

## 7. Séquences détaillées

- [Login / Register](./sequences/login.md)
- [Réservation + paiement](./sequences/booking-payment.md)
- [Annulation](./sequences/cancellation-refund.md)
