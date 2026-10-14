param(
    [ValidatePattern('^[A-Za-z0-9_]{3,50}$')][string]$Username = 'campus_admin',
    [string]$Nickname = '校园管理员'
)

$ErrorActionPreference = 'Stop'
# 仅供本机数据库管理员初始化新账号；普通用户注册接口始终创建USER。
# 已存在的用户名会在注册时被拒绝，不提升已有账号，不保存密码到文件。
$securePassword = Read-Host 'Enter a password for the new administrator (input hidden)' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $registrationBody = @{
        username = $Username
        nickname = $Nickname
        password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } | ConvertTo-Json
    $createdUser = Invoke-RestMethod 'http://localhost:18080/api/auth/register' -Method Post `
        -ContentType 'application/json' -Body $registrationBody
    $registrationBody = $null
    # Username仅允许字母、数字、下划线；不能插入SQL字符。
    & docker exec post-demo psql -U postgres -d secondhand_market -v ON_ERROR_STOP=1 `
        -c "UPDATE users SET role='ADMIN' WHERE id=$($createdUser.id) AND username='$Username' AND role='USER' RETURNING id,username,role;"
    if ($LASTEXITCODE -ne 0) { throw 'Role initialization failed; the new account remains a regular user.' }
    Write-Host "Administrator created: $Username. Sign in at http://localhost:5173/login."
}
finally {
    $registrationBody = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
