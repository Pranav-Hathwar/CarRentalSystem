// State
let cars = [];
let filteredCars = [];
let bookings = [];
let currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

// Price scaling
function getDisplayedPrice(basePrice) {
    const p = parseFloat(basePrice) || 0;
    const scale = p <= 1000 ? 100 : 1;
    return parseFloat((p * scale).toFixed(2));
}

function formatCurrency(amount) {
    return '₹' + Number(amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// Get reliable image URL
function getImageUrl(carImage) {
    if (!carImage || carImage.trim() === '') return 'images/default.jpg';
    if (carImage.startsWith('http://') || carImage.startsWith('https://')) return carImage;
    if (carImage.startsWith('images/')) return carImage;
    if (!carImage.includes('/')) return 'images/' + carImage;
    return carImage;
}

// Pagination state
let currentPage = 1;
let pageSize = parseInt(localStorage.getItem('pageSize') || '6', 10);
let imageObserver = null;

// DOM Elements
const carListEl = document.getElementById('car-list');
const bookingListEl = document.getElementById('booking-list');
const modal = document.getElementById('booking-modal');
const closeModalBtn = document.querySelectorAll('.close-modal');
const bookingForm = document.getElementById('booking-form');
const modalCarName = document.getElementById('modal-car-name');
const modalPrice = document.getElementById('modal-price');
const modalTotal = document.getElementById('modal-total');
const pickupTimeInput = document.getElementById('pickup-time');
const dropoffTimeInput = document.getElementById('dropoff-time');
const carIdInput = document.getElementById('car-id');
const loginForm = document.getElementById('loginForm');
const signupForm = document.getElementById('signupForm');
const navLinks = document.querySelector('.nav-links');
const searchInput = document.getElementById('search-input');
const priceFilter = document.getElementById('price-filter');
const sortFilter = document.getElementById('sort-filter');

// Initialize
function init() {
    updateNav();
    if (carListEl) {
        fetchCars();
        setupSearchFilters();
        updateAdminButton();
    }
    if (bookingListEl) {
        if (!currentUser) {
            window.location.href = 'login.html';
            return;
        }
        fetchBookings();
    }
    setupEventListeners();
}

function updateNav() {
    if (!navLinks) return;
    // Ensure link elements exist and control visibility by id
    const homeLink = document.getElementById('home-link');
    const carsLink = document.getElementById('cars-link');
    const bookingsLink = document.getElementById('bookings-link');
    const adminDashboardLink = document.getElementById('admin-dashboard-link');
    const loginLink = document.getElementById('login-link');
    const signupLink = document.getElementById('signup-link');

    // hide everything by default (only show what the role allows)
    [homeLink, carsLink, bookingsLink, adminDashboardLink, loginLink, signupLink].forEach(el => {
        if (el) el.style.display = 'none';
    });

    currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

    if (!currentUser) {
        // not logged in: show Home, Cars, Login, Signup
        if (homeLink) homeLink.style.display = 'inline-block';
        if (carsLink) carsLink.style.display = 'inline-block';
        if (loginLink) loginLink.style.display = 'inline-block';
        if (signupLink) signupLink.style.display = 'inline-block';
    } else if (currentUser.role === 'ADMIN') {
        // admin: show Home, Cars, Admin Dashboard
        if (homeLink) homeLink.style.display = 'inline-block';
        if (carsLink) carsLink.style.display = 'inline-block';
        if (adminDashboardLink) adminDashboardLink.style.display = 'inline-block';
    } else {
        // normal user: show Home, Cars, Bookings
        if (homeLink) homeLink.style.display = 'inline-block';
        if (carsLink) carsLink.style.display = 'inline-block';
        if (bookingsLink) bookingsLink.style.display = 'inline-block';
    }

    // remove existing dynamic auth links
    const authLinks = navLinks.querySelectorAll('.auth-link');
    authLinks.forEach(link => link.remove());

    // append logout if logged in, or ensure login/signup are visible when not logged in
    if (currentUser) {
        const logoutLink = document.createElement('a');
        logoutLink.href = '#';
        logoutLink.textContent = `Logout (${currentUser.username})`;
        logoutLink.className = 'auth-link';
        logoutLink.onclick = (e) => { e.preventDefault(); logout(); };
        navLinks.appendChild(logoutLink);
    }
}

async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
        console.error('Logout failed', e);
    }
    localStorage.removeItem('currentUser');
    currentUser = null;
    window.location.href = 'index.html';
}

