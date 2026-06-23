# Travel — Commandes de lancement

## Phase 2 — Infrastructure

```bash
cp .env.example .env
docker compose -f infrastructure/docker-compose.yml up --build -d
```

### Accès

- Eureka : http://localhost:8761
- Gateway API (HTTPS) : https://localhost:8080
- Angular (Phase 4) : https://localhost:4200 (`ng serve --ssl`)

---

## Phase 3 — Backend (stack complet)

```bash
docker compose -f infrastructure/docker-compose.yml up --build -d
```

Voir [docs/architecture/PHASE3-CHECKLIST.md](docs/architecture/PHASE3-CHECKLIST.md) pour les tests API.

### Arrêt

```bash
docker compose -f infrastructure/docker-compose.yml down
```

---

## Développement local (Phase 3+)

Lancer l'infra Docker, puis les microservices en local :

```bash
docker compose -f infrastructure/docker-compose.yml up -d postgres neo4j discovery-service
```

Gateway et services métier peuvent être lancés via IDE ou `mvn spring-boot:run`.

Frontend Angular en HTTPS :

```bash
cd frontend/travel-admin
ng serve --ssl
```
