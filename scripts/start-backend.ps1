param([switch]$VerifyDatabase)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceJdkRoot = Join-Path $projectRoot '..\work\tools\jdk17'
$workspaceJdk = Get-ChildItem -LiteralPath $workspaceJdkRoot -Directory -ErrorAction SilentlyContinue |
    Select-Object -First 1

if ($workspaceJdk) {
    $env:JAVA_HOME = $workspaceJdk.FullName
    $env:Path = "$($workspaceJdk.FullName)\bin;$env:Path"
}

$javaVersion = & java --version | Select-Object -First 1
if ($javaVersion -notmatch '\b17\.') {
    throw 'JDK 17 was not found. Install JDK 17 or set JAVA_HOME for this PowerShell session.'
}

$passwordWasSet = [bool]$env:DB_PASSWORD
if (-not $passwordWasSet) {
    $securePassword = Read-Host 'Enter PostgreSQL password (input hidden)' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

$env:MAVEN_USER_HOME = Join-Path $projectRoot '.cache\maven'

if (-not $env:SERVER_PORT) {
    $env:SERVER_PORT = '18080'
}
Write-Host "Backend port: $env:SERVER_PORT"

Push-Location (Join-Path $projectRoot 'backend')
try {
    if ($VerifyDatabase) {
        $previousDbTestFlag = $env:RUN_DB_TESTS
        try {
            $env:RUN_DB_TESTS = 'true'
            & .\mvnw.cmd test
            if ($LASTEXITCODE -ne 0) { throw 'Real database verification failed; backend startup stopped.' }
        }
        finally {
            if ($null -eq $previousDbTestFlag) { Remove-Item Env:RUN_DB_TESTS -ErrorAction SilentlyContinue }
            else { $env:RUN_DB_TESTS = $previousDbTestFlag }
        }
    }
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Backend startup failed.' }
}
finally {
    Pop-Location
    if (-not $passwordWasSet) {
        Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    }
}