function updateAdminButton() {
    const adminBtn = document.getElementById('admin-add-car-btn');
    if (!adminBtn) return;
    currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;
    if (currentUser && currentUser.role === 'ADMIN') {
        adminBtn.style.display = 'inline-block';
    } else {
        adminBtn.style.display = 'none';
    }
}

function openAddCarModal() {
    const modal = document.getElementById('add-car-modal');
    if (modal) modal.classList.add('active');
}

function closeAddCarModal() {
    const modal = document.getElementById('add-car-modal');
    if (modal) modal.classList.remove('active');
}

async function fetchCars() {
    try {
        if (carListEl) carListEl.innerHTML = '<div class="loading">Loading cars...</div>';
        const response = await fetch('/api/cars');
        if (!response.ok) throw new Error('Failed to load car data');
        const data = await response.json();
        cars = Array.isArray(data) ? data : [];
        filteredCars = [...cars];
        currentPage = 1;
        renderCars();
    } catch (error) {
        console.error('Error loading cars:', error);
        if (carListEl) carListEl.innerHTML = '<p class="empty-state">Error loading cars. Please try again later.</p>';
    }
}

function renderCars() {
    if (!carListEl) return;
    if (filteredCars.length === 0) {
        carListEl.innerHTML = '<p class="empty-state">No cars found. Try adjusting your search or filters.</p>';
        return;
    }

    const total = filteredCars.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    if (currentPage > totalPages) currentPage = totalPages;
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    const pageItems = filteredCars.slice(start, end);

    carListEl.innerHTML = pageItems.map(car => {
        const imageUrl = getImageUrl(car.image);
        const displayed = getDisplayedPrice(car.price);
        return `
            <div class="car-card">
                <div class="car-image-container">
                    <img src="${imageUrl}" alt="${car.name}" class="car-image" 
                         onerror="this.src='images/default.jpg'; this.onerror=null;" 
                         loading="lazy">
                    <div class="car-badge">${formatCurrency(displayed)}/day</div>
                </div>
                <div class="car-details">
                    <h3 class="car-title">${escapeHtml(car.name)}</h3>
                    <p class="car-meta" style="color:var(--text-light); margin-bottom:0.6rem; font-size:0.95rem;">${escapeHtml(car.type || 'CAR')} ${car.registrationNumber ? ' • ' + escapeHtml(car.registrationNumber) : ''}</p>
                    <div class="car-features">
                        ${car.features ? escapeHtml(car.features).split(',').slice(0, 3).map(f => `<span class="feature-tag">${f.trim()}</span>`).join('') : ''}
                    </div>
                    <button onclick="openBookingModal(${car.id})" class="btn btn-primary btn-block">Rent Now</button>
                </div>
            </div>
        `;
    }).join('');

    renderPagination(total, totalPages);
}

function setupSearchFilters() {
    if (searchInput) searchInput.addEventListener('input', applyFilters);
    if (priceFilter) priceFilter.addEventListener('change', applyFilters);
    if (sortFilter) sortFilter.addEventListener('change', applyFilters);
}

function applyFilters() {
    let filtered = [...cars];

    if (searchInput && searchInput.value) {
        const searchTerm = searchInput.value.toLowerCase();
        filtered = filtered.filter(car =>
            car.name.toLowerCase().includes(searchTerm) ||
            (car.type && car.type.toLowerCase().includes(searchTerm))
        );
    }

    if (priceFilter) {
        const value = priceFilter.value;
        filtered = filtered.filter(car => {
            const price = getDisplayedPrice(car.price); // Filter based on displayed price
            switch (value) {
                case 'low': return price <= 2000; // Adjusted for INR
                case 'medium': return price > 2000 && price <= 5000;
                case 'high': return price > 5000;
                default: return true;
            }
        });
    }

    if (sortFilter) {
        switch (sortFilter.value) {
            case 'name': filtered.sort((a, b) => a.name.localeCompare(b.name)); break;
            case 'price-low': filtered.sort((a, b) => getDisplayedPrice(a.price) - getDisplayedPrice(b.price)); break;
            case 'price-high': filtered.sort((a, b) => getDisplayedPrice(b.price) - getDisplayedPrice(a.price)); break;
        }
    }

    filteredCars = filtered;
    currentPage = 1;
    renderCars();
}

