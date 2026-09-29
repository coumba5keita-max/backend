# 🚀 Guide des Tests API Automatisés (TaskManager Backend)

Ce dossier contient des scripts de test prêts à l'emploi permettant de tester **100% des endpoints de l'API** sans avoir besoin d'ouvrir ou de configurer Postman.

---

## ⚡ Comment lancer les tests ? (2 méthodes)

### Méthode 1 : En 1 Clic (Recommandé pour Windows) 🖱️
Dans l'explorateur de fichiers Windows, faites simplement un **double-clic** sur le fichier `.bat` de votre choix :

| Fichier à double-cliquer | Fonctionnalité testée |
| :--- | :--- |
| **`RUN_ALL_TESTS.bat`** | **Lance la suite complète de TOUS les tests** et affiche un tableau récapitulatif |
| `01_auth_tests.bat` | Authentification, Inscription, Login JWT, Rôles RBAC et validations |
| `02_admin_users_tests.bat` | **Actions Responsable** : Inviter, modifier, désactiver/réactiver un compte, sécurités anti-lockout |
| `03_admin_teams_tests.bat` | **Gestion des Équipes** : Création, modification, affectation de membres et suppression |
| `04_admin_projects_tests.bat` | **Gestion des Projets** : Créer, modifier, archiver/désarchiver, supprimer et affecter des collaborateurs |
| `05_tasks_tests.bat` | **Gestion des Tâches** : Création, changement de statut, assignation et suppression |
| `06_notifications_tests.bat` | **Notifications** : Consultation de l'historique et marquage comme lu |
| `07_categories_priorities_tests.bat` | **Catégories & Priorités** : Configuration des catégories (Bug, Feature, Doc) et priorités (Haute, Moyenne, Basse) |
| `08_admin_tasks_and_dashboard_tests.bat` | **Tâches Admin & Dashboard** : Attribution multiple, réaffectation, échéance, clôture, métriques et exports PDF/CSV |

> 💡 **Astuce** : La fenêtre reste ouverte à la fin de l'exécution pour vous laisser le temps de lire tranquillement les résultats.

---

### Méthode 2 : En Ligne de Commande (PowerShell) 💻
Ouvrez un terminal PowerShell dans le dossier `api-tests/` et exécutez :

```powershell
# Pour exécuter TOUS les tests d'un coup
.\RUN_ALL_TESTS.ps1

# Ou pour tester une fonctionnalité spécifique :
.\01_auth_tests.ps1
.\02_admin_users_tests.ps1
.\03_admin_teams_tests.ps1
.\04_admin_projects_tests.ps1
.\05_tasks_tests.ps1
.\06_notifications_tests.ps1
.\07_categories_priorities_tests.ps1
.\08_admin_tasks_and_dashboard_tests.ps1
```

---

## 🔍 Ce que chaque script vérifie

### 1. `01_auth_tests.ps1` (Authentification)
- Inscription d'un utilisateur standard (`POST /api/auth/register`)
- Rejet des doublons d'email (400 Bad Request)
- Connexion avec identifiants valides et génération du token JWT (`POST /api/auth/login`)
- Rejet des mauvais mots de passe (401 Unauthorized)
- Création et connexion avec rôle `ROLE_ADMIN`

### 2. `02_admin_users_tests.ps1` (Comptes Utilisateurs - ROLE_ADMIN)
- Protection RBAC : Rejet des accès non autorisés (403 Forbidden)
- Invitation d'un utilisateur avec génération de mot de passe fort (`POST /api/admin/users/invite`)
- Liste et consultation de tous les comptes (`GET /api/admin/users`, `GET /api/admin/users/{id}`)
- Modification des données d'un collaborateur (`PUT /api/admin/users/{id}`)
- Assignation et retrait d'un collaborateur à un projet
- Suspension / Désactivation d'un compte (`PATCH /api/admin/users/{id}/status` avec `active=false`)
- **Sécurité** : Rejet immédiat de connexion pour compte désactivé (403 Forbidden)
- **Sécurité** : Protection anti-verrouillage (un admin ne peut pas désactiver son propre compte)
- Réactivation d'un compte (`active=true`)

