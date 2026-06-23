# Phase 2 — Checklist Infrastructure

> **Statut :** En cours — Infrastructure Docker

---

## Livrables

- [x] `infrastructure/docker-compose.yml`
- [x] PostgreSQL — init 4 schémas + tables + seed trips
- [x] Neo4j — seed graphe villes
- [x] Réseau `travel-network`
- [x] `discovery-service` (Eureka)
- [x] `gateway-service` (HTTPS + routes Phase 3)
- [x] `.env.example`
- [x] Keystore `travel.p12`
- [x] Documentation (`infrastructure/README.md`, `CmdLancement.md`)

---

## Validation

- [x] `docker compose up --build` démarre sans erreur
- [ ] PostgreSQL : 4 schémas + 3 trips démo (vérifier localement)
- [ ] Neo4j : 4 villes + connexions (vérifier localement)
- [ ] Eureka accessible sur http://localhost:8761
- [ ] Gateway HTTPS : `curl -k https://localhost:8080/actuator/health`

---

## Phase suivante

**Phase 3 — Backend** : auth-service, user-service, travel-service, payment-service
