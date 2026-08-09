function Get-HmacSha256($key, $message) {
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($key)
    $hash = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($message))
    return "sha256=" + [System.BitConverter]::ToString($hash).Replace("-", "").ToLower()
}

$secret = "GitPilotWebhook@2026#Secure"
$validPayload = '{"repository":{"id":123,"name":"test"},"commits":[]}'
$validSig = Get-HmacSha256 $secret $validPayload

$tests = @(
    @{ Name = "1. GET /webhooks/github"; Method = "GET"; Url = "http://localhost:8080/webhooks/github"; Body = $null; Headers = @{} },
    @{ Name = "2. POST /webhooks/github (no body)"; Method = "POST"; Url = "http://localhost:8080/webhooks/github"; Body = ""; Headers = @{ "Content-Type" = "application/json" } },
    @{ Name = "3. POST /webhooks/github (malformed JSON)"; Method = "POST"; Url = "http://localhost:8080/webhooks/github"; Body = "{bad:json"; Headers = @{ "Content-Type" = "application/json"; "x-hub-signature-256" = (Get-HmacSha256 $secret "{bad:json") } },
    @{ Name = "4. POST /webhooks/github (invalid signature)"; Method = "POST"; Url = "http://localhost:8080/webhooks/github"; Body = '{"repository":{"id":123}}'; Headers = @{ "x-hub-signature-256" = "sha256=invalid" } },
    @{ Name = "5. POST /webhooks/github (valid payload & signature)"; Method = "POST"; Url = "http://localhost:8080/webhooks/github"; Body = $validPayload; Headers = @{ "x-hub-signature-256" = $validSig; "x-github-event" = "push"; "Content-Type" = "application/json" } }
)

foreach ($t in $tests) {
    Write-Host "--------------------------------------------------"
    Write-Host "Scenario: $($t.Name)"
    try {
        $p = @{ Uri = $t.Url; Method = $t.Method; UseBasicParsing = $true }
        if ($t.Body) { $p.Body = $t.Body }
        if ($t.Headers.Count -gt 0) { $p.Headers = $t.Headers }
        $resp = Invoke-WebRequest @p
        Write-Host "HTTP Status: $($resp.StatusCode)"
        Write-Host "Response: $($resp.Content)"
    } catch {
        if ($_.Exception.Response) {
            Write-Host "HTTP Status: $($_.Exception.Response.StatusCode.value__)"
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "Response: $($reader.ReadToEnd())"
        } else {
            Write-Host "Error: $($_.Exception.Message)"
        }
    }
}
