# Phase 5 — CI/CD + Qualité

> Jenkins, SonarQube, tests automatisés

---

## Fichiers ajoutés

| Fichier | Rôle |
|---------|------|
| `Jenkinsfile` | Pipeline CI/CD complet |
| `jenkins-compose.yml` | Jenkins + SonarQube en Docker |
| `Dockerfile` | Image Jenkins (JDK 17 + Chromium) |
| `sonar-project.properties` | Config SonarQube locale |

---

## Prérequis SonarQube

```bash
sudo sysctl -w vm.max_map_count=262144
```

---

## Lancer Jenkins + SonarQube

```bash
cd /home/zone01student/BrancheJava/travel
docker compose -f jenkins-compose.yml up -d --build
```

| Service | URL |
|---------|-----|
| Jenkins | http://localhost:9092 |
| SonarQube | http://localhost:9002 |

Login SonarQube par défaut : `admin` / `admin` (changer au 1er accès).

---

## Configuration Jenkins (automatisée — audit)

**Guide complet :** `docs/CI-AUDIT-SETUP.md`

```bash
chmod +x scripts/setup-ci.sh scripts/setup-sonarqube.sh
./scripts/setup-ci.sh
```

Ce script configure SonarQube (projet, Quality Gate, token) et Jenkins (JCasC).

### Configuration manuelle (résumé)

1. **SonarQube** http://localhost:9002 → token + Quality Gate **Travel Audit Gate**
2. **Jenkins** http://localhost:9092 → tools `jdk-17`, `maven-3`, `node-22`, `sonar-scanner`
3. **SonarQube server** dans Jenkins : name `sonar-server`, URL `http://sonarqube:9000`
4. **Job pipeline** : `travel` (auto-créé par JCasC si plugins installés)

---

## Configuration Jenkins (détail manuel)

1. **Plugins** : SonarQube Scanner, SonarQube Quality Gates
2. **Global Tool Configuration** :
   - JDK 17 → `jdk-17`
   - Maven 3 → `maven-3`
   - NodeJS 22 → `node-22`
   - SonarQube Scanner → `sonar-scanner`
3. **Manage Credentials** : ajouter un token SonarQube (Secret text)
4. **Configure System → SonarQube servers** :
   - Name : `sonar-server`
   - Server URL : `http://sonarqube:9000` (depuis le réseau Docker Jenkins)
   - Server authentication token : le token créé dans SonarQube
5. **New Item → Pipeline** :
   - Nom : `travel`
   - Definition : Pipeline script from SCM ou copier le `Jenkinsfile` du repo
   - Script Path : `Jenkinsfile`

---

## Pipeline Jenkins (étapes)

1. **Build & Test Backend** — `mvn clean verify` sur les 6 microservices
2. **Test & Build Frontend** — `npm test` + `npm run build`
3. **Code Quality Analysis** — SonarQube (JaCoCo + lcov)
4. **Quality Gate** — validation qualité (non bloquant par défaut)
5. **Deploy** — `docker compose up -d --build` dans `infrastructure/`

---

## Tests locaux (sans Jenkins)

### Backend (un service)

```bash
cd microservices/auth-service
mvn clean verify
```

Rapport JaCoCo : `target/site/jacoco/index.html`

### Backend (tous les services)

```bash
for s in discovery-service gateway-service auth-service user-service travel-service payment-service; do
  echo "=== $s ==="
  (cd microservices/$s && mvn -q clean verify) || exit 1
done
```

### Frontend

```bash
cd frontend/travel-admin
npm run test -- --no-watch --no-progress --browsers=ChromeHeadless --code-coverage
npm run build
```

Couverture : `coverage/travel-admin/lcov.info`

---

## Tests implémentés

| Service | Tests |
|---------|-------|
| discovery-service | Context load |
| gateway-service | Context load (SSL/Eureka désactivés en test) |
| auth-service | Context load + JwtServiceTest |
| user-service | Context load (H2) |
| travel-service | Context load (H2, Neo4j exclu) |
| payment-service | Context load + PaymentServiceTest (Mockito) |
| frontend | App component spec |

Profil test : `@ActiveProfiles("test")` + `application-test.yml` (H2 en mémoire).

---

## Analyse SonarQube manuelle

```bash
# Après mvn verify + npm test --code-coverage
sonar-scanner \
  -Dsonar.host.url=http://localhost:9002 \
  -Dsonar.token=VOTRE_TOKEN
```

---

## Projet terminé

Toutes les phases du plan `travel-plan.md` sont couvertes :

| Phase | Statut |
|-------|--------|
| 1 — Architecture | ✅ |
| 2 — Infrastructure | ✅ |
| 3 — Backend | ✅ |
| 4 — Frontend | ✅ |
| 5 — CI/CD + Qualité | ✅ |
