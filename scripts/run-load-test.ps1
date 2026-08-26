$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$baseUrl = if ($args.Count -gt 0) { $args[0] } else { 'http://127.0.0.1:8080' }
$users = if ($args.Count -gt 1) { $args[1] } else { '100' }
$concurrency = if ($args.Count -gt 2) { $args[2] } else { '20' }

java '-Dfile.encoding=UTF-8' (Join-Path $projectRoot 'tools\CheckoutLoadTest.java') $baseUrl $users $concurrency
if ($LASTEXITCODE -ne 0) { throw 'Checkout load test failed.' }
