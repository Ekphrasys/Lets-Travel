# Sécurité

> Version finale — JWT simple, 2 rôles

---

## 1. Authentification

| Élément | Valeur |
|---------|--------|
| Émetteur | auth-service (seul) |
| Algorithme | HS256 |
| Secret | Variable d'env `JWT_SECRET` (partagé entre services) |
| Durée | 24 heures |
| Refresh token | **Non** — l'utilisateur se reconnecte |

### Payload JWT

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "exp": 1718620800
}
```

### Flux login

```
POST /api/auth/login { email, password }
  → auth-service vérifie BCrypt
  → auth-service appelle user-service pour le rôle
  → retourne { token, userId, role }
```

### Flux register

```
POST /api/auth/register { email, password, firstName, lastName }
  → auth-service crée users_auth
  → auth-service appelle user-service (role = USER)
  → retourne { token, userId, role }
```

---

## 2. Validation JWT

Chaque microservice métier valide le token avec le même `JWT_SECRET` (filtre Spring Security).

La Gateway **route uniquement** — elle ne valide pas le JWT.

### Endpoints publics (sans token)

| Endpoint | Service |
|----------|---------|
| `POST /api/auth/register` | auth |
| `POST /api/auth/login` | auth |
| `GET /api/travels` | travel |
| `GET /api/travels/routes/search` | travel |
| `GET /actuator/health` | tous |

---

## 3. Autorisation

### Rôles

| Rôle | Droits |
|------|--------|
| `USER` | Réserver, voir ses données |
| `ADMIN` | Tout + gestion catalogue et utilisateurs |

### Matrice simplifiée

| Action | USER | ADMIN | Public |
|--------|:----:|:-----:|:------:|
| Register / Login | — | — | ✅ |
| Voir catalogue voyages | ✅ | ✅ | — |
| Recherche itinéraire Neo4j | ✅ | ✅ | — |
| Réserver | ✅ | ✅ | — |
| Voir ses réservations | ✅ | ✅ | — |
| Annuler sa réservation | ✅ | ✅ | — |
| CRUD voyages (travelers) | ❌ | ✅ | — |
| CRUD utilisateurs | ❌ | ✅ | — |
| CRUD paiements | ❌ | ✅ | — |
| Lister tous les users / paiements | ❌ | ✅ | — |

### Appels inter-services (interne Docker)

| Endpoint | Protection |
|----------|------------|
| `POST /api/users/internal` | Header `X-Internal-Key` |
| `GET /api/users/internal/by-email/{email}` | Header `X-Internal-Key` |
| `POST /api/payments/internal` | Header `X-Internal-Key` |
| `POST /api/payments/internal/{id}/refund` | Header `X-Internal-Key` |

### Implémentation

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/api/travels")
public TripDto createTrip(@RequestBody CreateTripRequest req) { ... }
```

---

## 4. Mots de passe

- **BCrypt** (Spring Security par défaut)
- Min 8 caractères
- Jamais stockés en clair

---

## 5. HTTPS

HTTPS est **obligatoire** entre le frontend Angular et la Gateway (comme Buy-01 / Nexus).

| Composant | Protocole | URL dev |
|-----------|-----------|---------|
| Angular | HTTPS | `https://localhost:4200` |
| Gateway | HTTPS | `https://localhost:8080` |
| Microservices (interne Docker) | HTTP | réseau `travel-network` |
| Eureka | HTTP | `http://discovery-service:8761` (interne) |

### Gateway — keystore PKCS12

Fichier : `microservices/gateway-service/src/main/resources/travel.p12`

```yaml
server:
  port: 8080
  ssl:
    enabled: true
    key-store: classpath:travel.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD:password}
    key-store-type: PKCS12
    key-alias: travel
```

Génération du certificat dev (self-signed) :

```bash
keytool -genkeypair -alias travel -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore travel.p12 -validity 365 \
  -storepass password -keypass password \
  -dname "CN=localhost, OU=Travel, O=Zone01, L=Paris, ST=IDF, C=FR"
```

Placer `travel.p12` dans `gateway-service/src/main/resources/`.

### Angular

```bash
ng serve --ssl
```

`environment.ts` :

```typescript
export const environment = {
  apiUrl: 'https://localhost:8080'
};
```

Le navigateur affichera un avertissement certificat self-signed en dev — accepter pour localhost.

---

## 6. CORS

Sur la Gateway, autoriser **uniquement** Angular en HTTPS :

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "https://localhost:4200"
            allowedMethods:
              - GET, POST, PUT, DELETE, OPTIONS
            allowedHeaders:
              - Authorization, Content-Type
            allowCredentials: true
```

---

## 7. Secrets

| Variable | Usage |
|----------|-------|
| `JWT_SECRET` | Signature JWT |
| `INTERNAL_API_KEY` | Appels inter-services (auth→user, travel→payment) |
| `SSL_KEYSTORE_PASSWORD` | Keystore Gateway (travel.p12) |
| `POSTGRES_PASSWORD` | PostgreSQL |
| `NEO4J_PASSWORD` | Neo4j |

Fichier `.env.example` en Phase 2 — jamais de secrets dans Git.

---

## 8. Paiement

- Mock uniquement — pas de numéro de carte
- Pas de données bancaires sensibles
