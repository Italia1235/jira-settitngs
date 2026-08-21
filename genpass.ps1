 б$chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{};:,.<>?'.ToCharArray()
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$bytes = New-Object byte[] 30
$rng.GetBytes($bytes)
$sb = New-Object System.Text.StringBuilder
foreach ($b in $bytes) {
    [void]$sb.Append($chars[$b % $chars.Length])
}
Write-Output $sb.ToString()