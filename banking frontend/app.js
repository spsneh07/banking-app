// Base URLs for your Spring Boot backend
const AUTH_API_URL = 'http://localhost:8080/api/auth';
const ACCOUNT_API_URL = 'http://localhost:8080/api/account';

/**
 * Utility function to get the current page name (e.g., "login.html")
 */
function getPageName() {
    const path = window.location.pathname;
    return path.split("/").pop();
}

// --- Main execution logic ---
document.addEventListener('DOMContentLoaded', () => {
    const page = getPageName();
    if (page === 'login.html' || page === '' || page === 'index.html') {
        document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
    } else if (page === 'register.html') {
        document.getElementById('registerForm')?.addEventListener('submit', handleRegister);
    } else if (page === 'dashboard.html') {
        checkAuth();
        if (localStorage.getItem('authToken')) {
            setupDashboard();
        }
    }
});

/**
 * Sets up dashboard elements and fetches initial data.
 */
function setupDashboard() {
    // Page actions
    document.getElementById('logoutButton')?.addEventListener('click', handleLogout);
    
    // Form submit listeners (for modals)
    document.getElementById('depositForm')?.addEventListener('submit', handleDepositSubmit);
    document.getElementById('transferForm')?.addEventListener('submit', handleTransferSubmit);
    document.getElementById('payBillForm')?.addEventListener('submit', handlePayBillSubmit);
    
    // Verification button listener
    document.getElementById('verifyRecipientBtn')?.addEventListener('click', handleVerifyRecipient);

    // Reset transfer form if account number is changed after verification
    document.getElementById('transferAccountNumber')?.addEventListener('input', resetTransferForm);

    // Fetch initial data when the dashboard loads
    fetchUserDetails(); // <<< Fetches user details including account number
    fetchBalance();
    fetchTransactions();
}

/**
 * A helper function to make secure API calls with the JWT token.
 */
async function fetchSecure(url, options = {}) {
    const token = localStorage.getItem('authToken');
    if (!token) {
        alert("Authentication error. Please log in again.");
        window.location.href = 'login.html';
        throw new Error("No auth token found");
    }
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...options.headers,
    };
    return fetch(url, { ...options, headers });
}

/**
 * Fetches current user's details (including account number) and displays them.
 */
async function fetchUserDetails() {
    const accountNumberDisplay = document.getElementById('userAccountNumber');
    if (!accountNumberDisplay) return;

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/me`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const user = await response.json();
        if (user.accountNumber) {
            accountNumberDisplay.textContent = user.accountNumber;
        } else {
            accountNumberDisplay.textContent = "N/A";
        }
    } catch (error) {
        console.error('Error fetching user details:', error);
        accountNumberDisplay.textContent = "Error";
    }
}

/**
 * Fetches and displays the account balance.
 */
async function fetchBalance() {
    const balanceDisplay = document.getElementById('accountBalanceDisplay');
    if (!balanceDisplay) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/balance`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const balance = await response.json();
        balanceDisplay.textContent = `$ ${parseFloat(balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    } catch (error) {
        console.error('Error fetching balance:', error);
        balanceDisplay.textContent = "$ Error";
    }
}

/**
 * Fetches and displays recent transactions.
 */
async function fetchTransactions() {
    const tableBody = document.getElementById('transactionsTableBody');
    if (!tableBody) return;
    tableBody.innerHTML = '<tr><td colspan="4" class="text-center">Loading...</td></tr>';

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/transactions`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const transactions = await response.json();

        tableBody.innerHTML = '';
        if (transactions.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="4" class="text-center">No recent transactions found.</td></tr>';
            return;
        }

        transactions.forEach(tx => {
            const isCredit = tx.amount >= 0;
            const amountClass = isCredit ? 'text-success' : 'text-danger';
            const amountSign = isCredit ? '+' : '-';
            const formattedAmount = Math.abs(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
            let typeBadgeClass = 'bg-secondary';
            if (tx.type === 'DEPOSIT') typeBadgeClass = 'bg-success';
            else if (tx.type === 'TRANSFER' && isCredit) typeBadgeClass = 'bg-info';
            else if (tx.type === 'TRANSFER' && !isCredit) typeBadgeClass = 'bg-danger';
            else if (tx.type === 'PAYMENT') typeBadgeClass = 'bg-warning text-dark';

            const row = `
                <tr>
                    <td>${tx.timestamp}</td>
                    <td>${tx.description}</td>
                    <td><span class="badge ${typeBadgeClass}">${tx.type}</span></td>
                    <td class="${amountClass}">${amountSign}$${formattedAmount}</td>
                </tr>
            `;
            tableBody.insertAdjacentHTML('beforeend', row);
        });
    } catch (error) {
        console.error('Error fetching transactions:', error);
        tableBody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Error loading transactions.</td></tr>';
    }
}

/**
 * Handles the submit event from the Deposit modal form.
 */
async function handleDepositSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const amount = parseFloat(document.getElementById('depositAmount').value);
    const errorDiv = document.getElementById('depositError');
    const submitButton = document.getElementById('submitDeposit');
    
    hideModalError(errorDiv);
    if (isNaN(amount) || amount <= 0) {
        showModalError(errorDiv, "Please enter a valid, positive amount.");
        return;
    }
    
    toggleSpinner(submitButton, true);

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/deposit`, {
            method: 'POST',
            body: JSON.stringify({ amount }),
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(responseText);
        
        refreshDashboardData();
        bootstrap.Modal.getInstance(form.closest('.modal')).hide();
        form.reset();
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}

/**
 * Handles clicking the "Verify" button in the transfer modal.
 */
async function handleVerifyRecipient() {
    const accountNumber = document.getElementById('transferAccountNumber').value;
    const errorDiv = document.getElementById('transferError');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    
    hideModalError(errorDiv);
    if (!accountNumber) {
        showModalError(errorDiv, "Please enter an account number.");
        return;
    }

    toggleSpinner(verifyBtn, true);
    
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/verify-recipient?accountNumber=${accountNumber}`);
        const recipientName = await response.text();
        if (!response.ok) throw new Error(recipientName);

        // Success! Enable the rest of the form
        document.getElementById('verifiedRecipientName').textContent = recipientName;
        document.getElementById('recipientVerifiedInfo').classList.remove('d-none');
        document.getElementById('transferDetails').disabled = false;
        document.getElementById('submitTransfer').disabled = false;
        verifyBtn.textContent = 'Verified ✓';
        verifyBtn.classList.remove('btn-outline-secondary');
        verifyBtn.classList.add('btn-success');
        verifyBtn.disabled = true;

    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(verifyBtn, false);
    }
}

