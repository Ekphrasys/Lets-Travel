# Ansible — Travel Management System

Automatisation du déploiement conforme à l'audit (sans bonus).

## Structure

```
ansible/
├── ansible.cfg
├── inventory/
│   ├── hosts.ini          # localhost ansible_connection=local
│   └── group_vars/
│       └── all.yml        # Variables partagées (project_root, env_file)
└── playbooks/
    ├── site.yml           # Point d'entrée (prérequis + déploiement)
    ├── prerequisites.yml  # sysctl SonarQube, vérification Docker/Compose
    └── deploy.yml         # docker compose up via deploy-stack.sh
```

## Prérequis

### Option 1 — pip (sans sudo)

```bash
pip3 install --user --break-system-packages ansible-core
ansible-galaxy collection install ansible.posix
```

### Option 2 — apt (avec sudo)

```bash
sudo apt install ansible-core
ansible-galaxy collection install ansible.posix
```

## Lancer les playbooks

Le script wrapper `scripts/run-ansible.sh` détecte automatiquement le binaire ansible-playbook installé :

```bash
# Playbook complet (prérequis + déploiement)
./scripts/run-ansible.sh

# Prérequis seuls (sysctl + vérifications Docker)
./scripts/run-ansible.sh prerequisites

# Déploiement seul
./scripts/run-ansible.sh deploy

# Dry-run (aucun changement appliqué)
./scripts/run-ansible.sh --check
```

Ou directement :

```bash
cd ansible
ansible-playbook playbooks/site.yml
```

## Idempotence

Relancer les playbooks **ne casse rien** :

- `vm.max_map_count` : valeur lue avant toute modification ; si déjà à 262144, la tâche est **skippée** (pas de sudo requis).
- `.env` : Ansible échoue explicitement si absent, ne le modifie jamais.
- `docker compose up -d` : ne recrée que les conteneurs dont la configuration a changé.

Résultat typique d'une deuxième exécution :
```
localhost : ok=11  changed=1  unreachable=0  failed=0  skipped=2
```

## Expliquer à l'oral (audit)

| Playbook | Rôle | Idempotent |
|----------|------|-----------|
| `prerequisites.yml` | Configure `vm.max_map_count` pour SonarQube et vérifie Docker/Compose | Oui — lit la valeur avant de modifier |
| `deploy.yml` | Vérifie que `.env` existe, lance `scripts/deploy-stack.sh` | Oui — `docker compose up -d` est no-op si rien n'a changé |
| `site.yml` | Orchestre les deux dans l'ordre | Oui |