function renderPagination(totalItems, totalPages) {
    const paginationEl = document.getElementById('pagination');
    if (!paginationEl) return;
    paginationEl.innerHTML = '';

    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.textContent = 'Prev';
    prevBtn.disabled = currentPage === 1;
    prevBtn.onclick = () => changePage(currentPage - 1);
    paginationEl.appendChild(prevBtn);

    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, startPage + 4);
    for (let p = startPage; p <= endPage; p++) {
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (p === currentPage ? ' active' : '');
        btn.textContent = p;
        btn.onclick = () => changePage(p);
        paginationEl.appendChild(btn);
    }

    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.textContent = 'Next';
    nextBtn.disabled = currentPage === totalPages;
    nextBtn.onclick = () => changePage(currentPage + 1);
    paginationEl.appendChild(nextBtn);
}

function changePage(page) {
    currentPage = page;
    renderCars();
}

async function fetchBookings() {
    try {
        if (!currentUser) {
            window.location.href = 'login.html';
            return;
        }
        const response = await fetch('/api/bookings');
        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = 'login.html';
                return;
            }
            throw new Error('Failed to load bookings');
        }
        const data = await response.json();
        bookings = Array.isArray(data) ? data : [];
        renderBookings();
    } catch (error) {
        console.error('Error loading bookings:', error);
        if (bookingListEl) bookingListEl.innerHTML = '<p class="empty-state">Error loading bookings.</p>';
    }
}

function renderBookings() {
    if (!bookingListEl) return;
    if (bookings.length === 0) {
        bookingListEl.innerHTML = '<p class="empty-state">No bookings found.</p>';
        return;
    }
    bookingListEl.innerHTML = bookings.map(booking => {
        const statusClass = booking.status ? booking.status.toLowerCase() : 'pending';
        return `
            <div class="booking-item">
                <div class="booking-info">
                    <h3>Booking #${booking.id}</h3>
                    <p><strong>Car ID:</strong> ${booking.carId}</p>
                    <p><strong>Dates:</strong> ${formatDate(booking.startDate)} to ${formatDate(booking.endDate)}</p>
                    <p><strong>Total:</strong> ${formatCurrency(booking.totalPrice || 0)}</p>
                </div>
                <div class="booking-actions">
                    <div class="booking-status status-${statusClass}">${booking.status || 'PENDING'}</div>
                    ${statusClass !== 'cancelled' ? `<button onclick="openCancellationModal(${booking.id})" class="btn btn-sm btn-danger" style="margin-top: 10px; background-color: #dc3545; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer;">Cancel</button>` : ''}
                </div>
            </div>
        `;
    }).join('');
}

function openCancellationModal(bookingId) {
    const modal = document.getElementById('cancellation-modal');
    const input = document.getElementById('cancel-booking-id');
    if (modal && input) {
        input.value = bookingId;
        modal.classList.add('active');
    }
}

function closeCancellationModal() {
    const modal = document.getElementById('cancellation-modal');
    if (modal) modal.classList.remove('active');
}

async function handleCancellation(e) {
    e.preventDefault();
    const bookingId = document.getElementById('cancel-booking-id').value;
    const reason = document.getElementById('cancel-reason').value.trim();

    if (!bookingId || !reason) {
        alert('Please provide a reason for cancellation.');
        return;
    }

    try {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Cancelling...'; }

        const response = await fetch('/api/bookings/cancel', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ bookingId: parseInt(bookingId), reason: reason })
        });

        const data = await response.json();
        if (response.ok && data.success) {
            alert('Cancellation request submitted successfully.');
            closeCancellationModal();
            e.target.reset();
            fetchBookings();
        } else {
            alert(data.message || 'Failed to cancel booking.');
        }
    } catch (error) {
        console.error('Error cancelling booking:', error);
        alert('Error cancelling booking. Please try again.');
    } finally {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Confirm Cancellation'; }
    }
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        return new Date(dateStr).toLocaleDateString();
    } catch {
        return dateStr;
    }
}

