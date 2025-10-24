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

/**
 * Formats a number into Indian Rupee (INR) currency.
 * @param {number} amount - The number to format.
 * @returns {string} - The formatted currency string (e.g., "₹1,00,000.00").
 */
function formatCurrency(amount) {
    const numAmount = Number(amount);
    if (isNaN(numAmount)) {
        console.warn("Invalid amount passed to formatCurrency:", amount);
        return '₹ --.--';
    }
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(numAmount);
}

// --- Main execution logic ---
document.addEventListener('DOMContentLoaded', () => {
    const page = getPageName();

    // Attach event listeners based on the current page
    if (page === 'login.html') {
        document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
    } else if (page === 'register.html') {
        document.getElementById('registerForm')?.addEventListener('submit', handleRegister);
    } else if (page === 'create-pin.html') {
         document.getElementById('pinSetupForm')?.addEventListener('submit', handlePinSetup);
    } else if (page === 'dashboard.html') {
        checkAuth(); // Redirect if not logged in
        if (localStorage.getItem('authToken')) { // Proceed if logged in
            setupPortalDashboard();
        }
    } else if (page === 'account.html') {
        checkAuth(); // Redirect if not logged in
        if (localStorage.getItem('authToken')) { // Proceed if logged in
            setupAccountDashboard();
        }
    }

    setupThemeToggle(); // Setup theme toggle on all pages
});

// --- Portal Dashboard Setup ---
function setupPortalDashboard() {
    document.getElementById('logoutButton')?.addEventListener('click', handleLogout);
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }
    fetchUserAccounts(); // Fetches accounts and updates stats
    fetchAllBanks();
}

// --- Account Dashboard Setup ---
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

    document.body.dataset.accountId = accountId; // Store accountId for later use
    document.getElementById('bankNameDisplay').textContent = bankName || "Account Details";
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }

    // Tab Navigation
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
            if (paneId === 'card-pane') loadCardDetails();
        });
    });

    // Modal Triggers & Closers
    document.getElementById('depositBtn')?.addEventListener('click', () => showModal('depositModal'));
    document.getElementById('transferBtn')?.addEventListener('click', () => showModal('transferModal'));
    document.getElementById('payBillBtn')?.addEventListener('click', () => showModal('payBillModal'));
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault(); e.stopPropagation();
            const modal = e.target.closest('.fixed.z-50');
            if (modal) hideModal(modal.id);
        });
    });

    // Form Event Listeners
    document.getElementById('depositForm')?.addEventListener('submit', handleDepositSubmit);
    document.getElementById('transferForm')?.addEventListener('submit', handleTransferSubmit);
    document.getElementById('payBillForm')?.addEventListener('submit', handlePayBillSubmit);
    document.getElementById('updateProfileForm')?.addEventListener('submit', handleProfileUpdateSubmit);
    document.getElementById('changePasswordForm')?.addEventListener('submit', handleChangePasswordSubmit);
    document.getElementById('setPinForm')?.addEventListener('submit', handleSetPinSubmit);
    document.getElementById('download-csv-btn')?.addEventListener('click', handleDownloadCsv);
    // Inside setupAccountDashboard()
document.getElementById('card-toggle-btn')?.addEventListener('click', handleCardToggle);

    // Transfer Form Specifics
    document.getElementById('verifyRecipientBtn')?.addEventListener('click', handleVerifyRecipient);
    
    // Removed the line that was causing the input to clear
    // document.getElementById('transferAccountNumber')?.addEventListener('input', resetTransferForm);

    // Initial Data Fetch
    fetchUserDetails(); // This will now call populateProfileForm()
    fetchBalance();
    fetchTransactions(false);
}

// --- Theme & Modal UI Functions ---
function setupThemeToggle() {
    console.log("setupThemeToggle function started");
    const toggleButton = document.getElementById('theme-toggle');
    const toggleIcon = document.getElementById('theme-toggle-icon');

    if (!toggleButton || !toggleIcon) {
        console.error("Theme toggle button or icon not found!");
        return;
    } else {
        console.log("Theme toggle elements found.");
    }

    let currentTheme = localStorage.getItem('theme') || 'system';
    console.log("Initial theme from localStorage:", currentTheme);

    function applyTheme(theme) {
        console.log("Applying theme:", theme);
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        const isDark = (theme === 'dark') || (theme === 'system' && systemPrefersDark);

        document.documentElement.classList.toggle('dark', isDark);
        console.log("<html> classList after toggle:", document.documentElement.classList.toString());

        let iconClass = 'bi bi-display-fill text-xl';
        if (theme === 'dark') {
            iconClass = 'bi bi-moon-stars-fill text-xl';
        } else if (theme === 'light') {
            iconClass = 'bi bi-sun-fill text-xl';
        }
        toggleIcon.className = iconClass;
        console.log("Set icon class to:", toggleIcon.className);

        currentTheme = theme;
        localStorage.setItem('theme', theme);

        if (window.myTransactionChart && typeof window.myTransactionChart.destroy === 'function') {
            const currentPage = getPageName();
            if (currentPage === 'account.html' || currentPage === 'dashboard.html') {
                 console.log("Destroying and re-rendering chart for theme change.");
                 window.myTransactionChart.destroy();
                 if(typeof fetchTransactions === 'function') {
                    fetchTransactions(false);
                 }
            }
        }
    }

    toggleButton.addEventListener('click', () => {
        console.log("Theme toggle button clicked!");
        let nextTheme;
        if (currentTheme === 'system') {
            nextTheme = 'light';
        } else if (currentTheme === 'light') {
            nextTheme = 'dark';
        } else {
            nextTheme = 'system';
        }
        applyTheme(nextTheme);
    });

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        console.log("OS theme changed, current theme selection is:", currentTheme);
        if (currentTheme === 'system') {
            applyTheme('system');
        }
    });

    applyTheme(currentTheme);
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
        modal.querySelectorAll('[id$="Error"]').forEach(hideModalError);
    }
}

