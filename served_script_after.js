// State
let cars = [];
let filteredCars = [];
let bookings = [];
let currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

// Price scaling: convert seed/base prices to more realistic values for display
// Logic: if stored price seems like a small base (<= 1000), scale by 100 to approximate INR/day
// This keeps already-large values unaffected.
function getDisplayedPrice(basePrice) {
    const p = parseFloat(basePrice) || 0;
    const scale = p <= 1000 ? 100 : 1;
    return parseFloat((p * scale).toFixed(2));
}

function formatCurrency(amount) {
    // Format as INR rounded
    return '₹' + Number(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 });
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

// Theme: initialize from localStorage
function initTheme() {
    const theme = localStorage.getItem('theme') || 'dark';
    if (theme === 'dark') document.body.classList.add('dark-theme');
    else document.body.classList.remove('dark-theme');
    const btn = document.getElementById('theme-toggle');
    if (btn) btn.textContent = document.body.classList.contains('dark-theme') ? '☀' : '☾';
}

function toggleTheme() {
    document.body.classList.toggle('dark-theme');
    const active = document.body.classList.contains('dark-theme') ? 'dark' : 'light';
    localStorage.setItem('theme', active);
    const btn = document.getElementById('theme-toggle');
    if (btn) btn.textContent = document.body.classList.contains('dark-theme') ? '☀' : '☾';
}

// Initialize
function init() {
    initTheme();
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

    const authLinks = navLinks.querySelectorAll('.auth-link');
    authLinks.forEach(link => link.remove());

    currentUser = JSON.parse(localStorage.getItem('currentUser')) || null;

    if (currentUser) {
        const logoutLink = document.createElement('a');
        logoutLink.href = '#';
        logoutLink.textContent = `Logout (${currentUser.username})`;
        logoutLink.className = 'auth-link';
        logoutLink.onclick = (e) => { e.preventDefault(); logout(); };
        
        const userInfo = document.createElement('span');
        userInfo.textContent = currentUser.username;
        userInfo.className = 'user-info';
        
        navLinks.appendChild(logoutLink);
    } else {
        const loginLink = document.createElement('a');
        loginLink.href = 'login.html';
        loginLink.textContent = 'Login';
        loginLink.className = 'auth-link';
        navLinks.appendChild(loginLink);
    }
}

function logout() {
    localStorage.removeItem('currentUser');
    currentUser = null;
    window.location.href = 'index.html';
}

// Update Admin Button Visibility
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

// Open Add Car Modal
function openAddCarModal() {
    const modal = document.getElementById('add-car-modal');
    if (modal) {
        modal.classList.add('active');
    }
}

// Close Add Car Modal
function closeAddCarModal() {
    const modal = document.getElementById('add-car-modal');
    if (modal) {
        modal.classList.remove('active');
    }
}

// Fetch Cars from API
async function fetchCars() {
    try {
        if (carListEl) carListEl.innerHTML = '<div class="loading">Loading cars...</div>';
        
        const response = await fetch('/api/cars');
        if (!response.ok) throw new Error('Failed to load car data');
        const data = await response.json();
        
        cars = Array.isArray(data) ? data : [];
        filteredCars = [...cars];
        // reset pagination
        currentPage = 1;
        renderCars();
    } catch (error) {
        console.error('Error loading cars:', error);
        if (carListEl) {
            carListEl.innerHTML = '<p class="empty-state">Error loading cars. Please try again later.</p>';
        }
    }
}

