[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 03. Gestion des Equipes (ROLE_ADMIN)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "         03. TESTS API : GESTION DES EQUIPES (ROLE_ADMIN)        " -ForegroundColor Cyan
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

$targetTeamId = $null
$testMemberId = $loginRes.userId
$teamName = "Equipe Agile-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)

# 1. Créer une équipe
Assert-Test "Creer une nouvelle equipe (POST /api/admin/teams)" {
    $teamBody = @{
        name        = $teamName
        description = "Equipe agile pour tests automatises"
    } | ConvertTo-Json

    $team = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams" -Method Post -Headers $adminHeaders -Body $teamBody
    $script:targetTeamId = $team.id
    if (-not $team.id -or $team.name -ne $teamName) { throw "Creation d'equipe echouee." }
    Write-Host "     Equipe creee : ID $($team.id), Nom: $($team.name), Membres: $($team.memberCount)" -ForegroundColor Gray
}

# 2. Lister toutes les équipes
Assert-Test "Lister toutes les equipes (GET /api/admin/teams)" {
    $teams = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams" -Method Get -Headers $adminHeaders
    if ($teams.Count -eq 0) { throw "La liste des equipes est vide." }
    Write-Host "     Nombre d'equipes trouvees : $($teams.Count)" -ForegroundColor Gray
}

# 3. Consulter une équipe par ID
Assert-Test "Recuperer les details d'une equipe (GET /api/admin/teams/{id})" {
    $team = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId" -Method Get -Headers $adminHeaders
    if ($team.id -ne $script:targetTeamId) { throw "L'ID ne correspond pas." }
    Write-Host "     Equipe verifiee : ID $($team.id), Nom: $($team.name)" -ForegroundColor Gray
}

# 4. Modifier une équipe
Assert-Test "Modifier le nom et la description d'une equipe (PUT /api/admin/teams/{id})" {
    $updatedName = "$teamName - RENOVAL"
    $updateBody = @{
        name        = $updatedName
        description = "Nouvelle description mise a jour"
    } | ConvertTo-Json

    $updated = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId" -Method Put -Headers $adminHeaders -Body $updateBody
    if ($updated.name -ne $updatedName) { throw "Le nom n'a pas ete modifie." }
    Write-Host "     Nouveau nom d'equipe : $($updated.name)" -ForegroundColor Gray
}

# 5. Assigner un utilisateur à l'équipe
Assert-Test "Assigner un utilisateur a l'equipe (POST /api/admin/teams/{teamId}/users/{userId})" {
    $assigned = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId/users/$testMemberId" -Method Post -Headers $adminHeaders
    if ($assigned.memberCount -ne 1) { throw "L'utilisateur n'apparait pas dans les membres." }
    Write-Host "     Utilisateur ID $testMemberId assigne. Nombre de membres : $($assigned.memberCount)" -ForegroundColor Gray
}

# 6. Lister les membres d'une équipe
Assert-Test "Lister les membres d'une equipe (GET /api/admin/teams/{id}/users)" {
    $members = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId/users" -Method Get -Headers $adminHeaders
    if ($members.Count -ne 1) { throw "Nombre de membres incorrect." }
    Write-Host "     Membre trouve : $($members[0].firstname) $($members[0].lastname) ($($members[0].email))" -ForegroundColor Gray
}

# 7. Retirer un utilisateur d'une équipe
Assert-Test "Retirer un utilisateur d'une equipe (DELETE /api/admin/teams/{teamId}/users/{userId})" {
    $removed = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId/users/$testMemberId" -Method Delete -Headers $adminHeaders
    if ($removed.memberCount -ne 0) { throw "L'utilisateur est toujours present dans l'equipe." }
    Write-Host "     Membre retire avec succes. Nombre de membres : $($removed.memberCount)" -ForegroundColor Gray
}

# 8. Affectation en bloc des membres
Assert-Test "Affectation en bloc des membres (POST /api/admin/teams/{teamId}/members)" {
    $batchBody = @{
        userIds = @($testMemberId)
    } | ConvertTo-Json

    $batchRes = Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId/members" -Method Post -Headers $adminHeaders -Body $batchBody
    if ($batchRes.memberCount -ne 1) { throw "L'affectation en bloc a echoue." }
    Write-Host "     Membres mis a jour en bloc. Nombre de membres : $($batchRes.memberCount)" -ForegroundColor Gray
}

# 9. Supprimer une équipe
Assert-Test "Supprimer une equipe (DELETE /api/admin/teams/{id})" {
    Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId" -Method Delete -Headers $adminHeaders
    
    # Vérifier que l'équipe n'existe plus
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/teams/$script:targetTeamId" -Method Get -Headers $adminHeaders
        throw "L'equipe est toujours accessible apres suppression."
    } catch {
        Write-Host "     Suppression confirmee : l'equipe n'existe plus." -ForegroundColor Gray
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS GESTION DES EQUIPES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