function showToast(message, isError = false) {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toastId = `toast-${Date.now()}`;
    const iconClass = isError ? 'bi-exclamation-triangle-fill text-red-400' : 'bi-check-circle-fill text-green-400';
    const title = isError ? "Error" : "Success";
    const borderColor = isError ? 'border-red-500' : 'border-green-500';
    const toastHTML = `
        <div id="${toastId}" class="w-full max-w-sm p-4 text-gray-200 bg-gray-800 rounded-lg shadow-lg border ${borderColor} transition-transform duration-300 translate-x-full" role="alert">
            <div class="flex items-center">
                <div class="inline-flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-lg"><i class="bi ${iconClass}"></i></div>
                <div class="ms-3 text-sm font-normal">
                    <div class="text-sm font-semibold text-white">${title}</div>
                    <div>${message}</div>
                </div>
                <button type="button" class="ms-auto -mx-1.5 -my-1.5 bg-gray-800 text-gray-400 hover:text-white hover:bg-gray-700 rounded-lg p-1.5 inline-flex items-center justify-center h-8 w-8" data-toast-dismiss="${toastId}">
                    &times;
                </button>
            </div>
        </div>`;
    container.insertAdjacentHTML('beforeend', toastHTML);
    const toastEl = document.getElementById(toastId);
    toastEl.querySelector(`[data-toast-dismiss="${toastId}"]`).addEventListener('click', () => {
        toastEl.classList.add('opacity-0', 'scale-90');
        setTimeout(() => toastEl.remove(), 300);
    });
    requestAnimationFrame(() => toastEl.classList.remove('translate-x-full'));
    setTimeout(() => {
        if (document.getElementById(toastId)) {
            toastEl.classList.add('opacity-0', 'scale-90');
            setTimeout(() => toastEl.remove(), 300);
        }
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
    button.classList.toggle('opacity-70', show);
    button.classList.toggle('cursor-not-allowed', show);
}


// --- API Call Functions ---

async function fetchSecure(url, options = {}) {
    const token = localStorage.getItem('authToken');
    if (!token) {
        showToast("Authentication error. Please log in again.", true);
        window.location.href = 'login.html';
        throw new Error("No auth token found");
    }
    const headers = { 'Authorization': `Bearer ${token}`, ...options.headers };
    if (options.body && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }
    if (options.isCsv && headers['Content-Type']) {
        delete headers['Content-Type'];
    }
    try {
        const response = await fetch(url, { ...options, headers });
        
        if (response.status === 401 || response.status === 403) {
            const isPasswordCheck = url.includes('/set-pin') || url.includes('/change-password');
            if (isPasswordCheck) {
                return response; 
            } else {
                showToast("Session expired or unauthorized. Logging out.", true);
                handleLogout();
                throw new Error(`Unauthorized: ${response.status}`);
            }
        }
        return response;
    } catch (networkError) {
        console.error("Network error during fetchSecure:", networkError);
        showToast("Network error. Could not connect.", true);
        throw networkError;
    }
}

async function fetchUserDetails() {
    const accountNumberDisplay = document.getElementById('userAccountNumber');
    const pageAccountId = document.body.dataset.accountId;
    if (!accountNumberDisplay || !pageAccountId) return;

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/me`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const user = await response.json();
        localStorage.setItem('currentUser', JSON.stringify(user)); // Update user data

        const currentAccount = user.accounts?.find(acc => acc.id == pageAccountId);
        if (currentAccount?.accountNumber) {
            accountNumberDisplay.textContent = currentAccount.accountNumber.length === 10
                ? currentAccount.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3')
                : currentAccount.accountNumber;
        } else {
            accountNumberDisplay.textContent = "N/A";
            console.warn("Account number not found.");
        }
        
        // --- THIS IS THE FIX ---
        // Call populateProfileForm *after* fetching details
        if (getPageName() === 'account.html') {
             populateProfileForm();
        }
        // ----------------------

    } catch (error) {
        console.error('Error fetching user details:', error);
        accountNumberDisplay.textContent = "Error";
    }
}

// --- MODIFIED: populateProfileForm ---
// --- REPLACE your old function with this one ---
function populateProfileForm() {
    const userData = JSON.parse(localStorage.getItem('currentUser'));
    if (userData) {
        document.getElementById('profileFullName').value = userData.fullName || '';
        document.getElementById('profileEmail').value = userData.email || '';
        
        // Check if phone number exists and starts with +91.
        // If so, strip the +91 and show only the 10 digits.
        let phoneDisplay = '';
        if (userData.phoneNumber && userData.phoneNumber.startsWith('+91')) {
            phoneDisplay = userData.phoneNumber.substring(3); // Get only the 10 digits
        }
        // Set the 10-digit number to the input field
        document.getElementById('profilePhone').value = phoneDisplay;
        
        document.getElementById('profileDob').value = userData.dateOfBirth || '';
        document.getElementById('profileAddress').value = userData.address || '';
        document.getElementById('profileNominee').value = userData.nomineeName || '';
    }
}
async function fetchBalance() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    const balanceDisplay = document.getElementById('accountBalanceDisplay');
    if (!balanceDisplay) return;

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/balance`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const balance = await response.json();
        balanceDisplay.textContent = formatCurrency(balance);
    } catch (error) {
        console.error('Error fetching balance:', error);
        balanceDisplay.textContent = "₹ Error";
    }
}
async function fetchTransactions(tableOnly = false) {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    const tableBody = document.getElementById('transactionsTableBody');
    if (tableOnly && !tableBody) return;

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transactions`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const transactions = await response.json();

        if (tableBody) {
            tableBody.innerHTML = '';
            if (transactions.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-slate-400">No recent transactions found.</td></tr>';
            } else {
                transactions.forEach(tx => {
                    const isCredit = tx.amount >= 0;
                    const amountClass = isCredit ? 'text-green-400' : 'text-red-400';
                    const formattedAmount = formatCurrency(tx.amount);
                    let typeBadgeClass = 'bg-slate-700 text-slate-200';
                    if (tx.type === 'DEPOSIT') typeBadgeClass = 'bg-emerald-900/50 text-emerald-300 border border-emerald-500/30';
                    else if (tx.type === 'TRANSFER' && isCredit) typeBadgeClass = 'bg-blue-900/50 text-blue-300 border border-blue-500/30';
                    else if (tx.type === 'TRANSFER' && !isCredit) typeBadgeClass = 'bg-red-900/50 text-red-300 border border-red-500/30';
                    else if (tx.type === 'PAYMENT') typeBadgeClass = 'bg-amber-900/50 text-amber-300 border border-amber-500/30';
                    const row = `<tr class="border-b border-slate-700 hover:bg-slate-800/50"><td class="px-6 py-4 whitespace-nowrap">${new Date(tx.timestamp).toLocaleString()}</td><td class="px-6 py-4">${tx.description}</td><td class="px-6 py-4"><span class="px-2 py-0.5 rounded-full text-xs font-medium ${typeBadgeClass}">${tx.type}</span></td><td class="px-6 py-4 font-medium ${amountClass} whitespace-nowrap">${formattedAmount}</td></tr>`;
                    tableBody.insertAdjacentHTML('beforeend', row);
                });
            }
        }

        if (!tableOnly && (getPageName() === 'account.html')) {
            renderTransactionChart(transactions);
        }
    } catch (error) {
        console.error('Error fetching transactions:', error);
        if (tableBody) tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-red-400">Error loading transactions.</td></tr>';
    }
}

function renderTransactionChart(transactions) {
    const ctx = document.getElementById('transactionChart');
    if (!ctx) return;

    let totalIncome = 0;
    let totalExpenses = 0;
    transactions.forEach(tx => {
        const amount = Number(tx.amount);
        if (!isNaN(amount)) {
            if (amount >= 0) totalIncome += amount;
            else totalExpenses += Math.abs(amount);
        }
    });

    const hasData = totalIncome > 0 || totalExpenses > 0;
    const chartData = hasData ? [totalIncome, totalExpenses] : [1, 0];
    const chartLabels = ['Income', 'Expenses'];

    if (window.myTransactionChart && typeof window.myTransactionChart.destroy === 'function') {
        window.myTransactionChart.destroy();
    }

    const isDark = document.documentElement.classList.contains('dark');
    const chartTextColor = isDark ? '#e2e8f0' : '#111827';
    const incomeColor = isDark ? '#10b981' : '#16a34a';
    const expenseColor = isDark ? '#ef4444' : '#dc2626';
    const borderColor = isDark ? '#0f172a' : '#ffffff';

    window.myTransactionChart = new Chart(ctx, {
        type: 'doughnut',
        data: { labels: chartLabels, datasets: [{ data: chartData, backgroundColor: [incomeColor, expenseColor], borderColor: borderColor, borderWidth: 3 }] },
        options: {
            responsive: true, maintainAspectRatio: false, cutout: '70%',
            plugins: {
                legend: { position: 'bottom', labels: { color: chartTextColor, font: { family: 'Inter' } } },
                tooltip: {
                    backgroundColor: isDark ? 'rgba(15, 23, 42, 0.9)' : 'rgba(255, 255, 255, 0.9)',
                    titleColor: isDark ? '#cbd5e1' : '#374151',
                    bodyColor: isDark ? '#e2e8f0' : '#111827',
                    borderColor: isDark ? 'rgba(148, 163, 184, 0.2)' : 'rgba(209, 213, 219, 0.5)',
                    borderWidth: 1,
                    callbacks: { label: (context) => hasData ? ` ${context.label}: ${formatCurrency(context.parsed)}` : ' No transactions yet' }
                }
            }
        }
    });
}


// --- Portal Data Fetching ---
async function fetchUserAccounts() {
    const listEl = document.getElementById('userAccountList');
    const totalAccountsEl = document.getElementById('total-accounts');
    const totalBalanceEl = document.getElementById('total-balance');
    if (!listEl || !totalAccountsEl || !totalBalanceEl) return;

    totalAccountsEl.textContent = '...';
    totalBalanceEl.textContent = '...';
    const spinner = document.getElementById('accountsLoadingSpinner');
    if (spinner) spinner.classList.remove('hidden');

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/all`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const accounts = await response.json();

        if (spinner) spinner.classList.add('hidden');
        listEl.innerHTML = '';

        let calculatedTotalBalance = 0;
        const numberOfAccounts = accounts.length;

        if (numberOfAccounts === 0) {
            listEl.innerHTML = `<div class="col-span-full text-center text-bank-text-muted dark:text-slate-400 glass-card rounded-3xl p-12">You haven't added any bank accounts yet.</div>`;
            totalAccountsEl.textContent = '0';
            totalBalanceEl.textContent = formatCurrency(0);
            return;
        }

        accounts.forEach(account => {
            calculatedTotalBalance += parseFloat(account.balance || 0);
            const formattedBalance = formatCurrency(account.balance);
            const formattedAccountNumber = account.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
            const cardHTML = `
                <a href="account.html?id=${account.id}&name=${encodeURIComponent(account.bank.name)}"
                   class="bank-card rounded-3xl p-6 block transition-all duration-300 ease-in-out relative group animate-slide-up">
                    <div class="relative z-10">
                        <div class="flex justify-between items-center mb-4">
                            <h5 class="text-2xl font-bold tracking-tight text-bank-text-main dark:text-white group-hover:gradient-text transition-colors duration-300">${account.bank.name}</h5>
                            <i class="bi bi-arrow-right-circle text-bank-text-muted dark:text-slate-500 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 text-2xl transition-colors duration-300"></i>
                        </div>
                        <p class="font-normal text-bank-text-muted dark:text-slate-400 mb-1">Acct: ${formattedAccountNumber}</p>
                        <p class="text-4xl font-extrabold text-bank-text-main dark:text-white">${formattedBalance}</p>
                    </div>
                </a>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });

        totalAccountsEl.textContent = numberOfAccounts;
        totalBalanceEl.textContent = formatCurrency(calculatedTotalBalance);

    } catch (error) {
        console.error("Error fetching user accounts:", error);
        if (spinner) spinner.classList.add('hidden');
        listEl.innerHTML = `<div class="col-span-full text-center text-red-500 glass-card rounded-3xl p-12">Error loading accounts. Please try again later.</div>`;
        totalAccountsEl.textContent = 'Error';
        totalBalanceEl.textContent = 'Error';
    }
}
async function fetchAllBanks() {
    const listEl = document.getElementById('availableBankList');
    if (!listEl) return;
    try {
        const response = await fetchSecure(`${BANK_API_URL}/all`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        const banks = await response.json();
        listEl.innerHTML = '';

        if (banks.length === 0) {
             listEl.innerHTML = `<div class="col-span-full text-center text-bank-text-muted dark:text-slate-400 glass-card rounded-3xl p-12">No banks available to add right now.</div>`;
             return;
        }

        banks.forEach(bank => {
            const cardHTML = `
                <div class="bank-card rounded-3xl p-6 flex flex-col justify-between transition-all duration-300 ease-in-out">
                     <div class="relative z-10">
                        <h5 class="mb-2 text-2xl font-bold tracking-tight text-bank-text-main dark:text-white">${bank.name}</h5>
                        <p class="font-normal text-bank-text-muted dark:text-slate-400 mb-4">Link this bank to your portal.</p>
                     </div>
                     <button onclick="handleAddBank(event, ${bank.id}, '${bank.name}')"
                             class="add-bank-btn mt-4 w-full text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 font-medium rounded-xl text-sm px-5 py-3 text-center transform hover:scale-105 transition-transform duration-300 shadow-lg shadow-indigo-500/30 relative z-10">
                        Add Bank
                     </button>
                </div>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });
    } catch (error) {
        console.error("Error fetching all banks:", error);
        listEl.innerHTML = `<div class="col-span-full text-center text-red-500 glass-card rounded-3xl p-12">Error loading available banks.</div>`;
    }
}
async function handleAddBank(event, bankId, bankName) {
    const btn = event.target;
    if(!btn) return;
    toggleSpinner(btn, true);
    try {
        const response = await fetchSecure(`${BANK_API_URL}/add/${bankId}`, { method: 'POST' });
        if (!response.ok) {
            let errorText = `Failed to add account. Status: ${response.status}`;
            try { const backendError = await response.text(); if (backendError) errorText = backendError; } catch (e) {}
            throw new Error(errorText);
        }
        showToast(`Successfully opened an account at ${bankName}!`);
        await fetchUserAccounts();

    } catch (error) {
        showToast(error.message, true);
        console.error("Error adding bank:", error);
    } finally {
        toggleSpinner(btn, false);
    }
}

