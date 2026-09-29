[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 02. Gestion des Comptes (ROLE_ADMIN)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         02. TESTS API : GESTION DES COMPTES (ROLE_ADMIN)        " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Serveur cible : http://localhost:8080" -ForegroundColor Gray
Write-Host ""

$baseUrl = "http://localhost:8080"
$successCount = 0
$totalTests = 0

function Assert-Test {
    param(
        [string]$TestName,
        [scriptblock]$Action
    )
    $script:totalTests++
    Write-Host "[TEST $script:totalTests] $TestName..." -ForegroundColor Yellow
    try {
        & $Action
        Write-Host "  -> SUCCES : Validation reussie !" -ForegroundColor Green
        $script:successCount++
    } catch {
        Write-Host "  -> ECHEC  : $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                Write-Host "  -> Details serveur : $($reader.ReadToEnd())" -ForegroundColor DarkRed
            }
        }
    }
    Write-Host ""
}

# --- Preparation de l'environnement : Connexion Admin ---
$adminLogin = @{
    email    = "admin@taskmanager.com"
    password = "AdminPassword123!"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json; charset=utf-8"
} catch {
    # Créer le compte admin s'il n'existe pas encore
    $createAdmin = @{
        firstname = "Admin"
        lastname  = "Manager"
        email     = "admin@taskmanager.com"
        password  = "AdminPassword123!"
        role      = "ROLE_ADMIN"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createAdmin -ContentType "application/json; charset=utf-8"
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json; charset=utf-8"
}

$adminToken = $loginRes.token
$adminId = $loginRes.userId
$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type"  = "application/json; charset=utf-8"
}
Write-Host "Connecte en tant qu'administrateur : ID $adminId ($($loginRes.email))" -ForegroundColor Gray
Write-Host ""

$invitedUserId = $null
$newEmail = "membre." + [System.Guid]::NewGuid().ToString().Substring(0, 8) + "@test.com"

# 1. Protection RBAC (Accès sans token bloqué)
Assert-Test "Protection RBAC : Rejet des requetes non authentifiees (GET /api/admin/users)" {
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/users" -Method Get
        throw "L'endpoint aurait du bloquer la requete sans token."
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden) {
            Write-Host "     Rejet 403 Forbidden confirme." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 2. Inviter un utilisateur
Assert-Test "Inviter un utilisateur avec mot de passe temporaire (POST /api/admin/users/invite)" {
    $inviteBody = @{
        firstname = "Alice"
        lastname  = "Durand"
        email     = $newEmail
        role      = "ROLE_USER"
    } | ConvertTo-Json

    $user = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/invite" -Method Post -Headers $adminHeaders -Body $inviteBody
    $script:invitedUserId = $user.id
    if (-not $user.id -or $user.email -ne $newEmail) { throw "Reponse inattendue lors de l'invitation." }
    Write-Host "     Utilisateur cree : ID $($user.id), Email $($user.email), Actif: $($user.isActive)" -ForegroundColor Gray
}

# 3. Consulter la liste des utilisateurs
Assert-Test "Lister tous les comptes utilisateurs (GET /api/admin/users)" {
    $users = Invoke-RestMethod -Uri "$baseUrl/api/admin/users" -Method Get -Headers $adminHeaders
    if ($users.Count -eq 0) { throw "La liste des utilisateurs est vide." }
    Write-Host "     Nombre d'utilisateurs recuperes : $($users.Count)" -ForegroundColor Gray
}

# 4. Consulter les détails d'un utilisateur par ID
Assert-Test "Recuperer un compte par son identifiant (GET /api/admin/users/{id})" {
    $user = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId" -Method Get -Headers $adminHeaders
    if ($user.id -ne $script:invitedUserId) { throw "L'ID ne correspond pas." }
    Write-Host "     Utilisateur verifie : $($user.firstname) $($user.lastname) ($($user.email))" -ForegroundColor Gray
}

# 5. Modifier un compte utilisateur
Assert-Test "Modifier les informations d'un compte (PUT /api/admin/users/{id})" {
    $updateBody = @{
        firstname = "Alice-Marie"
        lastname  = "Durand-Gauthier"
        email     = $newEmail
        role      = "ROLE_USER"
    } | ConvertTo-Json

    $updated = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId" -Method Put -Headers $adminHeaders -Body $updateBody
    if ($updated.firstname -ne "Alice-Marie") { throw "Le prenom n'a pas ete mis a jour." }
    Write-Host "     Nouveau nom : $($updated.firstname) $($updated.lastname)" -ForegroundColor Gray
}

# 6. Assigner et retirer un projet directement depuis l'utilisateur
Assert-Test "Assigner puis retirer un utilisateur d'un projet (POST/DELETE /api/admin/users/{userId}/projects/{projectId})" {
    $projects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get
    if ($projects.Count -eq 0) {
        $newProj = @{ name = "Projet Test"; description = "Description" } | ConvertTo-Json
        $proj = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Body $newProj -ContentType "application/json; charset=utf-8"
        $projId = $proj.id
    } else {
        $projId = $projects[0].id
    }

    # Assigner
    $assign = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId/projects/$projId" -Method Post -Headers $adminHeaders
    Write-Host "     Assignation projet reussie : Projets = $($assign.projectNames -join ', ')" -ForegroundColor Gray

    # Retirer
    $remove = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId/projects/$projId" -Method Delete -Headers $adminHeaders
    Write-Host "     Retrait projet reussi." -ForegroundColor Gray
}

# 7. Désactiver un compte utilisateur
Assert-Test "Desactiver un compte utilisateur (PATCH /api/admin/users/{id}/status active=false)" {
    $statusBody = @{ active = $false } | ConvertTo-Json
    $disabled = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId/status" -Method Patch -Headers $adminHeaders -Body $statusBody
    if ($disabled.isActive -ne $false) { throw "Le compte n'est pas desactive." }
    Write-Host "     Statut compte confirme : Actif = $($disabled.isActive)" -ForegroundColor Gray
}

# 8. Rejet immédiat de connexion pour compte désactivé
Assert-Test "Blocage immediat de l'acces pour un compte desactive (POST /api/auth/login)" {
    $badLogin = @{
        email    = $newEmail
        password = "AnyPassword123!"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $badLogin -ContentType "application/json; charset=utf-8"
        throw "La connexion avec un compte desactive aurait du etre bloquee."
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden) {
            Write-Host "     Rejet 403 Forbidden confirme pour le compte desactive." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 9. Protection anti-verrouillage : l'admin ne peut pas se désactiver lui-même
Assert-Test "Protection anti-verrouillage (Self-lockout) : tentative d'auto-desactivation rejetee" {
    $selfDisableBody = @{ active = $false } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$adminId/status" -Method Patch -Headers $adminHeaders -Body $selfDisableBody
        throw "L'administrateur a reussi a se desactiver lui-meme !"
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Forbidden) {
            Write-Host "     Rejet 403 confirme : Auto-desactivation strictement interdite." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 10. Réactivation du compte utilisateur
Assert-Test "Reactivation d'un compte utilisateur (PATCH /api/admin/users/{id}/status active=true)" {
    $statusBody = @{ active = $true } | ConvertTo-Json
    $reactivated = Invoke-RestMethod -Uri "$baseUrl/api/admin/users/$script:invitedUserId/status" -Method Patch -Headers $adminHeaders -Body $statusBody
    if ($reactivated.isActive -ne $true) { throw "Le compte n'a pas ete reactive." }
    Write-Host "     Statut compte confirme : Actif = $($reactivated.isActive)" -ForegroundColor Gray
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS GESTION DES COMPTES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
