// Base URLs for your Spring Boot backend
const AUTH_API_URL = 'http://localhost:8080/api/auth';
const ACCOUNT_API_URL = 'http://localhost:8080/api/account';
const BANK_API_URL = 'http://localhost:8080/api/banks';

// Global variable to hold the chart instance
window.myTransactionChart = null;

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
    
    // --- THIS IS THE FIX ---
    // The logic is now separated correctly for each page.
    // checkAuth() is ONLY called on protected pages.

    if (page === 'login.html') {
        document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
    } else if (page === 'register.html') {
        document.getElementById('registerForm')?.addEventListener('submit', handleRegister);
    } else if (page === 'create-pin.html') {
        document.getElementById('pinSetupForm')?.addEventListener('submit', handlePinSetup);
    } else if (page === 'dashboard.html') { // This is the PORTAL dashboard
        checkAuth(); // Protect this page
        if (localStorage.getItem('authToken')) {
            setupPortalDashboard();
        }
    } else if (page === 'account.html') { // This is the ACCOUNT-specific dashboard
        checkAuth(); // Protect this page
        if (localStorage.getItem('authToken')) {
            setupAccountDashboard();
        }
    }
    // index.html (the landing page) does not need any specific logic here.

    // Setup theme toggle on all pages
    setupThemeToggle();
});

// --- NEW: Portal Dashboard Setup ---
function setupPortalDashboard() {
    document.getElementById('logoutButton')?.addEventListener('click', handleLogout);
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }
    fetchUserAccounts();
    fetchAllBanks();
}

// --- NEW: Account Dashboard Setup ---
function setupAccountDashboard() {
    document.getElementById('logoutButton')?.addEventListener('click', handleLogout);
    
    const urlParams = new URLSearchParams(window.location.search);
    const accountId = urlParams.get('id');
    const bankName = urlParams.get('name');

    if (!accountId) {
        alert("No account selected. Redirecting to portal.");
        window.location.href = 'dashboard.html';
        return;
    }
    
    document.body.dataset.accountId = accountId;
    document.getElementById('bankNameDisplay').textContent = bankName || "Account Details";
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }

    // Setup Tab Navigation
    const tabs = document.querySelectorAll('.nav-tab-btn');
    const panes = document.querySelectorAll('.nav-tab-content');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            panes.forEach(p => p.classList.add('hidden'));
            tab.classList.add('active');
            const paneId = tab.id.replace('-tab', '-pane');
            document.getElementById(paneId)?.classList.remove('hidden');

            if (paneId === 'profile-pane') populateProfileForm();
            if (paneId === 'transactions-pane') fetchTransactions(true);
            if (paneId === 'overview-pane') fetchTransactions(false);
        });
    });

    document.getElementById('depositBtn')?.addEventListener('click', () => showModal('depositModal'));
    document.getElementById('transferBtn')?.addEventListener('click', () => showModal('transferModal'));
    document.getElementById('payBillBtn')?.addEventListener('click', () => showModal('payBillModal'));
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault(); e.stopPropagation();
            const modal = e.target.closest('.fixed.z-50');
            hideModal(modal.id);
        });
    });
    
    document.getElementById('depositForm')?.addEventListener('submit', handleDepositSubmit);
    document.getElementById('transferForm')?.addEventListener('submit', handleTransferSubmit);
    document.getElementById('payBillForm')?.addEventListener('submit', handlePayBillSubmit);
    document.getElementById('updateProfileForm')?.addEventListener('submit', handleProfileUpdateSubmit);
    document.getElementById('changePasswordForm')?.addEventListener('submit', handleChangePasswordSubmit);
    document.getElementById('verifyRecipientBtn')?.addEventListener('click', handleVerifyRecipient);
    document.getElementById('transferAccountNumber')?.addEventListener('input', resetTransferForm);

    fetchUserDetails();
    fetchBalance();
    fetchTransactions(false);
}

