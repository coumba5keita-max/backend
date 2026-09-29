[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 07. Categories & Priorites (ROLE_ADMIN)"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "       07. TESTS API : CATEGORIES & PRIORITES (ROLE_ADMIN)       " -ForegroundColor Cyan
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

# --- Connexion Utilisateur Standard (pour tests de sécurité RBAC) ---
$userEmail = "dev.categories@taskmanager.com"
$userPassword = "UserPassword123!"
$userLogin = @{ email = $userEmail; password = $userPassword } | ConvertTo-Json

try {
    $userLoginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $userLogin -ContentType "application/json; charset=utf-8"
} catch {
    $createUser = @{
        firstname = "Dev"
        lastname  = "Tester"
        email     = $userEmail
        password  = $userPassword
        role      = "ROLE_USER"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $createUser -ContentType "application/json; charset=utf-8"
    $userLoginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $userLogin -ContentType "application/json; charset=utf-8"
}

$userHeaders = @{
    "Authorization" = "Bearer $($userLoginRes.token)"
    "Content-Type"  = "application/json; charset=utf-8"
}

$script:testCatId = $null
$catName = "DevOps-" + [System.Guid]::NewGuid().ToString().Substring(0, 6)

# 1. Lister les catégories par défaut
Assert-Test "Recuperer la liste des categories (GET /api/categories)" {
    $categories = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method Get -Headers $adminHeaders
    if ($categories.Count -eq 0) { throw "La liste des categories est vide." }
    Write-Host "     Categories existantes ($($categories.Count)) : $(($categories | ForEach-Object { $_.name }) -join ', ')" -ForegroundColor Gray
}

# 2. Configurer et consulter les priorités de tâches (Haute, Moyenne, Basse)
Assert-Test "Consulter les priorites configurees (GET /api/categories/priorities)" {
    $priorities = Invoke-RestMethod -Uri "$baseUrl/api/categories/priorities" -Method Get -Headers $adminHeaders
    if ($priorities.Count -lt 3) { throw "Moins de 3 priorites retournees." }
    foreach ($p in $priorities) {
        Write-Host "     Priorite : $($p.label) (Code: $($p.code), Niveau: $($p.level), Couleur: $($p.color))" -ForegroundColor Gray
    }
}

# 3. Créer une nouvelle catégorie en tant qu'ADMIN (POST /api/admin/categories)
Assert-Test "Creer une nouvelle categorie (POST /api/admin/categories)" {
    $body = @{
        name        = $catName
        description = "Taches liees aux pipelines CI/CD et deploiement"
        color       = "#805AD5"
    } | ConvertTo-Json

    $cat = Invoke-RestMethod -Uri "$baseUrl/api/admin/categories" -Method Post -Headers $adminHeaders -Body $body
    $script:testCatId = $cat.id
    if (-not $cat.id -or $cat.name -ne $catName) { throw "Creation de la categorie echouee." }
    Write-Host "     Categorie creee : ID $($cat.id), Nom: $($cat.name), Couleur: $($cat.color)" -ForegroundColor Gray
}

# 4. Rejeter la création d'un doublon de catégorie (Sécurité / Intégrité)
Assert-Test "Rejeter un doublon de categorie (POST /api/admin/categories - Nom identique)" {
    $duplicateBody = @{
        name        = $catName
        description = "Tentative de doublon"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/categories" -Method Post -Headers $adminHeaders -Body $duplicateBody
        throw "Le serveur aurait du rejeter le doublon avec une erreur 400."
    } catch {
        Write-Host "     Doublon correctement rejete par le serveur : $($_.Exception.Message)" -ForegroundColor Gray
    }
}

# 5. Consulter la catégorie par ID (GET /api/categories/{id})
Assert-Test "Consulter une categorie par son ID (GET /api/categories/{id})" {
    $cat = Invoke-RestMethod -Uri "$baseUrl/api/categories/$script:testCatId" -Method Get -Headers $adminHeaders
    if ($cat.id -ne $script:testCatId) { throw "L'identifiant ne correspond pas." }
    Write-Host "     Categorie verifiee : ID $($cat.id), Nom: $($cat.name)" -ForegroundColor Gray
}

# 6. Modifier une catégorie (PUT /api/admin/categories/{id})
Assert-Test "Modifier une categorie existante (PUT /api/admin/categories/{id})" {
    $updatedName = "$catName - Updated"
    $updateBody = @{
        name        = $updatedName
        description = "Description mise a jour pour la categorie"
        color       = "#319795"
    } | ConvertTo-Json

    $updated = Invoke-RestMethod -Uri "$baseUrl/api/admin/categories/$script:testCatId" -Method Put -Headers $adminHeaders -Body $updateBody
    if ($updated.name -ne $updatedName -or $updated.color -ne "#319795") { throw "Mise a jour echouee." }
    Write-Host "     Categorie mise a jour : $($updated.name), Couleur: $($updated.color)" -ForegroundColor Gray
}

# 7. Sécurité RBAC : Un utilisateur ROLE_USER ne doit PAS pouvoir créer une catégorie
Assert-Test "Securite RBAC : Refus de creation pour ROLE_USER (Attendu: 403 Forbidden)" {
    $unauthBody = @{
        name        = "HackCat"
        description = "Test non autorise"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/admin/categories" -Method Post -Headers $userHeaders -Body $unauthBody
        throw "Un simple utilisateur a pu creer une categorie ! Faille de securite !"
    } catch {
        Write-Host "     Acces refuse conformement a la securite RBAC (403 Forbidden)." -ForegroundColor Gray
    }
}

# 8. Supprimer la catégorie créée (DELETE /api/admin/categories/{id})
Assert-Test "Supprimer une categorie (DELETE /api/admin/categories/{id})" {
    $del = Invoke-RestMethod -Uri "$baseUrl/api/admin/categories/$script:testCatId" -Method Delete -Headers $adminHeaders
    Write-Host "     Reponse serveur : $($del.message)" -ForegroundColor Gray

    # Vérification que la catégorie n'existe plus
    try {
        Invoke-RestMethod -Uri "$baseUrl/api/categories/$script:testCatId" -Method Get -Headers $adminHeaders
        throw "La categorie est toujours accessible apres suppression."
    } catch {
        Write-Host "     Suppression confirmee : la categorie n'existe plus." -ForegroundColor Gray
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS CATEGORIES & PRIORITES : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
