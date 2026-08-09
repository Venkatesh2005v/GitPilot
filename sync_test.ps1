$cc = New-Object System.Net.CookieContainer

# 1. Mock Login
$r1 = [System.Net.HttpWebRequest]::Create("http://localhost:8080/auth/mock")
$r1.CookieContainer = $cc
$res1 = $r1.GetResponse()
$sr1 = New-Object System.IO.StreamReader($res1.GetResponseStream())
Write-Host "Auth Response:" $sr1.ReadToEnd()

# 2. Get Repositories
$r2 = [System.Net.HttpWebRequest]::Create("http://localhost:8080/dashboard/repositories")
$r2.CookieContainer = $cc
$res2 = $r2.GetResponse()
$sr2 = New-Object System.IO.StreamReader($res2.GetResponseStream())
Write-Host "Repositories:" $sr2.ReadToEnd()

# 3. Sync Repository 1
$r3 = [System.Net.HttpWebRequest]::Create("http://localhost:8080/repositories/1/sync")
$r3.Method = "POST"
$r3.ContentLength = 0
$r3.CookieContainer = $cc
$res3 = $r3.GetResponse()
$sr3 = New-Object System.IO.StreamReader($res3.GetResponseStream())
Write-Host "Sync Result:" $sr3.ReadToEnd()

# 4. Fetch Commits
$r4 = [System.Net.HttpWebRequest]::Create("http://localhost:8080/repositories/1/commits")
$r4.CookieContainer = $cc
$res4 = $r4.GetResponse()
$sr4 = New-Object System.IO.StreamReader($res4.GetResponseStream())
Write-Host "Commits:" $sr4.ReadToEnd()
