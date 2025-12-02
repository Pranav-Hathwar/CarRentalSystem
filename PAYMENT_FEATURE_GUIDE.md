# Payment Request Feature - Detailed Guide

## Overview

The payment request feature allows admins to request payment from customers and track payment status for each booking.

---

## Payment Status Flow

```
┌─────────────────┐
│     UNPAID      │  ← Initial state when booking created
│   (Red Badge)   │
└────────┬────────┘
         │
         │ Admin clicks "Request Payment"
         ↓
┌─────────────────────────┐
│  PAYMENT_REQUESTED      │  ← Payment has been requested
│   (Orange Badge)        │
└────────┬────────────────┘
         │
         │ Customer clicks "Confirm Payment"
         ↓
┌─────────────────┐
│      PAID       │  ← Payment confirmed and complete
│  (Green Badge)  │
└─────────────────┘
```

---

## Admin Workflow

### 1. Login as Admin
```
Email: admin@example.com
Password: admin
```

### 2. Navigate to Owner Dashboard
```
URL: http://localhost:8080/owner-dashboard.html
```

### 3. View Pending Bookings
- Click **"Pending Bookings"** tab
- Shows table with:
  - Booking ID
  - Vehicle ID
  - Dates
  - Amount
  - Payment Status
  - Actions

### 4. Request Payment
- Find booking with `UNPAID` status
- Click **"Request Payment"** button
- Confirm action
- Status changes to `PAYMENT_REQUESTED` immediately

### 5. Track Payment Status
- Keep viewing the table to see updates
- When customer confirms payment, status changes to `PAID`

---

## Customer Workflow

### 1. Login as Customer
```
Use the credentials you registered with
(e.g., john@gmail.com / password123)
```

### 2. Navigate to Customer Dashboard
```
URL: http://localhost:8080/customer-dashboard.html
OR
Click "My Bookings" in navigation
```

### 3. View Active Bookings
- Click **"Active Bookings"** tab
- Shows all active bookings with:
  - Booking ID
  - Vehicle ID
  - Dates
  - Total Amount
  - Booking Status
  - Payment Status (with badge)
  - Action buttons

### 4. See Payment Request
- When admin requests payment, badge shows: `PAYMENT_REQUESTED` (orange)
- **"Confirm Payment"** button appears

### 5. Confirm Payment
- Click **"Confirm Payment"** button
- Confirm action
- Payment Status changes to `PAID` (green)

---

## Payment Status Badges

### Visual Indicators

| Badge | Color | Status | Meaning |
|-------|-------|--------|---------|
| UNPAID | Red | `payment-unpaid` | Payment not yet requested |
| PAYMENT_REQUESTED | Orange | `payment-payment_requested` | Admin has requested payment |
| PAID | Green | `payment-paid` | Customer has confirmed payment |

### CSS Classes
```css
.payment-badge              /* Base styling */
.payment-unpaid            /* Red background */
.payment-payment_requested /* Orange background */
.payment-paid              /* Green background */
```

---

## API Endpoints

### Request Payment (Admin Only)

**Endpoint:**
```
POST /api/bookings/{bookingId}/request-payment
```

**Headers:**
```
Content-Type: application/json
Cookie: JSESSIONID=<session_id>
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Payment request sent to customer"
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Only admin can request payment"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/bookings/5/request-payment \
  -H "Content-Type: application/json" \
  -b "JSESSIONID=your_session_id"
```

---

### Mark as Paid (Customer)

**Endpoint:**
```
POST /api/bookings/{bookingId}/mark-paid
```

**Headers:**
```
Content-Type: application/json
Cookie: JSESSIONID=<session_id>
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Payment confirmed"
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Failed to confirm payment"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/bookings/5/mark-paid \
  -H "Content-Type: application/json" \
  -b "JSESSIONID=your_session_id"
```

---

### Get All Bookings with Payment Status

**Endpoint:**
```
GET /api/bookings
```

**Response:**
```json
[
  {
    "id": 1,
    "carId": 3,
    "startDate": "2024-12-05",
    "endDate": "2024-12-08",
    "totalPrice": 13500.00,
    "status": "CONFIRMED",
    "paymentStatus": "UNPAID"
  },
  {
    "id": 2,
    "carId": 5,
    "startDate": "2024-12-10",
    "endDate": "2024-12-12",
    "totalPrice": 9000.00,
    "status": "CONFIRMED",
    "paymentStatus": "PAYMENT_REQUESTED"
  }
]
```

---

## Database Schema

### Bookings Table

```sql
CREATE TABLE bookings (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  car_id INT,
  start_date DATE,
  end_date DATE,
  total_price DOUBLE,
  status VARCHAR(50),
  payment_status VARCHAR(50) DEFAULT 'UNPAID',
  pickup_datetime TIMESTAMP,
  dropoff_datetime TIMESTAMP,
  driving_license_path VARCHAR(1024),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (car_id) REFERENCES cars(id)
);
```

### Booking Entity

