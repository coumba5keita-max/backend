[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "TESTS API - 01. Authentification & Inscription"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "           01. TESTS API : AUTHENTIFICATION & COMPTES            " -ForegroundColor Cyan
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

# 1. Inscription Utilisateur Standard
$randomSuffix = [System.Guid]::NewGuid().ToString().Substring(0, 8)
$userEmail = "user.$randomSuffix@test.com"
$userPassword = "Password123!"

Assert-Test "Inscription d'un utilisateur standard (POST /api/auth/register)" {
    $body = @{
        firstname = "Jean"
        lastname  = "Dupont"
        email     = $userEmail
        password  = $userPassword
        role      = "ROLE_USER"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
    Write-Host "     Reponse serveur : $response" -ForegroundColor Gray
}

# 2. Doublon d'email
Assert-Test "Blocage des emails en doublon (POST /api/auth/register - doublon)" {
    $body = @{
        firstname = "Jean"
        lastname  = "Dupont"
        email     = $userEmail
        password  = $userPassword
        role      = "ROLE_USER"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
        throw "L'API aurait du rejeter l'inscription avec un email deja existant."
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::BadRequest) {
            Write-Host "     Rejet 400 Bad Request confirme pour email en doublon." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 3. Connexion reussie
Assert-Test "Connexion utilisateur avec identifiants valides (POST /api/auth/login)" {
    $body = @{
        email    = $userEmail
        password = $userPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
    if (-not $response.token) { throw "Aucun token JWT n'a ete renvoye !" }
    Write-Host "     Token JWT recu avec succes (UserId: $($response.userId), Role: $($response.role))" -ForegroundColor Gray
}

# 4. Connexion avec mot de passe errone
Assert-Test "Rejet de connexion avec mot de passe errone (POST /api/auth/login)" {
    $body = @{
        email    = $userEmail
        password = "MauvaisMotDePasse!"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json; charset=utf-8"
        throw "La connexion aurait du echouer avec un mauvais mot de passe."
    } catch {
        if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Unauthorized) {
            Write-Host "     Rejet 401 Unauthorized confirme pour mauvais mot de passe." -ForegroundColor Gray
        } else {
            throw $_
        }
    }
}

# 5. Inscription et Connexion d'un compte Administrateur
$adminEmail = "admin.$randomSuffix@test.com"
Assert-Test "Creation et connexion d'un compte ROLE_ADMIN (POST /api/auth/register & login)" {
    $regBody = @{
        firstname = "Admin"
        lastname  = "Testeur"
        email     = $adminEmail
        password  = "AdminPass123!"
        role      = "ROLE_ADMIN"
    } | ConvertTo-Json

    Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json; charset=utf-8"

    $loginBody = @{
        email    = $adminEmail
        password = "AdminPass123!"
    } | ConvertTo-Json

    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json; charset=utf-8"
    if ($loginRes.role -ne "ROLE_ADMIN") { throw "Le role renvoye n'est pas ROLE_ADMIN !" }
    Write-Host "     Admin authentifie : $($loginRes.firstname) $($loginRes.lastname) (Role: $($loginRes.role))" -ForegroundColor Gray
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   BILAN DES TESTS AUTHENTIFICATION : $successCount / $totalTests reussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Red" })
Write-Host "=================================================================" -ForegroundColor Cyan
