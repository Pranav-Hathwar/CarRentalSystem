// State
let cars = [];
let bookings = [];

// DOM Elements
const carListEl = document.getElementById('car-list');
const bookingListEl = document.getElementById('booking-list');
const modal = document.getElementById('booking-modal');
const closeModalBtn = document.querySelector('.close-modal');
const bookingForm = document.getElementById('booking-form');
const modalCarName = document.getElementById('modal-car-name');
const modalPrice = document.getElementById('modal-price');
const modalTotal = document.getElementById('modal-total');
const pickupTimeInput = document.getElementById('pickup-time');
const dropoffTimeInput = document.getElementById('dropoff-time');
const carIdInput = document.getElementById('car-id');

// Initialize
function init() {
    if (carListEl) fetchCars();
    if (bookingListEl) fetchBookings();
    setupEventListeners();
}

// Fetch Cars from JSON
async function fetchCars() {
    try {
        console.log('Fetching cars...');
        const response = await fetch('/api/cars');
        if (!response.ok) throw new Error('Failed to load car data');
        const data = await response.json();
        console.log('Cars data received:', data);
        cars = data;
        renderCars();
    } catch (error) {
        console.error('Error loading cars:', error);
        if (carListEl) carListEl.innerHTML = '<p class="empty-state">Error loading cars. Please try again later.</p>';
    }
}

// Fetch Bookings from API
async function fetchBookings() {
    try {
        console.log('Fetching bookings...');
        const response = await fetch('/api/bookings');
        if (!response.ok) throw new Error('Failed to load bookings');
        const data = await response.json();
        console.log('Bookings data received:', data);
        bookings = data;
        renderBookings();
    } catch (error) {
        console.error('Error loading bookings:', error);
        if (bookingListEl) bookingListEl.innerHTML = '<p class="empty-state">Error loading bookings. Please try again later.</p>';
    }
}

