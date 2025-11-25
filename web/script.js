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
const daysInput = document.getElementById('days');
const carIdInput = document.getElementById('car-id');

// Initialize
function init() {
    fetchCars();
    setupEventListeners();
}

// Fetch Cars from JSON
async function fetchCars() {
    try {
        const response = await fetch('/api/cars');
        if (!response.ok) throw new Error('Failed to load car data');
        const data = await response.json();

        // Filter for valid JPEG images
        cars = data.filter(car => {
            const isJpeg = car.image.toLowerCase().endsWith('.jpg') || car.image.toLowerCase().endsWith('.jpeg') || car.image.startsWith('http'); // Allow http for demo images, but warn for local
            // For local files, we strictly enforce .jpg/.jpeg
            if (!car.image.startsWith('http') && !isJpeg) {
                console.warn(`Skipping car ${car.name}: Image must be JPEG format.`);
                return false;
            }
            return true;
        });

        renderCars();
    } catch (error) {
        console.error('Error loading cars:', error);
        carListEl.innerHTML = '<p class="empty-state">Error loading cars. Please try again later.</p>';
    }
}

// Render Cars
function renderCars() {
    carListEl.innerHTML = cars.map(car => `
        <div class="car-card">
            <img src="${car.image}" alt="${car.name}" class="car-image">
            <div class="car-details">
                <div class="car-title">
                    <span>${car.name}</span>
                    <span class="car-price">$${car.price}/day</span>
                </div>
                <div class="car-features">
                    ${car.features.map(f => `<span>• ${f}</span>`).join('')}
                </div>
                <button class="btn btn-primary btn-block" onclick="openBookingModal(${car.id})">
                    Book Now
                </button>
            </div>
        </div>
    `).join('');
}

// Open Modal
window.openBookingModal = function (carId) {
    const car = cars.find(c => c.id === carId);
    if (!car) return;

    carIdInput.value = car.id;
    modalCarName.textContent = car.name;
    modalPrice.textContent = car.price;
    daysInput.value = 1;
    updateTotal();

    modal.classList.add('active');
};

// Close Modal
function closeModal() {
    modal.classList.remove('active');
}

// Update Total Price
function updateTotal() {
    const price = parseFloat(modalPrice.textContent);
    const days = parseInt(daysInput.value) || 0;
    modalTotal.textContent = (price * days).toFixed(2);
}

// Handle Booking Submission
function handleBooking(e) {
    e.preventDefault();

    const carId = parseInt(carIdInput.value);
    const car = cars.find(c => c.id === carId);
    const name = document.getElementById('name').value;
    const days = parseInt(daysInput.value);
    const total = parseFloat(modalTotal.textContent);

    const booking = {
        id: Date.now(),
        car: car,
        name: name,
        days: days,
        total: total,
        status: 'Confirmed',
        date: new Date().toLocaleDateString()
    };

    bookings.unshift(booking);
    renderBookings();
    closeModal();
    bookingForm.reset();

    // Scroll to bookings
    document.getElementById('my-bookings').scrollIntoView({ behavior: 'smooth' });
}

// Render Bookings
function renderBookings() {
    if (bookings.length === 0) {
        bookingListEl.innerHTML = '<p class="empty-state">You haven\'t booked any cars yet.</p>';
        return;
    }

    bookingListEl.innerHTML = bookings.map(booking => `
        <div class="booking-item">
            <div class="booking-info">
                <h3>${booking.car.name}</h3>
                <p>${booking.days} days • Total: $${booking.total}</p>
                <p class="text-light" style="font-size: 0.875rem; color: #6b7280;">Booked on ${booking.date}</p>
            </div>
            <span class="booking-status status-confirmed">${booking.status}</span>
        </div>
    `).join('');
}

// Event Listeners
function setupEventListeners() {
    closeModalBtn.addEventListener('click', closeModal);

    window.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    daysInput.addEventListener('input', updateTotal);
    bookingForm.addEventListener('submit', handleBooking);

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
                addCarForm.reset();
                fetchCars(); // Refresh list
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
