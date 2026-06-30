# CI/CD — Configuration Jenkins + SonarQube (audit)

> Répond aux critères audit : tests unitaires, pipeline PR/build, SonarQube sans erreurs bloquantes

---

## Critères audit couverts

| Critère audit | Implémentation |
|---------------|----------------|
| Tests unitaires par fonctionnalité | `mvn verify` × 6 services + `npm test` Angular |
| Tests à chaque build/PR | Job Jenkins `travel` — Git SCM `master` + poll SCM (5 min) ou webhook Gitea |
| SonarQube sans erreur bloquante | Quality Gate **Travel Audit Gate** (bugs=0, vulnérabilités=0) |
| Pipeline build / test / deploy | `Jenkinsfile` — 5 stages |
| Logs SonarQube propres | Exclusions `node_modules`, `target`, `jenkins_home` |

---

## Installation automatique (recommandé)

```bash
cd /home/alex/zone01/Lets-Travel
chmod +x scripts/*.sh
./scripts/setup-ci.sh
```

Ce script :
1. Configure `vm.max_map_count` (SonarQube)
2. Lance Docker Compose CI (`jenkins-compose.yml`)
3. Crée le projet SonarQube `travel`
4. Crée la Quality Gate **Travel Audit Gate**
5. Génère un token SonarQube → fichier `.env.ci`
6. Configure le dépôt Git détecté + redémarre Jenkins (JCasC, job `travel` en cpsScm)

---

## URLs

| Service | URL | Identifiants |
|---------|-----|--------------|
| Jenkins | http://localhost:9092 | Compte créé au 1er accès (wizard désactivé si JCasC) |
| SonarQube | http://localhost:9002 | `admin` / voir `.env.ci` |

---

## Architecture CI

```
┌─────────────┐   poll SCM / webhook   ┌──────────────┐
│ Gitea master│ ─────────────────────► │   Jenkins    │
│ travel-plan │   git clone + Jenkinsfile            │
└─────────────┘                        │  job: travel │
                                       └──────┬───────┘
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              ▼                          ▼                          ▼
        mvn verify                  npm test/build              sonar-scanner
        (6 services)              (Angular)                   (JaCoCo + lcov)
              │                          │                          │
              └──────────────────────────┴──────────────────────────┘
                                         │
                                         ▼
                              ┌──────────────────┐
                              │    SonarQube     │
                              │  Quality Gate    │
                              └────────┬─────────┘
                                       │ OK
                                       ▼
                              deploy-stack.sh (Docker Compose)
```

---

## Fichiers de configuration

| Fichier | Rôle |
|---------|------|
| `Jenkinsfile` | Pipeline declarative |
| `jenkins-compose.yml` | Jenkins + SonarQube Docker |
| `ci/jenkins/casc.yaml` | JCasC : plugins, tools, Sonar server, job `travel` |
| `ci/jenkins/plugins.txt` | Plugins Jenkins requis |
| `sonar-project.properties` | Config scanner locale |
| `scripts/setup-ci.sh` | Setup complet |
| `scripts/setup-sonarqube.sh` | Projet + token + quality gate |
| `scripts/deploy-stack.sh` | Déploiement Docker unifié (Jenkins + Ansible) |
| `scripts/setup-jenkins-git.sh` | Credentials Gitea + job Git SCM |

---

## Configuration manuelle (si setup auto échoue)

### 1. SonarQube

1. Ouvrir http://localhost:9002 → login `admin` / `admin`
2. Changer le mot de passe
3. **My Account → Security → Generate Token** (nom : `jenkins-travel`)
4. **Quality Gates → Create** : `Travel Audit Gate`
   - Bugs > 0 → Error
   - Vulnerabilities > 0 → Error
5. **Projects → travel → Project Settings → Quality Gate** → `Travel Audit Gate`

### 2. Jenkins

**Manage Jenkins → Plugins** (si pas JCasC) :
- SonarQube Scanner
- Pipeline
- Maven Integration
- NodeJS

