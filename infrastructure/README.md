# Phase 2 — Infrastructure

Stack Docker complète du projet Travel (microservices + frontend admin).

## Contenu

| Service | Container | Port hôte | Description |
|---------|-----------|-----------|-------------|
| PostgreSQL | travel-postgres | — (interne) | 4 schémas : auth, user, travel, payment |
| Neo4j | travel-neo4j | — (interne) | Graphe de villes |
| Eureka | travel-discovery | 8761 | Service discovery |
| Gateway | travel-gateway | 8080 (HTTPS) | Point d'entrée API |
| Auth | travel-auth | — (interne) | Authentification JWT |
| User | travel-user | — (interne) | Profils utilisateurs |
| Travel | travel-app | — (interne) | Voyages et réservations |
| Payment | travel-payment | — (interne) | Paiements mock |
| Frontend | travel-admin | 4200 (HTTPS) | Interface Angular admin |

Réseau Docker : `travel-network` — projet Compose : `travel`

---

## Démarrage

```bash
# 1. Variables d'environnement (à la racine du repo)
cp .env.example .env

# 2. Lancer la stack
docker compose -f infrastructure/docker-compose.yml --project-name travel up --build -d

# Ou via script (Jenkins / Ansible)
DEPLOY_ENV_FILE=.env bash scripts/deploy-stack.sh
```

---

## URLs

| Service | URL |
|---------|-----|
| Frontend Angular | https://localhost:4200 |
| API Gateway | https://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Actuator Gateway | https://localhost:8080/actuator/health |

> Accepter le certificat self-signed dans le navigateur.

Compte démo : `test@travel.com` / `password123` (promouvoir en ADMIN via SQL si besoin).

---

## Vérifications

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel ps
curl -sk https://localhost:8080/actuator/health
curl -sk -o /dev/null -w '%{http_code}\n' https://localhost:4200/
```

---

## Arrêt

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel down
```

Reset complet (BDD + Neo4j) :

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel down -v
```

---

## CI/CD

Jenkins et SonarQube tournent dans un projet Compose séparé (`travel-ci`) :

```bash
docker compose -f jenkins-compose.yml up -d
```

Voir `docs/CI-AUDIT-SETUP.md` et `README.md` à la racine.
