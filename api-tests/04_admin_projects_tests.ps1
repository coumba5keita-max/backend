[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 04. Projets & Membres (ROLE_ADMIN)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         04. TESTS API : PROJETS & MEMBRES (ROLE_ADMIN)          " -ForegroundColor Cyan
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

# --- Connexion Admin ---
$adminLogin = @{
    email    = "admin@taskmanager.com"
    password = "AdminPassword123!"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLogin -ContentType "application/json; charset=utf-8"
} catch {
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

$adminHeaders = @{
    "Authorization" = "Bearer $($loginRes.token)"
    "Content-Type"  = "application/json; charset=utf-8"
}

$testUserId = $loginRes.userId
$script:testProjectId = $null
$projName = "Projet Apollo-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)

# 1. Créer un projet (POST /api/admin/projects)
Assert-Test "Creer un nouveau projet (POST /api/admin/projects)" {
    $body = @{
        name        = $projName
        description = "Projet de refonte d'infrastructure haute securite"
    } | ConvertTo-Json

    $proj = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects" -Method Post -Headers $adminHeaders -Body $body
    $script:testProjectId = $proj.id
    if (-not $proj.id -or $proj.name -ne $projName) { throw "Creation du projet echouee." }
    Write-Host "     Projet cree : ID $($proj.id), Nom: $($proj.name), Archive: $($proj.isArchived)" -ForegroundColor Gray
}

# 2. Lister les projets
Assert-Test "Recuperer la liste des projets (GET /api/admin/projects)" {
    $projects = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects" -Method Get -Headers $adminHeaders
    if ($projects.Count -eq 0) { throw "La liste des projets est vide." }
    Write-Host "     Nombre total de projets trouves : $($projects.Count)" -ForegroundColor Gray
}

# 3. Consulter les détails d'un projet
Assert-Test "Consulter un projet par ID (GET /api/admin/projects/{id})" {
    $proj = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId" -Method Get -Headers $adminHeaders
    if ($proj.id -ne $script:testProjectId) { throw "L'identifiant ne correspond pas." }
    Write-Host "     Projet verifie : ID $($proj.id), Nom: $($proj.name)" -ForegroundColor Gray
}

# 4. Modifier un projet (PUT /api/admin/projects/{id})
Assert-Test "Modifier le nom et la description d'un projet (PUT /api/admin/projects/{id})" {
    $updatedName = "$projName - V2"
    $updateBody = @{
        name        = $updatedName
        description = "Description mise a jour avec nouvelles specifications"
    } | ConvertTo-Json

    $updated = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId" -Method Put -Headers $adminHeaders -Body $updateBody
    if ($updated.name -ne $updatedName) { throw "Le nom du projet n'a pas ete modifie." }
    Write-Host "     Nouveau nom : $($updated.name)" -ForegroundColor Gray
}

# 5. Archiver un projet (PATCH /api/admin/projects/{id}/archive active=true)
Assert-Test "Archiver un projet (PATCH /api/admin/projects/{id}/archive isArchived=true)" {
    $archiveBody = @{ isArchived = $true } | ConvertTo-Json
    $archived = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/archive" -Method Patch -Headers $adminHeaders -Body $archiveBody
    if ($archived.isArchived -ne $true) { throw "Le projet n'a pas ete archive." }
    Write-Host "     Projet archive avec succes : isArchived = $($archived.isArchived)" -ForegroundColor Gray
}

# 6. Filtrer les projets archivés
Assert-Test "Filtrer les projets archives (GET /api/admin/projects?isArchived=true)" {
    $archivedProjects = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects?isArchived=true" -Method Get -Headers $adminHeaders
    $found = $archivedProjects | Where-Object { $_.id -eq $script:testProjectId }
    if (-not $found) { throw "Le projet archive n'a pas ete trouve dans la liste filtree." }
    Write-Host "     Projet retrouve dans la liste des archives." -ForegroundColor Gray
}

# 7. Désarchiver le projet
Assert-Test "Desarchiver un projet (PATCH /api/admin/projects/{id}/archive isArchived=false)" {
    $unarchiveBody = @{ isArchived = $false } | ConvertTo-Json
    $unarchived = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/archive" -Method Patch -Headers $adminHeaders -Body $unarchiveBody
    if ($unarchived.isArchived -ne $false) { throw "Le projet n'a pas ete desarchive." }
    Write-Host "     Projet desarchive avec succes : isArchived = $($unarchived.isArchived)" -ForegroundColor Gray
}

# 8. Assigner un utilisateur au projet
Assert-Test "Assigner un utilisateur au projet (POST /api/admin/projects/{projectId}/users/{userId})" {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/users/$testUserId" -Method Post -Headers $adminHeaders
    Write-Host "     Reponse serveur : $($res.message)" -ForegroundColor Gray
}

# 9. Lister les membres d'un projet
Assert-Test "Lister les membres d'un projet (GET /api/admin/projects/{projectId}/members)" {
    $members = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/members" -Method Get -Headers $adminHeaders
    if ($members.Count -ne 1) { throw "Nombre de membres incorrect (attendu: 1, obtenu: $($members.Count))" }
    Write-Host "     Membre trouve : $($members[0].firstname) $($members[0].lastname)" -ForegroundColor Gray
}

# 10. Supprimer un projet (DELETE /api/admin/projects/{id})
Assert-Test "Supprimer un projet (DELETE /api/admin/projects/{id})" {
    $del = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId" -Method Delete -Headers $adminHeaders
    Write-Host "     Reponse serveur : $($del.message)" -ForegroundColor Gray

    # Vérification que le projet n'existe plus
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId" -Method Get -Headers $adminHeaders
        throw "Le projet est toujours accessible apres suppression."
    } catch {
        Write-Host "     Suppression confirmee : le projet n'existe plus." -ForegroundColor Gray
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS PROJETS ET MEMBRES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
