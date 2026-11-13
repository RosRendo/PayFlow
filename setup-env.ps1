# Configure Java 21 environment for PayFlow
$javaPath = "C:\Program Files\Android\Android Studio\jbr"

if (Test-Path "$javaPath\bin\java.exe") {
    $env:JAVA_HOME = $javaPath
    $env:PATH = "$javaPath\bin;" + $env:PATH
    Write-Host "Java 21 environment configured successfully!" -ForegroundColor Green
    & java -version
} else {
    Write-Host "Warning: Java JDK not found at $javaPath" -ForegroundColor Yellow
}

