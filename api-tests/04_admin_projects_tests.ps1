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
$projName = "Projet Phoenix-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)

# 1. Créer un projet
Assert-Test "Creer un nouveau projet (POST /api/projects)" {
    $body = @{
        name        = $projName
        description = "Projet dedie a la refonte globale de l'architecture"
        isArchived  = $false
    } | ConvertTo-Json

    $proj = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
    $script:testProjectId = $proj.id
    if (-not $proj.id -or $proj.name -ne $projName) { throw "Creation du projet echouee." }
    Write-Host "     Projet cree : ID $($proj.id), Nom: $($proj.name)" -ForegroundColor Gray
}

# 2. Lister les projets
Assert-Test "Recuperer tous les projets (GET /api/projects)" {
    $projects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get
    if ($projects.Count -eq 0) { throw "La liste des projets est vide." }
    Write-Host "     Nombre total de projets : $($projects.Count)" -ForegroundColor Gray
}

# 3. Consulter les détails d'un projet
Assert-Test "Consulter un projet par ID (GET /api/projects/{id})" {
    $proj = Invoke-RestMethod -Uri "$baseUrl/api/projects/$script:testProjectId" -Method Get
    if ($proj.id -ne $script:testProjectId) { throw "L'identifiant ne correspond pas." }
    Write-Host "     Projet verifie : ID $($proj.id), Nom: $($proj.name)" -ForegroundColor Gray
}

# 4. Assigner un utilisateur à un projet spécifique
Assert-Test "Assigner un utilisateur au projet (POST /api/admin/projects/{projectId}/users/{userId})" {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/users/$testUserId" -Method Post -Headers $adminHeaders
    Write-Host "     Reponse serveur : $($res.message)" -ForegroundColor Gray
}

# 5. Consulter les membres d'un projet
Assert-Test "Lister les membres d'un projet (GET /api/admin/projects/{projectId}/members)" {
    $members = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/members" -Method Get -Headers $adminHeaders
    if ($members.Count -ne 1) { throw "Nombre de membres incorrect (attendu: 1, obtenu: $($members.Count))" }
    Write-Host "     Membre trouve : $($members[0].firstname) $($members[0].lastname) ($($members[0].email))" -ForegroundColor Gray
}

# 6. Retirer un utilisateur du projet
Assert-Test "Retirer un utilisateur du projet (DELETE /api/admin/projects/{projectId}/users/{userId})" {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/users/$testUserId" -Method Delete -Headers $adminHeaders
    Write-Host "     Reponse serveur : $($res.message)" -ForegroundColor Gray

    # Vérification
    $members = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/members" -Method Get -Headers $adminHeaders
    if ($members.Count -ne 0) { throw "L'utilisateur est toujours affecte au projet !" }
    Write-Host "     Verification reussie : Aucun membre restant." -ForegroundColor Gray
}

# 7. Définir en bloc les membres d'un projet
Assert-Test "Affectation en bloc des membres (POST /api/admin/projects/{projectId}/members)" {
    $batchBody = @{
        userIds = @($testUserId)
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$baseUrl/api/admin/projects/$script:testProjectId/members" -Method Post -Headers $adminHeaders -Body $batchBody
    Write-Host "     Reponse serveur : $($res.message) (Affectes: $($res.assignedUserCount))" -ForegroundColor Gray
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS PROJETS ET MEMBRES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