// --- Form Handlers ---
async function handleDepositSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId; if (!accountId) return;
    
    const amount = parseFloat(document.getElementById('depositAmount').value);
    const source = document.getElementById('depositSource').value; // <-- Get the source
    const errorDiv = document.getElementById('depositError');
    const submitButton = document.getElementById('submitDeposit');
    hideModalError(errorDiv);

    if (!source) { // <-- Check the source
        showModalError(errorDiv, "Please select a deposit source."); 
        return; 
    }
    if (isNaN(amount) || amount <= 0) { 
        showModalError(errorDiv, "Invalid amount."); 
        return; 
    }

    toggleSpinner(submitButton, true);
    try {
        // --- MODIFIED ---
        // Send 'amount' AND 'source' in the body
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/deposit`, { 
            method: 'POST', 
            body: JSON.stringify({ amount, source }) // <-- Send both
        });
        // -----------------
        if (!response.ok) throw new Error(await response.text() || 'Deposit failed.');
        
        await refreshDashboardData();
        hideModal('depositModal');
        showToast("Deposit successful!");

    } catch (error) { 
        console.error("Deposit error:", error); 
        showModalError(errorDiv, error.message); 
    }
    finally { toggleSpinner(submitButton, false); }
}
async function handleVerifyRecipient() {
    const accountNumber = document.getElementById('transferAccountNumber').value.replace(/[-\s]/g, '');
    const errorDiv = document.getElementById('transferError');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    hideModalError(errorDiv);
    if (!accountNumber) { showModalError(errorDiv, "Enter account number."); return; }
    toggleSpinner(verifyBtn, true); verifyBtn.textContent = 'Verifying...';
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/verify-recipient?accountNumber=${encodeURIComponent(accountNumber)}`);
        const recipientName = await response.text();
        if (!response.ok) throw new Error(recipientName || 'Verification failed.');
        document.getElementById('verifiedRecipientName').textContent = recipientName;
        document.getElementById('recipientVerifiedInfo').classList.remove('hidden');
        document.getElementById('transferDetails').disabled = false;
        document.getElementById('submitTransfer').disabled = false;
        verifyBtn.textContent = 'Verified ✓';
        verifyBtn.classList.remove('bg-slate-700', 'hover:bg-slate-600');
        verifyBtn.classList.add('bg-emerald-700', 'cursor-default');
        verifyBtn.disabled = true;
    } catch (error) { console.error("Verification error:", error); showModalError(errorDiv, error.message); resetTransferForm(); }
    finally { if (!verifyBtn.disabled) { toggleSpinner(verifyBtn, false); verifyBtn.textContent = 'Verify'; } }
}

