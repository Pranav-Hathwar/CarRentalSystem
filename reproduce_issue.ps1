
$baseUrl = "http://localhost:8080"
$adminEmail = "admin@example.com"
$adminPassword = "admin"
$userEmail = "john_test_$(Get-Random)@gmail.com"
$userPassword = "password123"

# 0. Register User
Write-Host "Registering User $userEmail..."
$registerBody = @{
    name     = "John Test"
    email    = $userEmail
    password = $userPassword
    role     = "USER"
} | ConvertTo-Json

try {
    $regResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
    Write-Host "Registration Status: $($regResponse.StatusCode)"
}
catch {
    Write-Host "Registration Failed: $_"
}

# 1. Login as User to create a booking
Write-Host "Logging in as User..."
$loginBody = @{
    email    = $userEmail
    password = $userPassword
} | ConvertTo-Json

try {
    $userSession = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -SessionVariable "userSessionVar"
    Write-Host "User Login Status: $($userSession.StatusCode)"
}
catch {
    Write-Host "User Login Failed: $_"
    exit
}

# 2. Create a Booking
Write-Host "Creating a Booking..."
$pickup = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm")
$dropoff = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm")
$dob = "1990-01-01"

$bookingBody = @{
    carId           = 1
    pickupDateTime  = $pickup
    dropoffDateTime = $dropoff
    driverDob       = $dob
    licensePath     = "license.jpg"
    totalPrice      = 5000.00
} | ConvertTo-Json

try {
    $bookingResponse = Invoke-WebRequest -Uri "$baseUrl/api/bookings" -Method Post -Body $bookingBody -ContentType "application/json" -WebSession $userSessionVar
    Write-Host "Booking Created: $($bookingResponse.Content)"
}
catch {
    Write-Host "Failed to create booking: $_"
    exit
}

# 3. Get Booking ID from list
$bookingsResponse = Invoke-WebRequest -Uri "$baseUrl/api/bookings" -Method Get -WebSession $userSessionVar
$bookings = $bookingsResponse.Content | ConvertFrom-Json
$bookingId = $bookings[0].id
Write-Host "Booking ID: $bookingId"

# 4. Login as Admin
Write-Host "Logging in as Admin..."
$adminLoginBody = @{
    email    = $adminEmail
    password = $adminPassword
} | ConvertTo-Json

try {
    $adminSession = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json" -SessionVariable "adminSessionVar"
    Write-Host "Admin Login Status: $($adminSession.StatusCode)"
}
catch {
    Write-Host "Admin Login Failed: $_"
    exit
}

# 5. Request Payment
Write-Host "Requesting Payment for Booking $bookingId..."
try {
    $paymentResponse = Invoke-WebRequest -Uri "$baseUrl/api/bookings/$bookingId/request-payment" -Method Post -ContentType "application/json" -WebSession $adminSessionVar
    Write-Host "Payment Request Response: $($paymentResponse.Content)"
}
catch {
    Write-Host "Payment Request Failed: $_"
    # Print detailed error
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
        Write-Host "Error Body: $($reader.ReadToEnd())"
    }
}