window.openBookingModal = function (carId) {
    if (!currentUser) {
        alert('Please login to book a car');
        window.location.href = 'login.html';
        return;
    }
    const car = cars.find(c => c.id === carId);
    if (!car) {
        alert('Car not found');
        return;
    }
    if (carIdInput) carIdInput.value = car.id;
    if (modalCarName) modalCarName.textContent = car.name;
    if (modalPrice) modalPrice.textContent = formatCurrency(getDisplayedPrice(car.price));

    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    const tomorrow = new Date(now);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (pickupTimeInput) pickupTimeInput.value = now.toISOString().slice(0, 16);
    if (dropoffTimeInput) dropoffTimeInput.value = tomorrow.toISOString().slice(0, 16);
    updateTotal();
    if (modal) modal.classList.add('active');
};

function closeModal() {
    if (modal) modal.classList.remove('active');
}

function updateTotal() {
    if (!modalPrice || !pickupTimeInput || !dropoffTimeInput || !modalTotal) return;
    const price = parseFloat(modalPrice.textContent.replace(/[^0-9.]/g, '')) || 0;
    const pickup = new Date(pickupTimeInput.value);
    const dropoff = new Date(dropoffTimeInput.value);
    if (isNaN(pickup.getTime()) || isNaN(dropoff.getTime())) {
        modalTotal.textContent = "0.00";
        return;
    }
    const diffMs = dropoff - pickup;
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays <= 0) {
        modalTotal.textContent = "0.00";
        return;
    }
    const total = price * diffDays;
    modalTotal.textContent = formatCurrency(total);
}

async function handleBooking(e) {
    e.preventDefault();
    if (!currentUser) {
        alert('Please login to book a car');
        window.location.href = 'login.html';
        return;
    }
    const carId = parseInt(carIdInput.value);
    const pickupVal = pickupTimeInput.value;
    const dropoffVal = dropoffTimeInput.value;
    const pickup = new Date(pickupVal);
    const dropoff = new Date(dropoffVal);
    const total = parseFloat(modalTotal.textContent.replace(/[^0-9.]/g, ''));

    if (dropoff <= pickup) {
        alert("Dropoff time must be after pickup time");
        return;
    }

    const driverDob = document.getElementById('driver-dob').value;
    const licenseInput = document.getElementById('driver-license');
    const licensePath = licenseInput.files.length > 0 ? licenseInput.files[0].name : "";

    if (!driverDob) {
        alert("Please enter driver's date of birth");
        return;
    }

    const bookingData = {
        carId: carId,
        pickupDateTime: pickupVal,
        dropoffDateTime: dropoffVal,
        driverDob: driverDob,
        licensePath: licensePath,
        totalPrice: total
    };

    try {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Booking...'; }
        const response = await fetch('/api/bookings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(bookingData)
        });

        if (response.status === 401) {
            alert('Session expired. Please login again.');
            localStorage.removeItem('currentUser');
            window.location.href = 'login.html';
            return;
        }

        const data = await response.json();
        if (response.ok && data.success) {
            alert('Booking Confirmed!');
            closeModal();
            if (bookingForm) bookingForm.reset();
            if (bookingListEl) fetchBookings();
        } else {
            alert(data.message || 'Failed to create booking');
        }
    } catch (error) {
        console.error('Error creating booking:', error);
        alert('Error creating booking. Please try again.');
    } finally {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Confirm Booking'; }
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();
    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (!email || !password) { alert('Please enter both email and password'); return; }
    try {
        submitBtn.disabled = true; submitBtn.textContent = 'Logging in...';
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, password })
        });
        const data = await response.json();
        if (data.success) {
            localStorage.setItem('currentUser', JSON.stringify({
                username: data.username, role: data.role, id: data.id, email: data.email
            }));
            if (data.role === 'ADMIN') window.location.href = 'admin-dashboard.html';
            else window.location.href = 'index.html';
        } else {
            alert(data.message || 'Login failed.');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('An error occurred during login.');
    } finally {
        submitBtn.disabled = false; submitBtn.textContent = 'Login';
    }
}

