[CmdletBinding()]
param(
    [string]$Image = 'scylladb/scylla',
    [ValidatePattern('^[a-z][a-z0-9_]*$')]
    [string]$Keyspace = 'notifications',
    [ValidateRange(5, 3600)]
    [int]$WaitSeconds = 240,
    [switch]$RaiseAioLimit
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$seedName = 'scylla-dev'
$schemaPath = Join-Path $PSScriptRoot 'schema.cql'
$containerSchemaPath = '/tmp/schema.cql'

if (-not (Test-Path -LiteralPath $schemaPath -PathType Leaf)) {
    throw "Schema file not found: $schemaPath"
}

function Invoke-Docker([string[]]$Arguments) {
    $output = & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker command failed (exit $LASTEXITCODE): docker $($Arguments -join ' ')"
    }
    $output
}

function Start-ScyllaNode([string]$Name, [string]$NodeImage, [string]$SeedAddress) {
    if ($containers -ccontains $Name) {
        $running = Invoke-Docker @('inspect', '-f', '{{.State.Running}}', $Name)
        if ($running -eq 'false') {
            Invoke-Docker @('start', $Name) | Out-Null
        } elseif ($RaiseAioLimit) {
            $state = & docker exec $Name supervisorctl status scylla
            if ($state -match '^scylla[ \t]+FATAL\b') {
                Invoke-Docker @('exec', $Name, 'supervisorctl', 'start', 'scylla') | Out-Null
            }
        }
    } else {
        $options = @('run', '-d', '--name', $Name, '--network', 'bridge')
        if ($SeedAddress) {
            $options += @("$NodeImage", "--seeds=$SeedAddress")
        } else {
            $options += @('-p', '127.0.0.1:9042:9042', $NodeImage)
        }
        Invoke-Docker ($options + @('--smp', '1', '--memory', '2G')) | Out-Null
    }

    $address = Invoke-Docker @('inspect', '-f', '{{.NetworkSettings.Networks.bridge.IPAddress}}', $Name)
    if ([string]::IsNullOrWhiteSpace($address)) {
        throw "Container $Name needs an IP address on the Docker bridge network."
    }
    $address.Trim()
}

function Wait-ForNodes([string[]]$Addresses, [string]$PendingNode) {
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    do {
        try {
            $status = (Invoke-Docker @('exec', $seedName, 'nodetool', 'status') 2>$null) -join "`n"
            $ready = [regex]::Matches($status, '(?m)^UN[ \t]+(\S+)') |
                ForEach-Object { $_.Groups[1].Value }
            $missing = @($Addresses | Where-Object { $_ -notin $ready })
            if ($missing.Count -eq 0) {
                Invoke-Docker @('exec', $seedName, 'cqlsh', '-e', 'SELECT data_center FROM system.local;') 2>$null | Out-Null
                return $status
            }
        } catch {
            # nodetool and cqlsh can fail while the seed node is starting.
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)
    throw "Cluster or CQL did not become ready while waiting for $PendingNode. Check docker logs $PendingNode and nodetool status."
}

function Get-Replication {
    $query = "SELECT JSON replication FROM system_schema.keyspaces WHERE keyspace_name = '$Keyspace';"
    $output = Invoke-Docker @('exec', $seedName, 'cqlsh', '-e', $query)
    $row = $output | Where-Object { $_.TrimStart().StartsWith('{') } | Select-Object -First 1
    if ($row) {
        ($row | ConvertFrom-Json).replication
    }
}

$containers = @(Invoke-Docker @('ps', '-a', '--format', '{{.Names}}'))
if ($RaiseAioLimit) {
    # Applies to the shared Docker Desktop Linux kernel until the VM restarts.
    $aioLimit = & wsl.exe -d docker-desktop -u root -- sysctl -n fs.aio-max-nr
    if ($LASTEXITCODE -ne 0 -or "$aioLimit" -notmatch '^\s*\d+\s*$') {
        throw 'Could not read the Docker Desktop WSL AIO limit.'
    }
    if ([long]$aioLimit -lt 1048576) {
        & wsl.exe -d docker-desktop -u root -- sysctl -w fs.aio-max-nr=1048576
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not raise the Docker Desktop WSL AIO limit.'
        }
    }
}

$seedAddress = Start-ScyllaNode -Name $seedName -NodeImage $Image
Write-Host "Waiting for $seedName."
Wait-ForNodes @($seedAddress) $seedName | Out-Null
$clusterImage = Invoke-Docker @('inspect', '-f', '{{.Image}}', $seedName)

$addresses = @($seedAddress)
foreach ($name in @('scylla-node2', 'scylla-node3')) {
    $addresses += Start-ScyllaNode -Name $name -NodeImage $clusterImage -SeedAddress $seedAddress
    Write-Host "Waiting for $name to join the cluster."
    $clusterStatus = Wait-ForNodes $addresses $name
}

$datacenters = [regex]::Matches($clusterStatus, '(?m)^Datacenter:[ \t]*(\S+)')
if ($datacenters.Count -ne 1) {
    throw 'Expected a cluster with one datacenter.'
}
$datacenter = $datacenters[0].Groups[1].Value

$replication = Get-Replication
if ($null -eq $replication) {
    $escapedDatacenter = $datacenter.Replace("'", "''")
    $create = "CREATE KEYSPACE IF NOT EXISTS $Keyspace WITH replication = {'class': 'NetworkTopologyStrategy', '$escapedDatacenter': 3};"
    Invoke-Docker @('exec', $seedName, 'cqlsh', '-e', $create) | Out-Null
    $replication = Get-Replication
}

if ($null -eq $replication -or $replication.class -notmatch '(^|\.)NetworkTopologyStrategy$' -or
    $replication.PSObject.Properties[$datacenter].Value -ne '3') {
    throw "Expected $Keyspace to use NetworkTopologyStrategy and RF=3 in $datacenter. Use -Keyspace for a new keyspace or inspect the existing replication settings."
}

Write-Host $clusterStatus
Write-Host "Keyspace $Keyspace is ready with RF=3 in $datacenter."

Write-Host "Applying schema to $Keyspace."
Invoke-Docker @('cp', $schemaPath, "${seedName}:$containerSchemaPath") | Out-Null
Invoke-Docker @('exec', $seedName, 'cqlsh', '-k', $Keyspace, '-f', $containerSchemaPath)
Invoke-Docker @('exec', $seedName, 'cqlsh', '-k', $Keyspace, '-e', 'DESCRIBE TABLES;')
