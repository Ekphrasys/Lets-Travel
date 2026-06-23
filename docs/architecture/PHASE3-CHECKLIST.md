# Phase 3 — Backend

> Auth, User, Travel, Payment services

---

## Services implémentés

| Service | Port | BDD | API |
|---------|------|-----|-----|
| auth-service | 8081 | PostgreSQL `auth` | `/api/auth/register`, `/api/auth/login` |
| user-service | 8082 | PostgreSQL `user` | `/api/users/**` |
| travel-service | 8083 | PostgreSQL `travel` + Neo4j | `/api/travels/**`, `/api/bookings/**` |
| payment-service | 8084 | PostgreSQL `payment` | `/api/payments/**` |

Tous passent par la Gateway HTTPS : `https://localhost:8080`

---

## Lancement complet

```bash
cp .env.example .env
docker compose -f infrastructure/docker-compose.yml up --build -d
```

Attendre ~2 min que Eureka enregistre tous les services.

---

## Tests manuels

### 1. Register

```bash
curl -k -X POST https://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@travel.com","password":"password123","firstName":"Jean","lastName":"Dupont"}'
```

### 2. Login

```bash
curl -k -X POST https://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@travel.com","password":"password123"}'
```

Copier le `token` retourné.

### 3. Catalogue voyages (public)

```bash
curl -k https://localhost:8080/api/travels
```

### 4. Recherche itinéraire Neo4j (public)

```bash
curl -k "https://localhost:8080/api/travels/routes/search?origin=Paris&destination=Tokyo"
```

### 5. Réserver

```bash
curl -k -X POST https://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer VOTRE_TOKEN" \
  -d '{"tripId":"UUID_DU_VOYAGE"}'
```

### 6. Mes réservations

```bash
curl -k https://localhost:8080/api/bookings/me \
  -H "Authorization: Bearer VOTRE_TOKEN"
```

---

## Créer un admin (SQL)

```bash
docker exec -it travel-postgres psql -U travel -d travel_db \
  -c "UPDATE \"user\".users SET role = 'ADMIN' WHERE email = 'test@travel.com';"
```

Puis se reconnecter pour obtenir un JWT avec rôle ADMIN.