function resetTransferForm() {
    document.getElementById('transferDetails').disabled = true;
    document.getElementById('submitTransfer').disabled = true;
    document.getElementById('recipientVerifiedInfo').classList.add('hidden');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    if(verifyBtn){
        verifyBtn.textContent = 'Verify';
        verifyBtn.classList.remove('bg-emerald-700', 'cursor-default');
        verifyBtn.classList.add('bg-slate-700', 'hover:bg-slate-600');
        verifyBtn.disabled = false;
        toggleSpinner(verifyBtn, false);
    }
    // DO NOT CLEAR THE INPUT VALUE
    // const accountInput = document.getElementById('transferAccountNumber'); if(accountInput) accountInput.value = '';
    hideModalError(document.getElementById('transferError'));
}

async function handleTransferSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId; if (!accountId) return;
    const recipientAccountNumber = document.getElementById('transferAccountNumber').value.replace(/[-\s]/g, '');
    const amount = parseFloat(document.getElementById('transferAmount').value);
    const pin = document.getElementById('transferPin').value;
    const errorDiv = document.getElementById('transferError');
    const submitButton = document.getElementById('submitTransfer');
    hideModalError(errorDiv);
    if (!recipientAccountNumber || isNaN(amount) || amount <= 0 || !pin) { showModalError(errorDiv, "Complete all fields."); return; }
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) { showModalError(errorDiv, "PIN must be 4 digits."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transfer`, { method: 'POST', body: JSON.stringify({ recipientAccountNumber, amount, pin }) });
        if (!response.ok) throw new Error(await response.text() || 'Transfer failed.');
        await refreshDashboardData();
        hideModal('transferModal');
        showToast("Transfer successful!");
    } catch (error) { console.error("Transfer error:", error); showModalError(errorDiv, error.message); }
    finally { toggleSpinner(submitButton, false); }
}
async function handlePayBillSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId; if (!accountId) return;
    const billerName = document.getElementById('payBillBiller').value;
    const amount = parseFloat(document.getElementById('payBillAmount').value);
    const pin = document.getElementById('payBillPin').value;
    const errorDiv = document.getElementById('payBillError');
    const submitButton = document.getElementById('submitPayBill');
    hideModalError(errorDiv);
    if (!billerName || isNaN(amount) || amount <= 0 || !pin) { showModalError(errorDiv, "Complete all fields."); return; }
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) { showModalError(errorDiv, "PIN must be 4 digits."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/paybill`, { method: 'POST', body: JSON.stringify({ billerName, amount, pin }) });
        if (!response.ok) throw new Error(await response.text() || 'Bill payment failed.');
        await refreshDashboardData();
        hideModal('payBillModal');
        showToast("Bill payment successful!");
    } catch (error) { console.error("Pay bill error:", error); showModalError(errorDiv, error.message); }
    finally { toggleSpinner(submitButton, false); }
}
async function fetchDashboardSummary() {
  try {
    const token = localStorage.getItem('token');
    const res = await fetch(`${ACCOUNT_API_URL}/dashboard`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) throw new Error('Dashboard fetch failed');
    const data = await res.json();
    document.getElementById('userName').textContent = data.userName;
  } catch (err) {
    console.error(err);
  }
}