/**
 * Resets the transfer form if the account number is changed.
 */
function resetTransferForm() {
    document.getElementById('transferDetails').disabled = true;
    document.getElementById('submitTransfer').disabled = true;
    document.getElementById('recipientVerifiedInfo').classList.add('d-none');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    verifyBtn.textContent = 'Verify';
    verifyBtn.classList.remove('btn-success');
    verifyBtn.classList.add('btn-outline-secondary');
    verifyBtn.disabled = false;
    hideModalError(document.getElementById('transferError'));
}

/**
 * Handles the final submit event from the Transfer modal form.
 */
async function handleTransferSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const recipientAccountNumber = document.getElementById('transferAccountNumber').value;
    const amount = parseFloat(document.getElementById('transferAmount').value);
    const password = document.getElementById('transferPassword').value;
    const errorDiv = document.getElementById('transferError');
    const submitButton = document.getElementById('submitTransfer');

    hideModalError(errorDiv);
    // Basic validation (most should be covered by disabled fields)
    if (!recipientAccountNumber || isNaN(amount) || amount <= 0 || !password) {
        showModalError(errorDiv, "Please complete all fields.");
        return;
    }

    toggleSpinner(submitButton, true);

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/transfer`, {
            method: 'POST',
            body: JSON.stringify({ recipientAccountNumber, amount, password }),
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(responseText);
        
        refreshDashboardData();
        bootstrap.Modal.getInstance(form.closest('.modal')).hide();
        form.reset();
        resetTransferForm(); // Reset for next time

    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}

/**
 * Handles the submit event from the Pay Bill modal form.
 */
async function handlePayBillSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const billerName = document.getElementById('payBillBiller').value;
    const amount = parseFloat(document.getElementById('payBillAmount').value);
    const password = document.getElementById('payBillPassword').value;
    const errorDiv = document.getElementById('payBillError');
    const submitButton = document.getElementById('submitPayBill');

    hideModalError(errorDiv);
    if (!billerName) {
        showModalError(errorDiv, "Please enter a biller name."); return;
    }
    if (isNaN(amount) || amount <= 0) {
        showModalError(errorDiv, "Please enter a valid amount."); return;
    }
    if (!password) {
        showModalError(errorDiv, "Please enter your password to confirm."); return;
    }

    toggleSpinner(submitButton, true);

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/paybill`, {
            method: 'POST',
            body: JSON.stringify({ billerName, amount, password }),
        });
        const responseText = await response.text();
        if (!response.ok) throw new Error(responseText);
        
        refreshDashboardData();
        bootstrap.Modal.getInstance(form.closest('.modal')).hide();
        form.reset();
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}


// --- UTILITY AND AUTH FUNCTIONS ---

function showModalError(errorDiv, message) {
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.classList.remove('d-none');
    }
}

function hideModalError(errorDiv) {
    if (errorDiv) {
        errorDiv.textContent = '';
        errorDiv.classList.add('d-none');
    }
}

function toggleSpinner(button, show) {
    if (!button) return;
    const spinner = button.querySelector('.spinner-border');
    button.disabled = show;
    if (spinner) {
        show ? spinner.classList.remove('d-none') : spinner.classList.add('d-none');
    }
}

function refreshDashboardData() {
    fetchBalance();
    fetchTransactions();
}

async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    errorMessage.classList.add('d-none');
    try {
        const response = await fetch(`${AUTH_API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
        });
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('authToken', data.accessToken);
            localStorage.setItem('username', username);
            window.location.href = 'dashboard.html';
        } else {
            const errorText = await response.text();
            errorMessage.textContent = errorText || 'Invalid username or password.';
            errorMessage.classList.remove('d-none');
        }
    } catch (error) {
        console.error('Login error:', error);
        errorMessage.textContent = 'Cannot connect to the server.';
        errorMessage.classList.remove('d-none');
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const fullName = document.getElementById('fullName').value; // Get full name
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    errorMessage.classList.add('d-none');
    successMessage.classList.add('d-none');
    try {
        const response = await fetch(`${AUTH_API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username, email, password }), // Send full name
        });
        const responseText = await response.text();
        if (response.ok) {
            successMessage.classList.remove('d-none');
            event.target.reset();
        } else {
            errorMessage.textContent = responseText || `Registration failed (Status: ${response.status})`;
            errorMessage.classList.remove('d-none');
        }
    } catch (error) {
        console.error('Registration error:', error);
        errorMessage.textContent = 'Cannot connect to the server.';
        errorMessage.classList.remove('d-none');
    }
}

function checkAuth() {
    if (!localStorage.getItem('authToken')) {
        window.location.href = 'login.html';
    } else {
        const username = localStorage.getItem('username');
        const usernameDisplay = document.getElementById('username-display');
        if (usernameDisplay && username) {
            usernameDisplay.textContent = username;
        }
    }
}

function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    window.location.href = 'login.html';
}