// Render Cars
function renderCars() {
    if (!carListEl) return;
    
    if (filteredCars.length === 0) {
        carListEl.innerHTML = '<p class="empty-state">No cars found. Try adjusting your search or filters.</p>';
        return;
    }
    
    carListEl.innerHTML = filteredCars.map(car => {
        const imageUrl = car.image && car.image.startsWith('http') ? car.image : 
                        (car.image && car.image.startsWith('images/') ? car.image : 
                        'images/default.jpg');
        
        const displayed = getDisplayedPrice(car.price);
        return `
            <div class="car-card">
                <div class="car-image-container">
                    <img src="${imageUrl}" alt="${car.name}" class="car-image" onerror="this.src='images/default.jpg'">
                    <div class="car-badge">${formatCurrency(displayed)}/day</div>
                </div>
                <div class="car-details">
                    <h3 class="car-title">${escapeHtml(car.name)}</h3>
                    <div class="car-features">
                        ${car.features ? escapeHtml(car.features).split(',').slice(0, 3).map(f => `<span class="feature-tag">${f.trim()}</span>`).join('') : ''}
                    </div>
                    <button onclick="openBookingModal(${car.id})" class="btn btn-primary btn-block">
                        Rent Now
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// Search and Filter Setup
function setupSearchFilters() {
    if (searchInput) {
        searchInput.addEventListener('input', applyFilters);
    }
    if (priceFilter) {
        priceFilter.addEventListener('change', applyFilters);
    }
    if (sortFilter) {
        sortFilter.addEventListener('change', applyFilters);
    }
}

function applyFilters() {
    let filtered = [...cars];
    
    // Search filter
    if (searchInput && searchInput.value) {
        const searchTerm = searchInput.value.toLowerCase();
        filtered = filtered.filter(car => 

    // Apply pagination
    const total = filteredCars.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    if (currentPage > totalPages) currentPage = totalPages;
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    const pageItems = filteredCars.slice(start, end);

    carListEl.innerHTML = pageItems.map(car => {
        );
    }
    
        
        return `
        filtered = filtered.filter(car => {
            const price = car.price;
                    <img data-src="${imageUrl}" src="images/default.jpg" alt="${car.name}" class="car-image lazy" onerror="this.src='images/default.jpg'">
                    <div class="car-badge">${formatCurrency(displayed)}/day</div>
                case 'medium': return price > 50 && price <= 100;
                case 'high': return price > 100;
                default: return true;
            }
        });
    }
    
    // Sort
    if (sortFilter) {
        switch(sortFilter.value) {
            case 'name':
                filtered.sort((a, b) => a.name.localeCompare(b.name));
                break;

    renderPagination(total, totalPages);
    observeLazyImages();
            case 'price-low':

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

    // show up to 5 page buttons centered
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

    // bind page size selector
    const pageSizeSel = document.getElementById('page-size');
    if (pageSizeSel) {
        pageSizeSel.value = String(pageSize);
        pageSizeSel.onchange = (e) => {
            pageSize = parseInt(e.target.value, 10);
            localStorage.setItem('pageSize', String(pageSize));
            currentPage = 1;
            renderCars();
        };
    }
}

function changePage(page) {
    currentPage = page;
    renderCars();
}

function observeLazyImages() {
    const imgs = document.querySelectorAll('img.car-image.lazy');
    if ('IntersectionObserver' in window) {
        if (!imageObserver) {
            imageObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const img = entry.target;
                        img.src = img.dataset.src || img.src;
                        img.classList.remove('lazy');
                        img.classList.add('loaded');
                        imageObserver.unobserve(img);
                    }
                });
            }, { rootMargin: '100px' });
        }
        imgs.forEach(img => imageObserver.observe(img));
    } else {
        // Fallback: load all
        imgs.forEach(img => { img.src = img.dataset.src || img.src; img.classList.add('loaded'); });
    }
}
                filtered.sort((a, b) => a.price - b.price);
                break;
            case 'price-high':
                filtered.sort((a, b) => b.price - a.price);
                break;
        }
    }
    
    filteredCars = filtered;
    renderCars();
}

// Fetch Bookings
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
        if (bookingListEl) {
            bookingListEl.innerHTML = '<p class="empty-state">Error loading bookings. Please try again later.</p>';
        }
    }
}