// --- Fix #2: Card details stuck on loading ---
// --- Fix #2: Card details stuck on loading (REVISED) ---
// --- Fix #2: Card details stuck on loading (REVISED) ---
async function loadCardDetails() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) {
        console.error("No accountId found for loadCardDetails");
        return;
    }

    const loading = document.getElementById('card-status-loading');
    const controls = document.getElementById('card-controls');
    // Get the <p> tag inside the loading div
    const loadingTextElement = loading.querySelector('p'); 
    const defaultLoadingText = 'Loading card status...';

    // Reset UI to loading state
    loading.classList.remove('hidden');
    controls.classList.add('hidden');
    // Reset loading text
    if (loadingTextElement) loadingTextElement.textContent = defaultLoadingText;

    try {
        // Use fetchSecure and the correct URL that includes the accountId
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/card`);
        
        if (!response.ok) {
            const errorMsg = await response.text();
            throw new Error(errorMsg || 'Failed to fetch card details');
        }
        
        const card = await response.json();

        // --- Populate the Visual Debit Card ---
        document.getElementById('card-bank-name').textContent = document.getElementById('bankNameDisplay').textContent; // Re-use bank name
        document.getElementById('card-number').textContent = card.cardNumber.replace(/(\d{4})/g, '$1 ').trim(); // Format as XXXX XXXX ...
        document.getElementById('card-holder-name').textContent = card.cardHolderName;
        document.getElementById('card-expiry-date').textContent = card.expiryDate; // Assumes MM/YY format

        // --- Populate the Card Settings Box ---
        const statusText = document.getElementById('card-status-text');
        const toggleBtn = document.getElementById('card-toggle-btn');
        
        if (card.active) {
            statusText.textContent = 'ACTIVE';
            statusText.className = "text-sm font-bold text-green-500"; // Green text
            toggleBtn.textContent = 'Freeze Card';
            // Add classes for 'Freeze' button
            toggleBtn.className = 'px-6 py-3 font-semibold rounded-xl text-sm transform hover:scale-105 transition-all duration-300 text-white bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 shadow-lg shadow-orange-500/30';
        } else {
            statusText.textContent = 'FROZEN';
            statusText.className = "text-sm font-bold text-red-500"; // Red text
            toggleBtn.textContent = 'Unfreeze Card';
            // Add classes for 'Unfreeze' button
            toggleBtn.className = 'px-6 py-3 font-semibold rounded-xl text-sm transform hover:scale-105 transition-all duration-300 text-white bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 shadow-lg shadow-emerald-500/30';
        }

        // Show the card controls
        loading.classList.add('hidden');
        controls.classList.remove('hidden');

    } catch (err) {
        console.error("Error in loadCardDetails:", err);
        // Show the error message in the loading area
        if (loadingTextElement) {
            loadingTextElement.textContent = `Error: ${err.message}`;
            loadingTextElement.classList.add('text-red-400'); // Make error red
        }
    }
}
async function handleCardToggle() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;

    // Show the loading spinner and hide controls for instant feedback
    document.getElementById('card-status-loading').classList.remove('hidden');
    document.getElementById('card-controls').classList.add('hidden');

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/card/toggle`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(await response.text() || 'Failed to toggle card status.');
        }

        // Success! Now just reload the card details.
        // loadCardDetails will fetch the new state and show the controls again.
        await loadCardDetails(); 
        showToast("Card status updated!");

    } catch (error) {
        console.error('Error toggling card:', error);
        showToast(error.message, true); 
        // If the toggle fails, still reload the card details to show the original state
        await loadCardDetails();
    }
}
// --- Fix #1: Portal settings visible and populated ---
async function loadPortalSettings() {
  try {
    const token = localStorage.getItem('token');
    const res = await fetch(`${AUTH_API_URL}/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) throw new Error('Profile fetch failed');
    const user = await res.json();

    document.getElementById('profile-name').value = user.name || '';
    document.getElementById('profile-email').value = user.email || '';
  } catch (err) {
    console.error(err);
  }
}

// --- MODIFIED: handleProfileUpdateSubmit ---
// --- REPLACE your old function with this one ---
async function handleProfileUpdateSubmit(event) {
    event.preventDefault();
    
    // Read all new values
    const fullName = document.getElementById('profileFullName').value;
    const email = document.getElementById('profileEmail').value;
    const phoneDigits = document.getElementById('profilePhone').value; // This is *only* the 10 digits
    const dateOfBirth = document.getElementById('profileDob').value;
    const address = document.getElementById('profileAddress').value;
    const nomineeName = document.getElementById('profileNominee').value;

    const errorDiv = document.getElementById('profileUpdateError');
    const submitButton = document.getElementById('submitProfileUpdate');
    hideModalError(errorDiv);

    if (!fullName || !email) {
        showModalError(errorDiv, "Full name and email are required."); return;
    }
    if (!/\S+@\S+\.\S+/.test(email)) { // Basic email validation
         showModalError(errorDiv, "Please enter a valid email address."); return;
    }
    
    // Added phone validation
    let phoneToSend = null; // Default to null
    if (phoneDigits) { // If the field is not empty
        if (!/^\d{10}$/.test(phoneDigits)) { // Check if it's exactly 10 digits
            showModalError(errorDiv, "Phone number must be exactly 10 digits.");
            return;
        }
        // If it is 10 digits, prepend "+91" to send to backend
        phoneToSend = `+91${phoneDigits}`;
    }
    // If phoneDigits is empty, phoneToSend remains null
    // -------------------------------------

    toggleSpinner(submitButton, true);
    try {
        // Send the full object
        const response = await fetchSecure(`${ACCOUNT_API_URL}/profile`, {
            method: 'PUT',
            body: JSON.stringify({ 
                fullName, 
                email, 
                phoneNumber: phoneToSend, // Send the cleaned-up number
                dateOfBirth: dateOfBirth || null, // Send null if empty
                address, 
                nomineeName 
            })
        });

        if (!response.ok) throw new Error(await response.text() || 'Profile update failed.');
        
        const updatedUser = await response.json();
        localStorage.setItem('currentUser', JSON.stringify(updatedUser)); // Update stored user
        
        showToast("Profile updated!");
        
        // Repopulate form with potentially cleaned-up data
        populateProfileForm(); 

    } catch (error) { 
        console.error("Profile update error:", error); 
        showModalError(errorDiv, error.message); 
    }
    finally { toggleSpinner(submitButton, false); }
}

async function handleChangePasswordSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const errorDiv = document.getElementById('passwordChangeError');
    const submitButton = document.getElementById('submitPasswordChange');
    hideModalError(errorDiv);
    if (!currentPassword || !newPassword) { showModalError(errorDiv, "Both fields required."); return; }
    if (newPassword.length < 6) { showModalError(errorDiv, "New password min 6 chars."); return; }
    if (currentPassword === newPassword) { showModalError(errorDiv, "New pass can't be same as old."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/change-password`, { method: 'POST', body: JSON.stringify({ currentPassword, newPassword }) });
        if (!response.ok) {
            let errorText = await response.text() || 'Password change failed.';
            if (response.status === 400 || response.status === 401 || errorText.toLowerCase().includes("invalid")) {
                 errorText = "Incorrect current password.";
            }
            showModalError(errorDiv, errorText); throw new Error(errorText);
        }
        form.reset(); showToast("Password changed!");
    } catch (error) { console.error("Change password error:", error.message); }
    finally { toggleSpinner(submitButton, false); }
}
async function handleSetPinSubmit(event) { // For updating PIN on account.html
    event.preventDefault();
    const form = event.target;
    const currentPassword = document.getElementById('pinCurrentPassword').value;
    const newPin = document.getElementById('newPin').value;
    const errorDiv = document.getElementById('setPinError');
    const submitButton = document.getElementById('submitSetPin');
    hideModalError(errorDiv);
    if (!currentPassword || !newPin) { showModalError(errorDiv, "Fill both fields."); return; }
    if (newPin.length !== 4 || !/^\d{4}$/.test(newPin)) { showModalError(errorDiv, "PIN must be 4 digits."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/set-pin`, { method: 'POST', body: JSON.stringify({ password: currentPassword, pin: newPin }) });
        if (!response.ok) {
             let errorText = await response.text() || 'Failed to update PIN.';
            if (response.status === 401 || errorText.toLowerCase().includes("invalid")) {
                 errorText = "Invalid current password.";
            }
             showModalError(errorDiv, errorText); throw new Error(errorText);
        }
        form.reset(); showToast("Security PIN updated!");
    } catch (error) { console.error("Error setting/updating PIN:", error.message); }
    finally { toggleSpinner(submitButton, false); }
}

async function handleDownloadCsv() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) { showToast("Could not find account ID.", true); return; }
    const btn = document.getElementById('download-csv-btn');
    toggleSpinner(btn, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/export/csv`, { isCsv: true });
        if (!response.ok) {
            let errorMsg = 'Could not download statement.';
            try { const errText = await response.text(); if (errText) errorMsg += ` Server: ${errText}`; } catch (_) {}
            throw new Error(errorMsg);
        }
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none'; a.href = url; a.download = `statement-${accountId}.csv`;
        document.body.appendChild(a); a.click();
        window.URL.revokeObjectURL(url); a.remove();
    } catch (error) { console.error('Error downloading statement:', error); showToast(error.message, true); }
    finally { toggleSpinner(btn, false); }
}