// Render Cars
function renderCars() {
    if (!carListEl) return;
    carListEl.innerHTML = cars.map(car => `
        <div class="car-card">
            <img src="${car.image}" alt="${car.name}" class="car-image">
            <div class="car-details">
                <div class="car-title">
                    <span>${car.name}</span>
                    <span class="car-price">₹${car.price}/day</span>
                </div>
                <div class="car-features">
                    ${car.features.map(f => `<span>• ${f}</span>`).join('')}
                </div>
                <button class="btn btn-primary btn-block" onclick="openBookingModal(${car.id})">
                    Book Now
                </button>
                <button class="btn btn-danger btn-block" style="margin-top: 10px; background-color: #ef4444;" onclick="deleteCar(${car.id})">
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

async function deleteCar(id) {
    if (!confirm('Are you sure you want to delete this car?')) return;

    try {
        const response = await fetch(`/api/cars?id=${id}`, { method: 'DELETE' });
        if (response.ok) {
            alert('Car deleted successfully');
            fetchCars();
        } else {
            alert('Failed to delete car');
        }
    } catch (error) {
        console.error('Error deleting car:', error);
        alert('Error deleting car');
    }
}

// Open Modal
window.openBookingModal = function (carId) {
    const car = cars.find(c => c.id === carId);
    if (!car) return;

    if (carIdInput) carIdInput.value = car.id;
    if (modalCarName) modalCarName.textContent = car.name;
    if (modalPrice) modalPrice.textContent = car.price;

    // Set default times (pickup: now, dropoff: now + 24h)
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    const tomorrow = new Date(now);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (pickupTimeInput) pickupTimeInput.value = now.toISOString().slice(0, 16);
    if (dropoffTimeInput) dropoffTimeInput.value = tomorrow.toISOString().slice(0, 16);

    updateTotal();

    if (modal) modal.classList.add('active');
};

// Close Modal
function closeModal() {
    if (modal) modal.classList.remove('active');
}

// Update Total Price
function updateTotal() {
    if (!modalPrice || !pickupTimeInput || !dropoffTimeInput || !modalTotal) return;

    const price = parseFloat(modalPrice.textContent);
    const pickup = new Date(pickupTimeInput.value);
    const dropoff = new Date(dropoffTimeInput.value);

    if (isNaN(pickup.getTime()) || isNaN(dropoff.getTime())) return;

    const diffMs = dropoff - pickup;
    const diffHours = diffMs / (1000 * 60 * 60);

    if (diffHours <= 0) {
        modalTotal.textContent = "0.00";
        return;
    }

    // Calculate price based on hours (price is per day / 24 hours)
    const total = (price / 24) * diffHours;
    modalTotal.textContent = total.toFixed(2);
}

// Handle Booking Submission
async function handleBooking(e) {
    e.preventDefault();

    const carId = parseInt(carIdInput.value);
    const name = document.getElementById('name').value;
    const pickup = new Date(pickupTimeInput.value);
    const dropoff = new Date(dropoffTimeInput.value);
    const total = parseFloat(modalTotal.textContent);

    if (dropoff <= pickup) {
        alert("Dropoff time must be after pickup time");
        return;
    }

    const bookingData = {
        carId: carId,
        customerName: name,
        pickupTime: pickup.toLocaleString(),
        dropoffTime: dropoff.toLocaleString(),
        totalPrice: total
    };

    try {
        const response = await fetch('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        });

        if (response.ok) {
            alert('Booking Confirmed!');
            closeModal();
            bookingForm.reset();
            window.location.href = 'bookings.html';
        } else {
            const errorText = await response.text();
            alert('Failed to create booking: ' + errorText);
        }
    } catch (error) {
        console.error('Error creating booking:', error);
        alert('Error creating booking');
    }
}

// Render Bookings
function renderBookings() {
    if (!bookingListEl) return;

    if (bookings.length === 0) {
        bookingListEl.innerHTML = '<p class="empty-state">You haven\'t booked any cars yet.</p>';
        return;
    }

    bookingListEl.innerHTML = bookings.map(booking => `
        <div class="booking-item">
            <div class="booking-info">
                <h3>${booking.car.name}</h3>
                <p>From: ${booking.pickup} <br> To: ${booking.dropoff} <br> Total: ₹${booking.total}</p>
                <p class="text-light" style="font-size: 0.875rem; color: #6b7280;">Booked on ${booking.date}</p>
            </div>
            <span class="booking-status status-confirmed">${booking.status}</span>
        </div>
    `).join('');
}

// Event Listeners
function setupEventListeners() {
    if (closeModalBtn) closeModalBtn.addEventListener('click', closeModal);

    window.addEventListener('click', (e) => {
        if (modal && e.target === modal) closeModal();
    });

    if (pickupTimeInput) pickupTimeInput.addEventListener('change', updateTotal);
    if (dropoffTimeInput) dropoffTimeInput.addEventListener('change', updateTotal);
    if (bookingForm) bookingForm.addEventListener('submit', handleBooking);

    // Add Car Form Handling
    const addCarForm = document.getElementById('add-car-form');
    if (addCarForm) {
        addCarForm.addEventListener('submit', handleAddCar);
    }
}

async function handleAddCar(e) {
    e.preventDefault();

    const make = document.getElementById('car-make').value;
    const model = document.getElementById('car-model').value;
    const price = document.getElementById('car-price').value;
    const imageFile = document.getElementById('car-image').files[0];

    if (!imageFile) {
        alert("Please select an image");
        return;
    }

    const reader = new FileReader();
    reader.onloadend = async function () {
        const base64Image = reader.result;

        const carData = {
            make: make,
            model: model,
            price: parseFloat(price),
            image: base64Image,
            imageType: "base64"
        };

        try {
            const response = await fetch('/api/cars', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(carData)
            });

            if (response.ok) {
                alert('Car added successfully!');
                document.getElementById('add-car-modal').classList.remove('active');
                document.getElementById('add-car-form').reset();
                if (carListEl) fetchCars(); // Refresh list if on cars page
            } else {
                const errorText = await response.text();
                alert('Failed to add car: ' + errorText);
            }
        } catch (error) {
            console.error('Error adding car:', error);
            alert('Error adding car');
        }
    };
    reader.readAsDataURL(imageFile);
}

// Run
init();