function renderBookings() {
    if (!bookingListEl) return;
    
    if (bookings.length === 0) {
        bookingListEl.innerHTML = '<p class="empty-state">No bookings found. Start by renting a car!</p>';
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
                    <p><strong>Total:</strong> ₹${booking.totalPrice ? booking.totalPrice.toFixed(2) : '0.00'}</p>
                </div>
                <div class="booking-status status-${statusClass}">${booking.status || 'PENDING'}</div>
            </div>
        `;
    }).join('');
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        const date = new Date(dateStr);
        return date.toLocaleDateString();
    } catch {
        return dateStr;
    }
}

// Open Booking Modal
window.openBookingModal = function(carId) {
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
    if (modalPrice) modalPrice.textContent = getDisplayedPrice(car.price).toFixed(2);
    
    // Set default times
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
    
    const price = parseFloat(modalPrice.textContent) || 0;
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
    modalTotal.textContent = total.toFixed(2);
}

// Handle Booking Submission
async function handleBooking(e) {
    e.preventDefault();
    
    if (!currentUser) {
        alert('Please login to book a car');
        window.location.href = 'login.html';
        return;
    }
    
    const carId = parseInt(carIdInput.value);
    const pickup = new Date(pickupTimeInput.value);
    const dropoff = new Date(dropoffTimeInput.value);
    const total = parseFloat(modalTotal.textContent);
    
    if (dropoff <= pickup) {
        alert("Dropoff time must be after pickup time");
        return;
    }
    
    // Format dates for backend (YYYY-MM-DD)
    const startDate = pickup.toISOString().split('T')[0];
    const endDate = dropoff.toISOString().split('T')[0];
    
    const bookingData = {
        carId: carId,
        startDate: startDate,
        endDate: endDate,
        totalPrice: total
    };
    
    try {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Booking...';
        }
        
        const response = await fetch('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(bookingData)
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            alert('Booking Confirmed!');
            closeModal();
            if (bookingForm) bookingForm.reset();
            if (bookingListEl) fetchBookings();
            if (carListEl) fetchCars();
        } else {
            alert(data.message || 'Failed to create booking');
        }
    } catch (error) {
        console.error('Error creating booking:', error);
        alert('Error creating booking. Please try again.');
    } finally {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Confirm Booking';
        }
    }
}

// Handle Login
async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const submitBtn = e.target.querySelector('button[type="submit"]');
    
    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Logging in...';
        
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        if (data.success) {
            localStorage.setItem('currentUser', JSON.stringify({ 
                username: data.username, 
                role: data.role, 
                id: data.id,
                email: data.email 
            }));
            window.location.href = 'index.html';
        } else {
            alert(data.message || 'Login failed');
        }
    } catch (error) {
        console.error('Login error:', error);
        alert('An error occurred during login. Please try again.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Login';
    }
}

// Handle Signup
async function handleSignup(e) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const role = document.getElementById('role') ? document.getElementById('role').value : 'USER';
    const submitBtn = e.target.querySelector('button[type="submit"]');
    
    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Signing up...';
        
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
            alert(data.message || 'Signup failed');
        }
    } catch (error) {
        console.error('Signup error:', error);
        alert('An error occurred during signup. Please try again.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign Up';
    }
}

// Handle Add Car (Admin)
async function handleAddCar(e) {
    e.preventDefault();
    
    if (!currentUser || currentUser.role !== 'ADMIN') {
        alert('You do not have permission to add cars');
        return;
    }
    
    const make = document.getElementById('car-make').value.trim();
    const model = document.getElementById('car-model').value.trim();
    const price = document.getElementById('car-price').value.trim();
    const imageUrl = document.getElementById('car-image-url').value.trim();
    const features = document.getElementById('car-features').value.trim();
    
    if (!make || !model || !price || !imageUrl) {
        alert('Please fill in all required fields');
        return;
    }
    
    const carData = {
        name: `${make} ${model}`,
        price: parseFloat(price),
        image: imageUrl,
        features: features
    };
    
    try {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Adding...';
        }
        
        const response = await fetch('/api/admin/cars', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
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
        alert('Error adding car. Please try again.');
    } finally {
        const submitBtn = e.target.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Add Car';
        }
    }
}

// Event Listeners
function setupEventListeners() {
    closeModalBtn.forEach(btn => {
        btn.addEventListener('click', closeModal);
    });
    
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
    
    // Add Car Modal Close Button
    const addCarCloseBtn = document.querySelector('#add-car-modal .close-btn');
    if (addCarCloseBtn) {
        addCarCloseBtn.addEventListener('click', closeAddCarModal);
    }
    
    const addCarForm = document.getElementById('add-car-form');
    if (addCarForm) {
        addCarForm.addEventListener('submit', handleAddCar);
    }

    // Theme toggle
    const themeBtn = document.getElementById('theme-toggle');
    if (themeBtn) themeBtn.addEventListener('click', toggleTheme);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Run initialization
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}