// --- Auth Functions ---
async function handleLogin(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    const submitButton = event.target.querySelector('button[type="submit"]');
    hideModalError(errorMessage);
    if (!username || !password) { showModalError(errorMessage, "Enter username and password."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetch(`${AUTH_API_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) });
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('authToken', data.accessToken);
            localStorage.setItem('username', username);
            window.location.href = 'dashboard.html';
        } else { showModalError(errorMessage, await response.text() || 'Invalid credentials.'); }
    } catch (error) { console.error("Login error:", error); showModalError(errorMessage, 'Login failed. Cannot connect.'); }
    finally { toggleSpinner(submitButton, false); }
}
async function handleRegister(event) {
    event.preventDefault();
    const fullName = document.getElementById('fullName').value;
    const username = document.getElementById('username').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    const submitButton = event.target.querySelector('button[type="submit"]');
    hideModalError(errorMessage); successMessage.classList.add('hidden');
    if (!fullName || !username || !email || !password) { showModalError(errorMessage, "Fill all fields."); return; }
    if (password.length < 6) { showModalError(errorMessage, "Password min 6 chars."); return; }
    if (!/\S+@\S+\.\S+/.test(email)) { showModalError(errorMessage, "Invalid email."); return; }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetch(`${AUTH_API_URL}/register`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fullName, username, email, password }) });
        const responseText = await response.text();
        if (response.ok) {
            localStorage.setItem('tempUser', JSON.stringify({ username, password })); // Save for PIN setup
            window.location.href = 'create-pin.html'; // Redirect
        } else { showModalError(errorMessage, responseText || `Registration failed.`); }
    } catch (error) { console.error("Registration error:", error); showModalError(errorMessage, 'Registration failed. Cannot connect.'); }
    finally { toggleSpinner(submitButton, false); }
}

async function handlePinSetup(event) { // For create-pin.html
    event.preventDefault();
    const password = document.getElementById('password').value;
    const newPin = document.getElementById('newPin').value;
    const confirmNewPin = document.getElementById('confirmNewPin').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    const submitButton = document.getElementById('submitPinSetup');
    hideModalError(errorMessage); successMessage.classList.add('hidden');
    if (!password || !newPin || !confirmNewPin) { showModalError(errorMessage, "Fill all fields."); return; }
    if (newPin.length !== 4 || !/^\d{4}$/.test(newPin)) { showModalError(errorMessage, "PIN must be 4 digits."); return; }
    if (newPin !== confirmNewPin) { showModalError(errorMessage, "PINs do not match."); return; }
    const tempUserData = JSON.parse(localStorage.getItem('tempUser'));
    if (!tempUserData?.username || !tempUserData?.password) {
        showModalError(errorMessage, "Session expired. Register again.");
        localStorage.removeItem('tempUser'); return;
    }
    if (password !== tempUserData.password) { showModalError(errorMessage, "Incorrect registration password."); return; }
    toggleSpinner(submitButton, true);
    let tempToken = null;
    try {
        const loginResponse = await fetch(`${AUTH_API_URL}/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(tempUserData) });
        if (!loginResponse.ok) throw new Error("Auth failed before PIN set.");
        const loginData = await loginResponse.json();
        tempToken = loginData.accessToken;
        const setPinResponse = await fetch(`${ACCOUNT_API_URL}/set-pin`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tempToken}` }, body: JSON.stringify({ password: tempUserData.password, pin: newPin }) });
        if (!setPinResponse.ok) throw new Error(await setPinResponse.text() || 'Failed to set PIN.');
        localStorage.removeItem('tempUser');
        successMessage.textContent = "PIN set! Redirecting to login...";
        successMessage.classList.remove('hidden');
        setTimeout(() => { window.location.href = 'login.html'; }, 2000);
    } catch (error) {
        console.error("PIN Setup error:", error);
        showModalError(errorMessage, error.message);
        localStorage.removeItem('tempUser');
    } finally {
        toggleSpinner(submitButton, false);
    }
}

function checkAuth() {
    const token = localStorage.getItem('authToken');
    const currentPage = getPageName();
    const protectedPages = ['dashboard.html', 'account.html'];
    const authPages = ['login.html', 'register.html', 'create-pin.html', 'index.html'];

    if (token) {
        const username = localStorage.getItem('username');
        const usernameDisplay = document.getElementById('username-display');
        if (usernameDisplay && username) usernameDisplay.textContent = username;
        if (authPages.includes(currentPage)) {
            window.location.href = 'dashboard.html';
        }
    } else {
        if (protectedPages.includes(currentPage)) {
            window.location.href = 'index.html';
        }
    }
}

function handleLogout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('tempUser');
    window.location.href = 'index.html';
}

async function refreshDashboardData() {
    await fetchBalance();
    await fetchTransactions(false); // Fetch for table AND chart
}