// --- Theme & Modal UI Functions ---
function setupThemeToggle() {
    const toggleButton = document.getElementById('theme-toggle');
    const darkIcon = document.getElementById('theme-toggle-dark-icon');
    const lightIcon = document.getElementById('theme-toggle-light-icon');
    if (!toggleButton || !darkIcon || !lightIcon) return;

    if (localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        lightIcon.classList.remove('hidden'); document.documentElement.classList.add('dark');
    } else {
        darkIcon.classList.remove('hidden'); document.documentElement.classList.remove('dark');
    }

    toggleButton.addEventListener('click', () => {
        const isDark = document.documentElement.classList.toggle('dark');
        localStorage.theme = isDark ? 'dark' : 'light';
        darkIcon.classList.toggle('hidden', !isDark);
        lightIcon.classList.toggle('hidden', isDark);
        if((getPageName() === 'account.html' || getPageName() === 'dashboard.html') && window.myTransactionChart) {
             fetchTransactions(false);
        }
    });
}
function showModal(modalId) {
    document.getElementById(modalId)?.classList.remove('hidden');
    document.getElementById('modalBackdrop')?.classList.remove('hidden');
}
function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
        document.getElementById('modalBackdrop')?.classList.add('hidden');
        const form = modal.querySelector('form');
        if (form) form.reset();
        if (modalId === 'transferModal') resetTransferForm();
        modal.querySelectorAll('.p-4.text-sm.text-red-300').forEach(err => err.classList.add('hidden'));
    }
}
function showToast(message, isError = false) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toastId = `toast-${Date.now()}`;
    const icon = isError 
        ? `<i class="bi bi-exclamation-triangle-fill text-red-400"></i>` 
        : `<i class="bi bi-check-circle-fill text-green-400"></i>`;
    const title = isError ? "Error" : "Success";
    const borderColor = isError ? 'border-red-500' : 'border-green-500';
    const toastHTML = `<div id="${toastId}" class="w-full max-w-sm p-4 text-gray-200 bg-gray-800 rounded-lg shadow-lg border ${borderColor} transition-transform duration-300 translate-x-full dark:bg-gray-700 dark:border-gray-600 dark:text-white" role="alert"><div class="flex items-center"><div class="inline-flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-lg">${icon}</div><div class="ms-3 text-sm font-normal"><div class="text-sm font-semibold text-white dark:text-white">${title}</div><div>${message}</div></div><button type="button" class="ms-auto -mx-1.5 -my-1.5 bg-gray-800 text-gray-400 hover:text-white hover:bg-gray-700 dark:bg-gray-700 dark:text-gray-300 dark:hover:text-white dark:hover:bg-gray-600 rounded-lg p-1.5 inline-flex items-center justify-center h-8 w-8" data-toast-dismiss="${toastId}">&times;</button></div></div>`;
    container.insertAdjacentHTML('beforeend', toastHTML);
    const toastEl = document.getElementById(toastId);
    toastEl.querySelector(`[data-toast-dismiss="${toastId}"]`).addEventListener('click', () => {
        toastEl.classList.add('opacity-0', 'scale-90');
        setTimeout(() => toastEl.remove(), 300);
    });
    setTimeout(() => toastEl.classList.remove('translate-x-full'), 10);
    setTimeout(() => {
        toastEl.classList.add('opacity-0', 'scale-90');
        setTimeout(() => toastEl.remove(), 300);
    }, 5000);
}
function showModalError(errorDiv, message) {
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.classList.remove('hidden');
    }
}
function hideModalError(errorDiv) {
    if (errorDiv) {
        errorDiv.textContent = '';
        errorDiv.classList.add('hidden');
    }
}
function toggleSpinner(button, show) {
    if (!button) return;
    button.disabled = show;
    if (show) {
        button.classList.add('opacity-70', 'cursor-not-allowed');
    } else {
        button.classList.remove('opacity-70', 'cursor-not-allowed');
    }
}

// --- API Call Functions ---

