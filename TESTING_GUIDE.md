# Quick Testing Guide

## System Access

**URL:** http://localhost:8080

---

## Admin Login (Only This Account Works)

**Email:** `admin@example.com`
**Password:** `admin`

**Important:** Only this specific email can have ADMIN role. Any other user attempting to register with ADMIN role will be automatically converted to USER role.

---

## Regular User Registration

Register with any email and password - will automatically be USER role:

Example:
- **Email:** `john@gmail.com`
- **Password:** `password123`

---

## Testing Payment Features

### Step 1: Admin Views Pending Bookings
1. Login with `admin@example.com` / `admin`
2. Go to **Owner Dashboard**
3. Click **"Pending Bookings"** tab
4. You should see a table with all active bookings

### Step 2: Admin Requests Payment
1. In the Pending Bookings table, find a booking with UNPAID status
2. Click **"Request Payment"** button
3. Confirm the action
4. Booking status changes to: `PAYMENT_REQUESTED` ✓

### Step 3: Customer Sees Payment Request
1. Login as regular user (who made the booking)
2. Go to **My Bookings** (Customer Dashboard)
3. Click **"Active Bookings"** tab
4. You should see:
   - Booking Status (e.g., CONFIRMED)
   - Payment Status badge showing: `PAYMENT_REQUESTED` (in orange)
   - Button: **"Confirm Payment"**

### Step 4: Customer Confirms Payment
1. Click **"Confirm Payment"** button
2. Confirm the action
3. Payment status changes to: `PAID` ✓

---

## Testing Image Loading

### Verify Images Display:
1. Go to **Browse Fleet** (cars.html)
2. All car images should load properly
3. If an image fails to load, it falls back to default placeholder
4. Hover over car images - they should zoom smoothly (1.15x scale)
5. Scroll down - lazy loading should work without blocking

---

## Testing Admin Security

### Attempt 1: Try to Register as Admin
1. Go to **Sign Up**
2. Fill in details and try to send role as "ADMIN" in network request
3. **Result:** Role is forced to "USER" ✓

### Attempt 2: Try to Login with Different Email
1. Create a user account with email `hacker@evil.com`
2. Somehow try to change database to make them ADMIN role (won't work in UI)
3. Try to login with this account
4. **Result:** Login rejected - "Only admin@example.com can access admin features" ✓

### Attempt 3: Successful Admin Login
1. Login with `admin@example.com` / `admin`
2. **Result:** Success ✓

---

## API Endpoints for Testing (via curl or Postman)

### Request Payment (Admin Only)
```bash
POST http://localhost:8080/api/bookings/1/request-payment
Headers: Content-Type: application/json
Credentials: include (session cookie)
```

### Mark as Paid (Customer)
```bash
POST http://localhost:8080/api/bookings/1/mark-paid
Headers: Content-Type: application/json
Credentials: include (session cookie)
```

### Get All Bookings (Returns with Payment Status)
```bash
GET http://localhost:8080/api/bookings
Credentials: include (session cookie)
```

---

## Payment Status Codes

| Status | Color | Meaning |
|--------|-------|---------|
| `UNPAID` | Red | Payment not yet requested |
| `PAYMENT_REQUESTED` | Orange | Admin has requested payment from customer |
| `PAID` | Green | Customer has confirmed payment |

---

## Feature Checklist

- ✅ **Single Admin Account**: Only `admin@example.com` can be admin
- ✅ **User Registration**: Any user can register (always USER role)
- ✅ **Payment Requests**: Admin can request payment from bookings
- ✅ **Payment Confirmation**: Customer can confirm payment
- ✅ **Image Loading**: All images load with proper fallback
- ✅ **Admin Dashboard**: Shows pending bookings with payment status
- ✅ **Customer Dashboard**: Shows bookings with payment buttons
- ✅ **Styling**: Color-coded payment status badges
- ✅ **Security**: Admin role strictly enforced

---

## Troubleshooting

### Images Not Loading?
- Check browser console for errors
- Verify image URLs are accessible
- Clear browser cache and refresh
- Check `images/` folder exists in WebContent

### Payment Button Not Showing?
- Make sure you're logged in as correct user
- Check booking status is CONFIRMED
- Payment status should be UNPAID to see request button
- Payment status should be PAYMENT_REQUESTED to see confirm button

### Admin Login Fails?
- Verify email is exactly: `admin@example.com` (case-sensitive)
- Verify password is: `admin` (exact match)
- Check no extra spaces in email/password
- Ensure you're using the login page, not registration

---

**All systems operational!** 🚀
