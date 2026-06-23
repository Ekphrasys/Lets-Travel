# Phase 4 — Frontend Angular

> Dashboard admin Travel Management System

---

## Application

| Élément | Valeur |
|---------|--------|
| Dossier | `frontend/travel-admin/` |
| Framework | Angular 20 (standalone) |
| URL dev | `https://localhost:4200` (SSL) |
| API | `https://localhost:8080` (Gateway) |

---

## Pages

| Route | Accès | Description |
|-------|-------|-------------|
| `/login` | Invité | Connexion |
| `/register` | Invité | Inscription |
| `/` | Auth | Dashboard |
| `/trips` | Auth | Catalogue voyages |
| `/trips/new`, `/trips/:id/edit` | Admin | CRUD voyages |
| `/bookings` | Auth | Mes réservations |
| `/routes` | Public | Recherche itinéraires Neo4j |
| `/admin/users` | Admin | Gestion utilisateurs |
| `/admin/payments` | Admin | Liste paiements |

---

## Lancement complet (stack)

### 1. Backend (Docker)

```bash
cp infrastructure/.env.example infrastructure/.env
docker compose -f infrastructure/docker-compose.yml up --build -d
```

Attendre ~2 min (Eureka + microservices).

### 2. Frontend (local)

```bash
cd frontend/travel-admin
npm install
npm start
```

Ouvrir `https://localhost:4200` et accepter le certificat self-signed.

> Le certificat SSL est dans `frontend/travel-admin/ssl/` (`localhost-cert.pem`, `localhost-key.pem`).

---

## Scénario de test

1. **Inscription** : `/register` → créer un compte
2. **Connexion** : `/login` → accéder au dashboard
3. **Voyages** : `/trips` → voir les 3 voyages démo
4. **Itinéraires** : `/routes` → Paris → Tokyo (sans login)
5. **Réservation** : `/bookings` → réserver un voyage
6. **Admin** : promouvoir l'utilisateur en SQL puis se reconnecter :

```bash
docker exec -it travel-postgres psql -U travel -d travel_db \
  -c "UPDATE \"user\".users SET role = 'ADMIN' WHERE email = 'votre@email.com';"
```

7. **Admin CRUD** : `/trips/new` → créer un voyage
8. **Admin users/payments** : `/admin/users`, `/admin/payments`

---

## Build production

```bash
cd frontend/travel-admin
npm run build
```

Sortie : `frontend/travel-admin/dist/travel-admin/`

---

## Phase suivante

Phase 5 : Jenkins, SonarQube, tests automatisés.