async function handleSignup(e) {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value.trim();
    const role = 'USER';
    const submitBtn = e.target.querySelector('button[type="submit"]');
    if (!username || !email || !password) { alert('Please fill in all fields'); return; }
    if (password.length < 6) { alert('Password must be at least 6 characters long'); return; }
    try {
        submitBtn.disabled = true; submitBtn.textContent = 'Signing up...';
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: username, email, password, role })
        });
        const data = await response.json();
        if (data.success) {
            alert('Registration successful! Please login.');
            window.location.href = 'login.html';
        } else {
            alert(data.message || 'Signup failed.');
        }
    } catch (error) {
        console.error('Signup error:', error);
        alert('An error occurred during signup.');
    } finally {
        submitBtn.disabled = false; submitBtn.textContent = 'Create Account';
    }
}

async function handleAddCar(e) {
    e.preventDefault();
    if (!currentUser || currentUser.role !== 'ADMIN') { alert('Permission denied'); return; }
    const make = document.getElementById('car-make').value.trim();
    const model = document.getElementById('car-model').value.trim();
    const price = document.getElementById('car-price').value.trim();
    const imageUrl = document.getElementById('car-image-url').value.trim();
    const features = document.getElementById('car-features').value.trim();
    if (!make || !model || !price || !imageUrl) { alert('Please fill in required fields'); return; }
    const carData = {
        name: `${make} ${model}`,
        price: parseFloat(price),
        image: imageUrl,
        features: features
    };
    try {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Adding...'; }
        const response = await fetch('/api/admin/cars', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(carData)
        });
        const data = await response.json();
        if (response.ok && data.success) {
            alert('Car added successfully!');
            closeAddCarModal();
            e.target.reset();
            if (carListEl) fetchCars();
        } else {
            alert(data.message || 'Failed to add car');
        }
    } catch (error) {
        console.error('Error adding car:', error);
        alert('Error adding car.');
    } finally {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Add Car'; }
    }
}

function setupEventListeners() {
    closeModalBtn.forEach(btn => btn.addEventListener('click', closeModal));
    window.addEventListener('click', (e) => {
        if (modal && e.target === modal) closeModal();
        const addCarModal = document.getElementById('add-car-modal');
        if (addCarModal && e.target === addCarModal) closeAddCarModal();
    });
    if (pickupTimeInput) pickupTimeInput.addEventListener('change', updateTotal);
    if (dropoffTimeInput) dropoffTimeInput.addEventListener('change', updateTotal);
    if (bookingForm) bookingForm.addEventListener('submit', handleBooking);
    if (loginForm) loginForm.addEventListener('submit', handleLogin);
    if (signupForm) signupForm.addEventListener('submit', handleSignup);
    const addCarCloseBtn = document.querySelector('#add-car-modal .close-btn');
    if (addCarCloseBtn) addCarCloseBtn.addEventListener('click', closeAddCarModal);
    const addCarForm = document.getElementById('add-car-form');
    if (addCarForm) addCarForm.addEventListener('submit', handleAddCar);

    // Cancellation Modal
    const cancelModal = document.getElementById('cancellation-modal');
    const cancelCloseBtn = document.querySelector('#cancellation-modal .close-modal');
    const cancelForm = document.getElementById('cancellation-form');

    if (cancelCloseBtn) cancelCloseBtn.addEventListener('click', closeCancellationModal);
    if (cancelForm) cancelForm.addEventListener('submit', handleCancellation);

    window.addEventListener('click', (e) => {
        if (modal && e.target === modal) closeModal();
        const addCarModal = document.getElementById('add-car-modal');
        if (addCarModal && e.target === addCarModal) closeAddCarModal();
        if (cancelModal && e.target === cancelModal) closeCancellationModal();
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}