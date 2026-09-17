const API_BASE = 'http://localhost:8080/api';
let stompClient = null;
let currentRideId = null;

// --- UI Navigation ---
function switchAuthTab(tab) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelector(`[onclick="switchAuthTab('${tab}')"]`).classList.add('active');
    
    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    document.getElementById('auth-error').innerText = '';
}

function toggleLicenseField() {
    const role = document.getElementById('register-role').value;
    document.getElementById('license-group').style.display = role === 'ROLE_DRIVER' ? 'block' : 'none';
}

function showSection(sectionId) {
    // List of all sections
    const sections = [
        'home-section', 'about-section', 'safety-section', 
        'career-section', 'blog-section', 'press-section', 
        'contact-section', 'auth-section', 'rider-section', 'driver-section'
    ];
    
    sections.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.style.display = 'none';
    });
    
    const target = document.getElementById(sectionId);
    if(target) target.style.display = 'block';
    
    updateNavButtons();
}

function updateNavButtons() {
    const isLoggedIn = localStorage.getItem('jwt') !== null;
    const loginBtn = document.getElementById('nav-login-btn');
    const logoutBtn = document.getElementById('nav-logout-btn');
    
    if (isLoggedIn) {
        if(loginBtn) loginBtn.style.display = 'none';
        if(logoutBtn) logoutBtn.style.display = 'block';
        
        // If they click on the logo or home, redirect to dashboard if logged in
        // (Optional: leaving it as Home for now, but usually it goes to dashboard)
    } else {
        if(loginBtn) loginBtn.style.display = 'block';
        if(logoutBtn) logoutBtn.style.display = 'none';
    }
}

// --- Authentication ---
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        if (!res.ok) throw new Error('Invalid credentials');
        const data = await res.json();
        
        localStorage.setItem('jwt', data.token);
        localStorage.setItem('role', data.role);
        
        initDashboard();
        updateNavButtons();
    } catch (err) {
        document.getElementById('auth-error').innerText = err.message;
    }
});

document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        email: document.getElementById('register-email').value,
        password: document.getElementById('register-password').value,
        name: document.getElementById('register-name').value,
        phone: document.getElementById('register-phone').value,
        role: document.getElementById('register-role').value,
        licenseNumber: document.getElementById('register-license').value
    };

    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        
        if (!res.ok) throw new Error(await res.text());
        switchAuthTab('login');
        document.getElementById('auth-error').innerText = 'Registration successful! Please login.';
        document.getElementById('auth-error').style.color = 'var(--success)';
    } catch (err) {
        document.getElementById('auth-error').innerText = err.message;
        document.getElementById('auth-error').style.color = '#ef4444';
    }
});

function logout() {
    localStorage.removeItem('jwt');
    localStorage.removeItem('role');
    if (stompClient) stompClient.disconnect();
    showSection('home-section');
}

// --- App Initialization ---
function initDashboard() {
    const role = localStorage.getItem('role');
    if (!role) return;

    if (role === 'ROLE_RIDER') {
        showSection('rider-section');
        connectWebSocket();
    } else if (role === 'ROLE_DRIVER') {
        showSection('driver-section');
        fetchDriverCurrentRides();
        connectWebSocket();
    }
}

// --- WebSocket ---
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs
    stompClient.connect({}, function (frame) {
        if(currentRideId) {
            subscribeToRide(currentRideId);
        }
    });
}

function subscribeToRide(rideId) {
    if(!stompClient || !stompClient.connected) return;
    currentRideId = rideId;
    stompClient.subscribe('/topic/rides/' + rideId, function (message) {
        const ride = JSON.parse(message.body);
        updateRideUI(ride);
    });
}

function updateRideUI(ride) {
    const role = localStorage.getItem('role');
    if(role === 'ROLE_RIDER') {
        document.getElementById('rider-active-ride').style.display = 'block';
        document.getElementById('rider-ride-status').innerText = ride.status;
        document.getElementById('rider-driver-name').innerText = ride.driver ? ride.driver.name : 'Searching...';
        document.getElementById('rider-fare').innerText = ride.fare.toFixed(2);
        document.getElementById('rider-distance').innerText = ride.distance.toFixed(2);
    } else {
        document.getElementById('driver-active-ride').style.display = 'block';
        document.getElementById('driver-ride-status').innerText = ride.status;
        
        let riderDisplay = ride.rider.name;
        if(ride.pickupLocation) riderDisplay += ` (Pickup: ${ride.pickupLocation})`;
        
        document.getElementById('driver-rider-name').innerText = riderDisplay;
        document.getElementById('driver-fare').innerText = ride.fare.toFixed(2);
        
        const actionsDiv = document.getElementById('driver-ride-actions');
        if (ride.status === 'REQUESTED') {
            actionsDiv.innerHTML = `<button class="btn success-btn" onclick="updateRideStatus('ACCEPTED')">Accept</button>`;
        } else if (ride.status === 'ACCEPTED') {
            actionsDiv.innerHTML = `<button class="btn primary-btn" onclick="updateRideStatus('ONGOING')">Start Trip</button>`;
        } else if (ride.status === 'ONGOING') {
            actionsDiv.innerHTML = `<button class="btn secondary-btn" onclick="updateRideStatus('COMPLETED')">Complete Trip</button>`;
        } else {
            actionsDiv.innerHTML = '';
        }
    }
}

// --- Rider Functions ---
document.getElementById('ride-request-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        pickupLocation: document.getElementById('pickup-location').value,
        dropoffLocation: document.getElementById('dropoff-location').value,
        serviceType: document.getElementById('ride-service-type').value
    };

    const res = await fetch(`${API_BASE}/ride/request`, {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem('jwt')
        },
        body: JSON.stringify(payload)
    });
    
    if (res.ok) {
        const ride = await res.json();
        subscribeToRide(ride.id);
        updateRideUI(ride);
    }
});

// --- Driver Functions ---
async function updateDriverLocation() {
    const payload = {
        latitude: parseFloat(document.getElementById('driver-lat').value),
        longitude: parseFloat(document.getElementById('driver-lng').value),
        available: document.getElementById('driver-available-toggle').checked
    };

    await fetch(`${API_BASE}/driver/location`, {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem('jwt')
        },
        body: JSON.stringify(payload)
    });
}

async function fetchDriverCurrentRides() {
    const res = await fetch(`${API_BASE}/ride/driver`, {
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('jwt') }
    });
    if(res.ok) {
        const rides = await res.json();
        // Just grab the most recent non-completed ride
        const activeRide = rides.find(r => r.status !== 'COMPLETED' && r.status !== 'CANCELLED');
        if (activeRide) {
            subscribeToRide(activeRide.id);
            updateRideUI(activeRide);
        }
    }
}

async function updateRideStatus(status) {
    if(!currentRideId) return;
    const res = await fetch(`${API_BASE}/ride/${currentRideId}/status?status=${status}`, {
        method: 'PUT',
        headers: { 'Authorization': 'Bearer ' + localStorage.getItem('jwt') }
    });
    if(res.ok) {
        const ride = await res.json();
        updateRideUI(ride);
    }
}

// Auto-init if token exists, else ensure nav buttons are correct
updateNavButtons();
if (localStorage.getItem('jwt')) {
    initDashboard();
} else {
    showSection('home-section');
}
