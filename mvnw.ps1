# Maven Wrapper PowerShell script
param (
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

# 1. Ensure JAVA_HOME is set
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $fallbackJava = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path "$fallbackJava\bin\java.exe") {
        $env:JAVA_HOME = $fallbackJava
        $env:PATH = "$fallbackJava\bin;" + $env:PATH
    }
}

# 2. Local Maven Directory
$mavenVersion = "3.9.9"
$mavenHome = "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-$mavenVersion"
$mvnCmd = "$mavenHome\apache-maven-$mavenVersion\bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    Write-Host "Maven not found locally. Downloading Apache Maven $mavenVersion..." -ForegroundColor Cyan
    $zipPath = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    
    New-Item -ItemType Directory -Force -Path $mavenHome | Out-Null
    Invoke-WebRequest -Uri $url -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $mavenHome -Force
    Remove-Item $zipPath -Force
}

# 3. Execute Maven
& $mvnCmd @MavenArgs

