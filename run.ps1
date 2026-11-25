# Compilation et execution du projet RF (SharvitB2).
# Usage : .\run.ps1 [options transmises a experiment.Main, ex. --experience knn]
param([Parameter(ValueFromRemainingArguments = $true)] $Arguments)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# JDK >= 17 requis : on prend JAVA_HOME s'il est defini, sinon le JDK 21 standard.
if ($env:JAVA_HOME) { $jdk = Join-Path $env:JAVA_HOME "bin" } else { $jdk = "C:\Program Files\Java\jdk-21\bin" }

if (Test-Path out) { Remove-Item -Recurse -Force out }
$sources = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" -encoding UTF-8 --release 17 -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& "$jdk\java.exe" -cp out experiment.Main @Arguments