### 3. `03_admin_teams_tests.ps1` (Équipes - ROLE_ADMIN)
- Création d'une nouvelle équipe (`POST /api/admin/teams`)
- Liste des équipes et détails (`GET /api/admin/teams`, `GET /api/admin/teams/{id}`)
- Modification du nom et de la description (`PUT /api/admin/teams/{id}`)
- Ajout d'un membre à une équipe (`POST /api/admin/teams/{teamId}/users/{userId}`)
- Consultation des membres d'une équipe (`GET /api/admin/teams/{id}/users`)
- Retrait d'un membre (`DELETE /api/admin/teams/{teamId}/users/{userId}`)
- Affectation en bloc des membres (`POST /api/admin/teams/{teamId}/members`)
- Suppression d'une équipe (`DELETE /api/admin/teams/{id}`)

### 4. `04_admin_projects_tests.ps1` (Gestion Complète des Projets - ROLE_ADMIN)
- Création d'un projet (`POST /api/admin/projects`)
- Consultation de la liste et filtrage par état (`GET /api/admin/projects`, `GET /api/admin/projects?isArchived=true`)
- Consultation détaillée par ID (`GET /api/admin/projects/{id}`)
- Modification du nom et de la description (`PUT /api/admin/projects/{id}`)
- Archivage et désarchivage d'un projet (`PATCH /api/admin/projects/{id}/archive`)
- Assignation d'un collaborateur au projet (`POST /api/admin/projects/{projectId}/users/{userId}`)
- Consultation des membres affectés (`GET /api/admin/projects/{projectId}/members`)
- Retrait d'un membre (`DELETE /api/admin/projects/{projectId}/users/{userId}`)
- Suppression d'un projet avec dissociation sécurisée (`DELETE /api/admin/projects/{id}`)

### 5. `05_tasks_tests.ps1` (Tâches)
- Création d'une tâche avec assignation (`POST /api/tasks`)
- Liste de toutes les tâches (`GET /api/tasks`)
- Consultation par ID (`GET /api/tasks/{id}`)
- Transition de statut : `A_FAIRE` ➡️ `EN_COURS` ➡️ `TERMINE` (`PATCH /api/tasks/{id}/status`)
- Suppression de la tâche avec détachement sécurisé (`DELETE /api/tasks/{id}`)

### 6. `06_notifications_tests.ps1` (Notifications)
- Protection d'accès aux notifications (403 sans token)
- Consultation de l'historique des notifications (`GET /api/notifications`)
- Marquage d'une notification comme lue (`PATCH /api/notifications/{id}/read`)

### 7. `07_categories_priorities_tests.ps1` (Catégories & Priorités de Tâches)
- Consultation des catégories par défaut auto-initialisées (`GET /api/categories`) : Bug, Feature, Documentation, Amélioration
- Consultation des priorités configurées avec couleurs et niveaux (`GET /api/categories/priorities`) : Haute, Moyenne, Basse
- Création d'une nouvelle catégorie par l'administrateur (`POST /api/admin/categories`)
- Rejet des doublons de catégorie avec contrôle d'intégrité
- Modification d'une catégorie existante (`PUT /api/admin/categories/{id}`)
- Sécurité RBAC : Refus d'accès pour les utilisateurs réguliers (403 Forbidden)
- Suppression d'une catégorie avec détachement sécurisé des tâches associées (`DELETE /api/admin/categories/{id}`)

### 8. `08_admin_tasks_and_dashboard_tests.ps1` (Tâches, Dashboard & Reporting)
- Attribution d'une tâche à un collaborateur spécifique (ex : Aboubacar) (`POST /api/admin/tasks`)
- Attribution d'une tâche à plusieurs collaborateurs simultanés dont l'administrateur
- Réaffectation d'une tâche d'un membre à un autre (`PUT /api/admin/tasks/{id}/reassign`) avec notification temps réel
- Modification de la date d'échéance (Due date) (`PATCH /api/admin/tasks/{id}/due-date`) avec notification
- Clôture définitive d'un ticket (`PATCH /api/admin/tasks/{id}/close`) avec horodatage et note de résolution
- Filtrage des tâches clôturées (`GET /api/admin/tasks?status=CLOTURE`)
- Consultation des métriques globales du dashboard (`GET /api/admin/dashboard/metrics`) : total tâches, clôturées, retards, taux d'achèvement et charge de travail (%) par collaborateur
- Export de rapport d'activité au format **CSV** compatible Excel avec BOM UTF-8 (`GET /api/admin/reports/export/csv`)
- Export de rapport d'activité au format **PDF** élégant paysage (`GET /api/admin/reports/export/pdf`)
- Sécurité RBAC : Refus d'accès au dashboard et exports pour `ROLE_USER` (403 Forbidden)