```java
public class Booking {
  private int id;
  private int userId;
  private int carId;
  private Date startDate;
  private Date endDate;
  private double totalPrice;
  private String status;           // PENDING, CONFIRMED, CANCELLED
  private String paymentStatus;    // UNPAID, PAYMENT_REQUESTED, PAID
  private Timestamp pickupDatetime;
  private Timestamp dropoffDatetime;
  private String drivingLicensePath;
  private Timestamp createdAt;
  
  // Getters and setters...
}
```

---

## Backend Implementation

### BookingDAO Changes

```java
// Update payment status
public boolean updatePaymentStatus(int bookingId, String paymentStatus) {
  String sql = "UPDATE bookings SET payment_status = ? WHERE id = ?";
  // Execute update...
}

// Retrieve booking with payment status
public List<Booking> getBookingsByUserId(int userId) {
  // Queries and sets paymentStatus on each booking
}
```

### BookingServlet Changes

```java
// Handle payment request from admin
private void handlePaymentRequest(HttpServletRequest req, HttpServletResponse resp, User user) {
  // Verify admin role
  // Extract booking ID from path
  // Update payment status to "PAYMENT_REQUESTED"
}

// Handle payment confirmation from customer
private void handleMarkPaid(HttpServletRequest req, HttpServletResponse resp, User user) {
  // Extract booking ID from path
  // Update payment status to "PAID"
}

// Route incoming requests
String path = req.getPathInfo();
if (path.contains("/request-payment/")) handlePaymentRequest(...);
else if (path.contains("/mark-paid/")) handleMarkPaid(...);
```

---

## Frontend Implementation

### Customer Dashboard

```html
<!-- Show payment status badge -->
<p><strong>Payment Status:</strong> 
  <span class="payment-badge payment-${paymentStatus.toLowerCase()}">
    ${paymentStatus}
  </span>
</p>

<!-- Show action buttons based on status -->
${paymentStatus === 'PAYMENT_REQUESTED' ? `
  <button class="btn btn-success" onclick="markPaymentDone(${booking.id})">
    Confirm Payment
  </button>
` : ''}
```

### Owner Dashboard

```html
<!-- Show payment status in table -->
<td>
  <span class="payment-badge payment-${paymentStatus.toLowerCase()}">
    ${paymentStatus}
  </span>
</td>

<!-- Show request payment button for unpaid bookings -->
${paymentStatus === 'UNPAID' ? `
  <button class="btn btn-warning" onclick="requestPayment(${booking.id})">
    Request Payment
  </button>
` : ''}
```

### JavaScript Functions

```javascript
// Admin requests payment
async function requestPayment(bookingId) {
  const response = await fetch(`/api/bookings/${bookingId}/request-payment`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include'
  });
  // Update UI...
}

// Customer confirms payment
async function markPaymentDone(bookingId) {
  const response = await fetch(`/api/bookings/${bookingId}/mark-paid`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include'
  });
  // Update UI...
}
```

---

## Security Considerations

1. **Authentication Required**: All payment endpoints check session
2. **Admin Restriction**: Only ADMIN role can request payment
3. **Email Verification**: Admin must be `admin@example.com`
4. **Session Validation**: JSESSIONID required for all requests
5. **CORS Headers**: Properly configured for API access

---

## Testing Scenarios

### Scenario 1: Basic Payment Request
1. Admin creates booking (or use existing)
2. Admin requests payment → Status: PAYMENT_REQUESTED ✓
3. Customer sees orange badge ✓
4. Customer clicks confirm → Status: PAID ✓

### Scenario 2: Multiple Bookings
1. Create 3 bookings with different customers
2. Request payment for booking 1 and 3
3. Customers confirm payment for booking 1
4. Booking 2 still UNPAID, Booking 3 still PAYMENT_REQUESTED ✓

### Scenario 3: Admin-Only Access
1. Login as regular customer
2. Try to hit `/api/bookings/{id}/request-payment` endpoint
3. Response: "Only admin can request payment" ✓

### Scenario 4: Unauthorized Admin
1. Create user with email `otherperson@gmail.com`
2. Manually set role to ADMIN in database (if possible)
3. Try to login
4. Response: "Only admin@example.com can access admin features" ✓

---

## Troubleshooting

### Button Not Appearing?
- Check payment status in API response
- Verify booking status is CONFIRMED
- Check console for JavaScript errors

### Status Not Updating?
- Verify session is active (check JSESSIONID)
- Check server logs for errors
- Refresh page to see latest status

### API Request Failing?
- Check Content-Type header
- Verify credentials: `include`
- Check booking ID is correct
- Check user has right role (admin/customer)

---

## Future Enhancements

1. **Email Notifications**: Send email when payment requested
2. **Payment Gateway**: Integrate Stripe/PayPal for online payment
3. **Invoice Generation**: Create PDF invoices for payments
4. **Payment History**: Track payment dates and methods
5. **Refund Management**: Handle refunds for cancellations
6. **Payment Reminders**: Auto-send reminders if payment overdue

---

**Feature Status: ✅ FULLY IMPLEMENTED & DEPLOYED**

For detailed implementation code, see:
- `BookingServlet.java` - API endpoints
- `BookingDAO.java` - Database operations
- `Booking.java` - Entity model
- `customer-dashboard.html` - Customer UI
- `owner-dashboard.html` - Admin UI
