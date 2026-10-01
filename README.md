# Bienvenue sur le Backend TaskManager !

Salut collègue coumba ! 
Bienvenue dans l'équipe et sur le projet **TaskManager**. Ce dépôt contient l'API backend complète conçue avec **Spring Boot 3 (Java 17)** et **PostgreSQL 16**.

Ce guide a été rédigé spécialement pour toi. En quelques étapes simples, tu vas pouvoir installer les outils, démarrer tout l'environnement sans prise de tête grâce à Docker, et exécuter la suite de tests automatisés pour vérifier que tout tourne comme une horloge sur ta machine.

Installe-toi confortablement avec un café ☕, et suis le guide !

---

## Table des matières

1. [Prérequis](#-1-prérequis)
2. [Télécharger et installer Docker](#-2-télécharger-et-installer-docker)
3. [Démarrer l'application avec Docker](#-3-démarrer-lapplication-avec-docker)
4. [Lancer les tests de l'API (Zéro configuration Postman requise)](#-4-lancer-les-tests-de-lapi)
5. [Guide pratique du Collaborateur (Ce que tu peux faire)](#-5-guide-pratique-du-collaborateur)
6. [Commandes utiles & Astuces du quotidien](#-6-commandes-utiles--astuces-du-quotidien)
7. [Dépannage fréquent (Troubleshooting)](#-7-dépannage-fréquent)

---

## 1. Prérequis

Pour travailler sereinement, tu as simplement besoin de :
- Un système d'exploitation moderne (**Windows 10/11**, macOS ou Linux).
- **Git** installé pour cloner le projet.
- **Docker Desktop** (on t'explique comment l'obtenir juste en dessous).

> **Bonne nouvelle :** Tu n'as même pas besoin d'installer Java, Maven ou PostgreSQL sur ta machine ! Docker se charge d'encapsuler tout le système pour toi.

---

## 2. Télécharger et installer Docker

Docker permet de lancer en une seule commande la base de données PostgreSQL et l'application Spring Boot dans des conteneurs isolés et prêts à l'emploi.

### Téléchargement

1. Rends-toi sur le site officiel de Docker :
   **[Télécharger Docker Desktop pour Windows / Mac / Linux](https://www.docker.com/products/docker-desktop/)**
2. Clique sur le bouton correspondant à ton système (ex: **Docker Desktop for Windows**).
3. Lance l'installateur téléchargé (`Docker Desktop Installer.exe`).

### Conseils d'installation (Spécial Windows)
- Durant l'installation, assure-toi que l'option **"Use WSL 2 instead of Hyper-V"** est bien cochée (recommandé pour de meilleures performances).
- Une fois l'installation terminée, redémarre ton ordinateur si l'installateur te le demande.
- Lance **Docker Desktop** depuis le menu Démarrer.
- Attends quelques instants jusqu'à ce que la petite icône de baleine en bas à gauche de la fenêtre Docker Desktop devienne verte avec la mention **"Engine running"**.

### Vérifier que Docker fonctionne
Ouvre ton terminal favori (PowerShell, Invite de commandes ou Terminal) et tape :

```bash
docker --version
docker compose version
```

Si ces commandes affichent les numéros de version, bravo, Docker est fin prêt ! 🎉

---

## 3. Démarrer l'application avec Docker

Tu es maintenant prêt à lancer le projet. Rien de plus facile !

### 1. Ouvre un terminal à la racine du projet
Navigue dans le dossier du projet backend :

```powershell
cd c:\developpement\backend
```

### 2. Lance la commande magique

```bash
docker compose up --build -d
```

### Que fait cette commande ?
- `--build` : Compile le code Java et fabrique l'image de l'application Spring Boot.
- `-d` (*detached mode*) : Lance les conteneurs en arrière-plan pour ne pas bloquer ton terminal.
- Démarre automatiquement deux conteneurs reliés entre eux :
  1. `postgres_db` : Base PostgreSQL 16 (accessible sur le port `5432`).
  2. `springboot_app` : L'API Spring Boot (accessible sur le port `8080`).

### 3. Vérifier que tout est en ligne
Tape la commande suivante pour voir l'état des conteneurs :

```bash
docker compose ps
```

Tu devrais voir :
- `postgres_db` avec le statut `Up (healthy)`
- `springboot_app` avec le statut `Up`

Tu peux également jeter un œil aux logs en direct du serveur avec :

```bash
docker compose logs -f app
```
*(Fais `Ctrl + C` pour quitter les logs quand tu veux).*

L'API est désormais disponible sur : **`http://localhost:8080`** 🎯

---

## 4. Lancer les tests de l'API

Pour te simplifier la vie au maximum et t'éviter d'avoir à importer et configurer des dizaines de requêtes dans Postman, une suite complète de tests prêts à l'emploi est incluse dans le dossier `api-tests/`.

Deux options s'offrent à toi :

### Option A : En 1 Clic (Ultra simple sous Windows)
Ouvre ton explorateur de fichiers dans `backend\api-tests\` et **double-clique** simplement sur :
- **`RUN_ALL_TESTS.bat`** : Exécute l'intégralité des 9 suites de test et affiche un beau résumé récapitulatif !

Tu peux aussi double-cliquer sur un fichier spécifique si tu veux tester une seule partie (ex: `09_collaborator_user_tests.bat`).

---

### Option B : En ligne de commande (PowerShell)
Depuis la racine du projet ou depuis le dossier `api-tests/`, lance :

```powershell
# Pour exécuter TOUS les tests d'un coup (9 suites au total)
.\api-tests\RUN_ALL_TESTS.ps1
```

Ou pour cibler un test précis :
```powershell
.\api-tests\01_auth_tests.ps1                     # Authentification & Inscription
.\api-tests\02_admin_users_tests.ps1              # Gestion des utilisateurs (Admin)
.\api-tests\03_admin_teams_tests.ps1              # Gestion des équipes
.\api-tests\04_admin_projects_tests.ps1           # Gestion des projets & membres
.\api-tests\05_tasks_tests.ps1                    # Gestion des tâches
.\api-tests\06_notifications_tests.ps1            # Notifications en temps réel
.\api-tests\07_categories_priorities_tests.ps1    # Catégories & Priorités
.\api-tests\08_admin_tasks_and_dashboard_tests.ps1# Dashboard & Exports PDF/CSV
.\api-tests\09_collaborator_user_tests.ps1        # Espace Collaborateur (ROLE_USER)
```

> **Résultat attendu :** 100% des tests en vert avec le message `Validation réussie !`.

---

## 5. Guide pratique du Collaborateur

En tant que collaborateur (`ROLE_USER`), voici comment fonctionne l'application pour toi au quotidien :

### 1. Tes Projets Assignés
- Dès qu'un responsable t'affecte à un projet, celui-ci apparaît dans ta liste via `GET /api/user/projects`.
- Tu peux voir en un coup d'œil la description, le nombre de membres et le nombre de tâches rattachées.

### 2. Tes Tâches et Tickets
- Tu as accès à **tous les tickets des projets dont tu fais partie**, ainsi qu'aux tâches qui te sont directement assignées (`GET /api/user/tasks`).
- Tu peux filtrer par statut, priorité, mot-clé ou projet.
- **Faire avancer un ticket** : Tu peux changer l'état d'une tâche à tout moment via `PATCH /api/user/tasks/{id}/status` :
  - `A_FAIRE` (À faire)
  - `EN_COURS` (En cours de traitement)
  - `TERMINE` (Terminé, prêt pour revue)
  *(Note : La clôture définitive `CLOTURE` reste l'apanage des administrateurs).*

### 3. Gestion de ton Temps de Travail
- Tu peux estimer ou saisir les heures réellement passées sur un ticket avec `PATCH /api/user/tasks/{id}/time`.

### 4. Communication & Pièces Jointes
- **Commentaires** : Pose une question ou partage un point d'avancement directement sur le ticket via `POST /api/tasks/{id}/comments`.
- **Fichiers joints** : Téléverse des captures d'écran, schémas ou logs via `POST /api/tasks/{id}/attachments` (formats images, PDF, textes supportés).

### 5. Notifications
- Dès qu'une tâche t'est assignée ou qu'un statut évolue, tu reçois une alerte consultable sur `GET /api/notifications`.

---

## 6. Commandes utiles & Astuces du quotidien

| Action voulue | Commande à exécuter |
| :--- | :--- |
| **Démarrer l'application** | `docker compose up -d` |
| **Arrêter l'application** | `docker compose down` |
| **Reconstruire après une modif de code** | `docker compose up --build -d` |
| **Voir les logs de l'API en direct** | `docker compose logs -f app` |
| **Voir les logs de la base PostgreSQL** | `docker compose logs -f db` |
| **Vérifier l'état des conteneurs** | `docker compose ps` |
| **Redémarrer uniquement l'API Spring** | `docker compose restart app` |
| **Nettoyer complètement la base de données** | `docker compose down -v` *(attention : réinitialise les données)* |

---

## 7. Dépannage fréquent

### Le port 8080 est déjà utilisé ?
Si une autre application (ex: un Tomcat local ou un autre service) tourne déjà sur le port 8080 :
- Arrête l'application en conflit, ou
- Modifie le port dans `docker-compose.yml` (par exemple `"8081:8080"`).

### Docker Desktop ne démarre pas sous Windows ?
- Vérifie que la virtualisation matérielle (VT-x ou AMD-V) est bien activée dans le BIOS de ton PC.
- Exécute `wsl --update` dans un terminal administrateur pour mettre à jour le sous-système Linux.

### Les tests échouent avec une erreur de connexion ?
- Assure-toi que le conteneur `springboot_app` est bien démarré (`docker compose ps`).
- Attends 5 à 10 secondes après le démarrage du conteneur pour laisser à Spring Boot le temps de charger le contexte et la base.

---

## Besoin d'aide ?

Tu bloques sur quelque chose ou tu as une suggestion d'amélioration ? N'hésite pas à échanger avec l'équipe !  
