#!/usr/bin/env bash
# ============================================================
# run-ansible.sh — Lance les playbooks Ansible de Travel
#
# Usage :
#   ./scripts/run-ansible.sh               # site.yml complet
#   ./scripts/run-ansible.sh prerequisites # prérequis seuls
#   ./scripts/run-ansible.sh deploy        # déploiement seul
#   ./scripts/run-ansible.sh --check       # dry-run (aucun changement)
#
# Prérequis :
#   - ansible-core >= 2.14  ET  collection ansible.posix
#   - OU : pip3 install --user --break-system-packages ansible-core
#          ansible-galaxy collection install ansible.posix
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ANSIBLE_DIR="${PROJECT_ROOT}/ansible"

# ── Résolution du binaire ansible-playbook ─────────────────
ANSIBLE_BIN=""
for candidate in \
    ansible-playbook \
    "${HOME}/.local/bin/ansible-playbook" \
    /usr/local/bin/ansible-playbook \
    /usr/bin/ansible-playbook; do
    if command -v "${candidate}" &>/dev/null 2>&1 || [ -x "${candidate}" ]; then
        ANSIBLE_BIN="${candidate}"
        break
    fi
done

if [ -z "${ANSIBLE_BIN}" ]; then
    echo "ERREUR : ansible-playbook introuvable."
    echo "Installez-le avec :"
    echo "  pip3 install --user --break-system-packages ansible-core"
    echo "  ansible-galaxy collection install ansible.posix"
    exit 1
fi

echo "ansible-playbook : ${ANSIBLE_BIN}"
"${ANSIBLE_BIN}" --version | head -1

# ── Sélection du playbook ───────────────────────────────────
PLAYBOOK="site.yml"
EXTRA_ARGS=()

for arg in "$@"; do
    case "${arg}" in
        prerequisites) PLAYBOOK="prerequisites.yml" ;;
        deploy)        PLAYBOOK="deploy.yml" ;;
        --check|-C)    EXTRA_ARGS+=("--check") ;;
        *)             EXTRA_ARGS+=("${arg}") ;;
    esac
done

echo ""
echo "Lancement : ${PLAYBOOK}"
echo "-----------------------------------------------------------"

cd "${ANSIBLE_DIR}"
exec "${ANSIBLE_BIN}" "playbooks/${PLAYBOOK}" "${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}"