**Manage Jenkins → Tools** :
| Outil | Nom exact (Jenkinsfile) |
|-------|-------------------------|
| JDK | `jdk-17` |
| Maven | `maven-3` |
| NodeJS | `node-22` |
| SonarQube Scanner | `sonar-scanner` |

**Manage Jenkins → Credentials → Secret text** :
- ID : `sonar-token`
- Secret : token SonarQube

**Manage Jenkins → System → SonarQube servers** :
- Name : `sonar-server` *(identique à Jenkinsfile)*
- URL : `http://sonarqube:9000`
- Token : credential `sonar-token`

**New Item → Pipeline `travel`** :
- Definition : Pipeline script from SCM
- Repository : `https://github.com/Ekphrasys/Lets-Travel.git` (ou l'URL de votre dépôt)
- Credentials : `git-travel-repo`
- Branches : `*/master`
- Script Path : `Jenkinsfile`

### 3. Variables d'environnement Jenkins

Dans `.env.ci` (généré par les scripts, non versionné) :

```bash
SONAR_TOKEN=squ_xxxxxxxx
GIT_USERNAME=ajoly
GIT_TOKEN=xxxxxxxx
```

### 4. Webhook Gitea (optionnel)

Si Jenkins est joignable depuis `zone01normandie.org` (IP publique, VPN, tunnel) :

1. Gitea → repo **travel-plan** → **Settings → Webhooks → Add Webhook**
2. URL : `http://<IP_JENKINS>:9092/gitea-webhook/post`
3. Content-Type : `application/json`
4. Événement : **Push**

Sinon, le **poll SCM** (`H/5 * * * *`) détecte un merge sur `master` en ≤ 5 minutes.

---

## Build automatique après merge

1. Merger sur `master` (ex. PR `cursor/fix-sonar-scanner-sources`)
2. Jenkins poll Gitea toutes les 5 min **ou** webhook immédiat si configuré
3. Clone `master` → exécute `Jenkinsfile` → deploy

Première config Git : `./scripts/setup-jenkins-git.sh`

---

## Lancer un build manuel

1. Jenkins → **travel** → **Build Now**
2. Vérifier les stages :
   - Build & Test Backend ✅
   - Test & Build Frontend ✅
   - Code Quality Analysis ✅ (SonarQube + Quality Gate)
   - Deploy ✅

3. SonarQube → **Projects → travel** → voir bugs, vulnérabilités, couverture

---

## Réponses oral audit

**« Décrivez votre pipeline CI/CD »**
> Jenkins poll le repo toutes les 5 min. Le pipeline compile et teste les 6 microservices Maven avec JaCoCo, teste et build l'Angular avec couverture, envoie l'analyse à SonarQube (Quality Gate via `sonar.qualitygate.wait=true`), puis déploie via `scripts/deploy-stack.sh` (Docker Compose).

**« SonarQube bloque-t-il le pipeline ? »**
> Oui, le scanner attend la Quality Gate (`-Dsonar.qualitygate.wait=true`). La gate **Travel Audit Gate** exige 0 bugs et 0 vulnérabilités.

**« Les tests tournent-ils à chaque PR ? »**
> Merge sur `master` → Jenkins clone le repo (Git SCM) et exécute le pipeline (poll SCM 5 min ou webhook Gitea). Chaque build lance `mvn verify` et `npm test`. Pas de Multibranch Pipeline dédié aux PR.

---

## Dépannage

| Problème | Solution |
|----------|----------|
| SonarQube ne démarre pas | `sudo sysctl -w vm.max_map_count=262144` |
| Quality Gate timeout | Attendre fin analyse SonarQube (~2 min) |
| `SONAR_AUTH_TOKEN` vide | Relancer `./scripts/setup-ci.sh` ou configurer credential |
| Maven/Node introuvable | Vérifier JCasC ou Global Tool Configuration |
| Job `travel` absent | `./scripts/setup-ci.sh` ou `./scripts/setup-jenkins-git.sh` |
| Clone Git échoue (401) | `./scripts/setup-jenkins-git.sh` — token Gitea dans `.env.ci` |
| Build ne part pas après merge | Vérifier branche `master` ; attendre poll SCM ou configurer webhook |