async function fetchSecure(url, options = {}) {
    const token = localStorage.getItem('authToken');
    if (!token) {
        showToast("Authentication error. Please log in again.", true);
        window.location.href = 'login.html';
        throw new Error("No auth token found");
    }
    const headers = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`, ...options.headers };
    return fetch(url, { ...options, headers });
}
async function fetchUserDetails() {
    const accountNumberDisplay = document.getElementById('userAccountNumber');
    if (!accountNumberDisplay) return;
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/me`);
        if (!response.ok) throw new Error(await response.text());
        const user = await response.json();
        localStorage.setItem('currentUser', JSON.stringify(user));
        const myAccount = user.accounts?.find(acc => acc.id == accountId);
        if (myAccount && myAccount.accountNumber.length === 10) {
            accountNumberDisplay.textContent = myAccount.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
        } else if (myAccount) {
            accountNumberDisplay.textContent = myAccount.accountNumber;
        } else {
            accountNumberDisplay.textContent = "N/A";
        }
    } catch (error) {
        console.error('Error fetching user details:', error.message);
        accountNumberDisplay.textContent = "Error";
    }
}
function populateProfileForm() {
    const userData = JSON.parse(localStorage.getItem('currentUser'));
    if (userData) {
        document.getElementById('profileFullName').value = userData.fullName || '';
        document.getElementById('profileEmail').value = userData.email || '';
    }
}
async function fetchBalance() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    const balanceDisplay = document.getElementById('accountBalanceDisplay');
    if (!balanceDisplay) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/balance`);
        if (!response.ok) throw new Error(await response.text());
        const balance = await response.json();
        balanceDisplay.textContent = `$ ${parseFloat(balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    } catch (error) {
        console.error('Error fetching balance:', error.message);
        balanceDisplay.textContent = "$ Error";
    }
}
async function fetchTransactions(tableOnly = false) {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    const tableBody = document.getElementById('transactionsTableBody');
    if (tableOnly && !tableBody) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transactions`);
        if (!response.ok) throw new Error(await response.text());
        const transactions = await response.json();
        if (tableBody) {
            tableBody.innerHTML = '';
            if (transactions.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-gray-500 dark:text-gray-400">No recent transactions found.</td></tr>';
            } else {
                transactions.forEach(tx => {
                    const isCredit = tx.amount >= 0;
                    const amountClass = isCredit ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400';
                    const amountSign = isCredit ? '+' : '-';
                    const formattedAmount = Math.abs(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
                    let typeBadgeClass = 'bg-gray-500 text-gray-100';
                    if (tx.type === 'DEPOSIT') typeBadgeClass = 'bg-green-100 text-green-800 dark:bg-green-700 dark:text-green-100';
                    else if (tx.type === 'TRANSFER' && isCredit) typeBadgeClass = 'bg-blue-100 text-blue-800 dark:bg-blue-700 dark:text-blue-100';
                    else if (tx.type === 'TRANSFER' && !isCredit) typeBadgeClass = 'bg-red-100 text-red-800 dark:bg-red-700 dark:text-red-100';
                    else if (tx.type === 'PAYMENT') typeBadgeClass = 'bg-yellow-100 text-yellow-800 dark:bg-yellow-700 dark:text-yellow-100';
                    const row = `<tr class="border-b border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700"><td class="px-6 py-4">${tx.timestamp}</td><td class="px-6 py-4">${tx.description}</td><td class="px-6 py-4"><span class="px-2 py-0.5 rounded-full text-xs font-medium ${typeBadgeClass}">${tx.type}</span></td><td class="px-6 py-4 font-medium ${amountClass}">${amountSign}$${formattedAmount}</td></tr>`;
                    tableBody.insertAdjacentHTML('beforeend', row);
                });
            }
        }
        if (!tableOnly) {
            renderTransactionChart(transactions);
        }
    } catch (error) {
        console.error('Error fetching transactions:', error.message);
        if (tableBody) tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-red-400">Error loading transactions.</td></tr>';
    }
}
function renderTransactionChart(transactions) {
    const ctx = document.getElementById('transactionChart');
    if (!ctx) return;
    let totalIncome = 0;
    let totalExpenses = 0;
    transactions.forEach(tx => {
        if (tx.amount >= 0) totalIncome += tx.amount;
        else totalExpenses += Math.abs(tx.amount);
    });
    if (totalIncome === 0 && totalExpenses === 0) { totalIncome = 1; }
    if (window.myTransactionChart) window.myTransactionChart.destroy();
    const isDark = document.documentElement.classList.contains('dark');
    const chartTextColor = isDark ? '#e5e7eb' : '#374151';
    const incomeColor = isDark ? '#22c55e' : '#16a34a';
    const expenseColor = isDark ? '#ef4444' : '#dc2626';
    window.myTransactionChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Income', 'Expenses'],
            datasets: [{
                data: [totalIncome, totalExpenses],
                backgroundColor: [incomeColor, expenseColor],
                borderColor: [isDark ? '#1f2937' : '#ffffff'],
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            cutout: '70%',
            plugins: {
                legend: { position: 'bottom', labels: { color: chartTextColor, font: { family: 'Inter' } } },
                tooltip: {
                    callbacks: {
                        label: (context) => (totalIncome === 1 && totalExpenses === 0) ? ' No transactions yet' : ` ${context.label}: ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(context.parsed)}`
                    }
                }
            }
        }
    });
}

// --- Portal Data Fetching ---
async function fetchUserAccounts() {
    const listEl = document.getElementById('userAccountList');
    if (!listEl) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/all`);
        if (!response.ok) throw new Error(await response.text());
        const accounts = await response.json();
        document.getElementById('accountsLoadingSpinner')?.classList.add('hidden');
        listEl.innerHTML = '';
        if (accounts.length === 0) {
            listEl.innerHTML = `<div class="col-span-full text-center text-gray-500 dark:text-gray-400">You haven't added any bank accounts yet.</div>`;
            return;
        }
        accounts.forEach(account => {
            const formattedBalance = `$ ${parseFloat(account.balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
            const formattedAccountNumber = account.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
            const cardHTML = `<a href="account.html?id=${account.id}&name=${encodeURIComponent(account.bank.name)}" class="block p-6 bg-white dark:bg-bank-secondary bg-opacity-70 dark:bg-opacity-70 backdrop-blur-lg shadow-2xl rounded-2xl border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all duration-200 transform hover:scale-[1.02]"><h5 class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">${account.bank.name}</h5><p class="font-normal text-gray-700 dark:text-gray-400">Acct: ${formattedAccountNumber}</p><p class="text-3xl font-bold text-gray-900 dark:text-white mt-2">${formattedBalance}</p></a>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });
    } catch (error) {
        console.error("Error fetching user accounts:", error);
        document.getElementById('accountsLoadingSpinner')?.classList.add('hidden');
        listEl.innerHTML = `<div class="col-span-full text-center text-red-500">Error loading accounts.</div>`;
    }
}
async function fetchAllBanks() {
    const listEl = document.getElementById('availableBankList');
    if (!listEl) return;
    try {
        const response = await fetchSecure(`${BANK_API_URL}/all`);
        if (!response.ok) throw new Error(await response.text());
        const banks = await response.json();
        listEl.innerHTML = '';
        banks.forEach(bank => {
            const cardHTML = `<div class="p-6 bg-white dark:bg-bank-secondary bg-opacity-70 dark:bg-opacity-70 backdrop-blur-lg shadow-2xl rounded-2xl border border-gray-200 dark:border-gray-700 flex flex-col justify-between"><h5 class="mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white">${bank.name}</h5><p class="font-normal text-gray-700 dark:text-gray-400">Open a new account with us and get a $50 bonus.</p><button onclick="handleAddBank(${bank.id}, '${bank.name}')" class="add-bank-btn mt-4 w-full text-white bg-blue-600 hover:bg-blue-700 font-medium rounded-lg text-sm px-5 py-2.5 text-center">Add Bank</button></div>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });
    } catch (error) {
        console.error("Error fetching all banks:", error);
    }
}
async function handleAddBank(bankId, bankName) {
    const btn = event.target;
    toggleSpinner(btn, true);
    try {
        const response = await fetchSecure(`${BANK_API_URL}/add/${bankId}`, { method: 'POST' });
        if (!response.ok) throw new Error(await response.text());
        showToast(`Successfully opened an account at ${bankName}!`);
        document.getElementById('userAccountList').innerHTML = '';
        const spinner = document.getElementById('accountsLoadingSpinner');
        if (spinner) spinner.classList.remove('hidden');
        fetchUserAccounts();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        toggleSpinner(btn, false);
    }
}

// --- Form Handlers ---
async function handleDepositSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
    const form = event.target;
    const amount = parseFloat(document.getElementById('depositAmount').value);
    const errorDiv = document.getElementById('depositError');
    const submitButton = document.getElementById('submitDeposit');
    hideModalError(errorDiv);
    if (isNaN(amount) || amount <= 0) {
        showModalError(errorDiv, "Please enter a valid, positive amount."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/deposit`, { method: 'POST', body: JSON.stringify({ amount }) });
        if (!response.ok) throw new Error(await response.text());
        refreshDashboardData();
        hideModal('depositModal');
        showToast("Deposit successful!");
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleVerifyRecipient() {
    const accountNumber = document.getElementById('transferAccountNumber').value.replace(/[-\s]/g, '');
    const errorDiv = document.getElementById('transferError');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    hideModalError(errorDiv);
    if (!accountNumber) {
        showModalError(errorDiv, "Please enter an account number."); return;
    }
    toggleSpinner(verifyBtn, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/verify-recipient?accountNumber=${accountNumber}`);
        const recipientName = await response.text();
        if (!response.ok) throw new Error(recipientName);
        document.getElementById('verifiedRecipientName').textContent = recipientName;
        document.getElementById('recipientVerifiedInfo').classList.remove('hidden');
        document.getElementById('transferDetails').disabled = false;
        document.getElementById('submitTransfer').disabled = false;
        verifyBtn.textContent = 'Verified ✓';
        verifyBtn.classList.remove('bg-gray-200', 'dark:bg-gray-600', 'hover:bg-gray-300', 'dark:hover:bg-gray-700');
        verifyBtn.classList.add('bg-green-600', 'cursor-default', 'text-white');
        verifyBtn.disabled = true;
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(verifyBtn, false);
    }
}
function resetTransferForm() {
    document.getElementById('transferDetails').disabled = true;
    document.getElementById('submitTransfer').disabled = true;
    document.getElementById('recipientVerifiedInfo').classList.add('hidden');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    verifyBtn.textContent = 'Verify';
    verifyBtn.classList.remove('bg-green-600', 'cursor-default', 'text-white');
    verifyBtn.classList.add('bg-gray-200', 'dark:bg-gray-600', 'hover:bg-gray-300', 'dark:hover:bg-gray-700');
    verifyBtn.disabled = false;
    hideModalError(document.getElementById('transferError'));
}
async function handleTransferSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
    const recipientAccountNumber = document.getElementById('transferAccountNumber').value.replace(/[-\s]/g, '');
    const amount = parseFloat(document.getElementById('transferAmount').value);
    const password = document.getElementById('transferPassword').value;
    const errorDiv = document.getElementById('transferError');
    const submitButton = document.getElementById('submitTransfer');
    hideModalError(errorDiv);
    if (!recipientAccountNumber || isNaN(amount) || amount <= 0 || !password) {
        showModalError(errorDiv, "Please complete all fields."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transfer`, { method: 'POST', body: JSON.stringify({ recipientAccountNumber, amount, password }) });
        if (!response.ok) throw new Error(await response.text());
        refreshDashboardData();
        hideModal('transferModal');
        showToast("Transfer successful!");
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handlePayBillSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
    const form = event.target;
    const billerName = document.getElementById('payBillBiller').value;
    const amount = parseFloat(document.getElementById('payBillAmount').value);
    const password = document.getElementById('payBillPassword').value;
    const errorDiv = document.getElementById('payBillError');
    const submitButton = document.getElementById('submitPayBill');
    hideModalError(errorDiv);
    if (!billerName || isNaN(amount) || amount <= 0 || !password) {
        showModalError(errorDiv, "Please complete all fields."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/paybill`, { method: 'POST', body: JSON.stringify({ billerName, amount, password }) });
        if (!response.ok) throw new Error(await response.text());
        refreshDashboardData();
        hideModal('payBillModal');
        showToast("Bill payment successful!");
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleProfileUpdateSubmit(event) {
    event.preventDefault();
    const fullName = document.getElementById('profileFullName').value;
    const email = document.getElementById('profileEmail').value;
    const errorDiv = document.getElementById('profileUpdateError');
    const submitButton = document.getElementById('submitProfileUpdate');
    hideModalError(errorDiv);
    if (!fullName || !email) {
        showModalError(errorDiv, "Full name and email are required."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/profile`, { method: 'PUT', body: JSON.stringify({ fullName, email }) });
        if (!response.ok) throw new Error(await response.text());
        const updatedUser = await response.json();
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        showToast("Profile updated successfully!");
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleChangePasswordSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const errorDiv = document.getElementById('passwordChangeError');
    const submitButton = document.getElementById('submitPasswordChange');
    hideModalError(errorDiv);
    if (!currentPassword || !newPassword) {
        showModalError(errorDiv, "Both password fields are required."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/change-password`, { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) });
        if (!response.ok) throw new Error(await response.text());
        form.reset();
        showToast("Password changed successfully!");
    } catch (error) {
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}

// --- Auth Functions ---
async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    hideModalError(errorMessage);
    try {
        const response = await fetch(`${AUTH_API_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) });
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('authToken', data.accessToken);
            localStorage.setItem('username', username);
            window.location.href = 'dashboard.html';
        } else {
            showModalError(errorMessage, await response.text() || 'Invalid username or password.');
        }
    } catch (error) {
        showModalError(errorMessage, 'Cannot connect to the server.');
    }
}
async function handleRegister(event) {
    event.preventDefault();
    const fullName = document.getElementById('fullName').value;
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    hideModalError(errorMessage);
    successMessage.classList.add('hidden');
    try {
        const response = await fetch(`${AUTH_API_URL}/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName, username, email, password }) });
        const responseText = await response.text();
        if (response.ok) {
            localStorage.setItem('tempUser', JSON.stringify({username, password}));
            window.location.href = 'create-pin.html';
        } else {
            showModalError(errorMessage, responseText || `Registration failed`);
        }
    } catch (error) {
        showModalError(errorMessage, 'Cannot connect to the server.');
    }
}
async function handlePinSetup(event) {
    event.preventDefault();
    const newPin = document.getElementById('newPin').value;
    const rePin = document.getElementById('rePin').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    hideModalError(errorMessage);
    successMessage.classList.add('hidden');
    if (newPin.length !== 4 || rePin.length !== 4) {
        showModalError(errorMessage, "PIN must be exactly 4 digits."); return;
    }
    if (newPin !== rePin) {
        showModalError(errorMessage, "PINs do not match."); return;
    }
    if (!password) {
        showModalError(errorMessage, "Please enter your password to verify."); return;
    }
    const tempUserData = JSON.parse(localStorage.getItem('tempUser'));
    if (!tempUserData || !tempUserData.username) {
        showModalError(errorMessage, "Registration session expired. Please register again."); return;
    }
    if (password !== tempUserData.password) {
         showModalError(errorMessage, "Incorrect password. Please enter the password you just used to register."); return;
    }
    const requestBody = { username: tempUserData.username, password: tempUserData.password, pin: newPin };
    try {
        const response = await fetch(`${ACCOUNT_API_URL}/set-pin`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(requestBody) });
        const responseText = await response.text();
        if (response.ok) {
            localStorage.removeItem('tempUser');
            successMessage.textContent = "PIN set successfully! Redirecting to login...";
            successMessage.classList.remove('hidden');
            setTimeout(() => { window.location.href = 'login.html'; }, 2000);
        } else {
            showModalError(errorMessage, responseText);
        }
    } catch (error) {
        showModalError(errorMessage, "Cannot connect to the server.");
    }
}
function checkAuth() {
    if (!localStorage.getItem('authToken')) {
        window.location.href = 'index.html';
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
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}
function refreshDashboardData() {
    fetchBalance();
    fetchTransactions(true); // Update table
    fetchTransactions(false); // Update chart
}

