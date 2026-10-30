param([int]$Port = 8080, [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectPath
if ($Port -lt 1024 -or $Port -gt 65535) { throw 'Choose a port between 1024 and 65535.' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is already in use. Try -Port 18080." }
if (-not $SkipBuild) {
    & .\mvnw.cmd -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }
}
$version = '11.0.27'
$localPath = Join-Path $projectPath '.local'
$tomcatPath = Join-Path $localPath "apache-tomcat-$version"
if (-not (Test-Path -LiteralPath $tomcatPath)) {
    New-Item -ItemType Directory -Force -Path $localPath | Out-Null
    $archive = Join-Path $localPath "tomcat-$version.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/tomcat/tomcat/$version/tomcat-$version.zip"
    Invoke-WebRequest -Uri $url -OutFile $archive
    Invoke-WebRequest -Uri "$url.sha512" -OutFile "$archive.sha512"
    $expected = ((Get-Content -LiteralPath "$archive.sha512" -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expected) { throw 'Tomcat checksum mismatch.' }
    Expand-Archive -LiteralPath $archive -DestinationPath $localPath -Force
}
[xml]$server = Get-Content -LiteralPath (Join-Path $tomcatPath 'conf/server.xml') -Raw
$server.Server.SetAttribute('port', '-1')
foreach ($connector in $server.SelectNodes('//Connector')) {
    if ($connector.GetAttribute('protocol') -eq 'HTTP/1.1') {
        $connector.SetAttribute('port', [string]$Port)
        $connector.SetAttribute('address', '127.0.0.1')
    }
}
foreach ($valve in $server.SelectNodes('//Valve')) {
    if ($valve.GetAttribute('className').EndsWith('AccessLogValve')) {
        $valve.SetAttribute('pattern', '%h %l %u %t "%m %U %H" %s %b')
    }
}
$server.Save((Join-Path $tomcatPath 'conf/server.xml'))
Copy-Item -LiteralPath 'target/enjoytrip.war' -Destination (Join-Path $tomcatPath 'webapps/enjoytrip.war') -Force
$config = Join-Path $projectPath 'config/application-local.properties'
if (-not (Test-Path -LiteralPath $config)) { Copy-Item -LiteralPath 'config/application-local.properties.example' -Destination $config }
$env:APP_BASE_URL = "http://localhost:$Port/enjoytrip"
Write-Host "EnjoyTrip: $env:APP_BASE_URL"
Write-Host 'Stop with Ctrl+C. Redis: docker compose up -d'
& java "-Dcatalina.home=$tomcatPath" "-Dcatalina.base=$tomcatPath" "-Denjoytrip.config=$config" '-Dfile.encoding=UTF-8' '-Dsun.stdout.encoding=UTF-8' '-Dsun.stderr.encoding=UTF-8' -cp "$tomcatPath/bin/bootstrap.jar;$tomcatPath/bin/tomcat-juli.jar" org.apache.catalina.startup.Bootstrap start
