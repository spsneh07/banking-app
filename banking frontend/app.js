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

// --- NEW: Currency Formatting Helper ---
/**
 * Formats a number into Indian Rupee (INR) currency.
 * @param {number} amount - The number to format.
 * @returns {string} - The formatted currency string (e.g., "₹1,00,000.00").
 */
function formatCurrency(amount) {
    // Use 'en-IN' for Rupees and correct formatting (e.g., 1,00,000.00)
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount);
}

// --- Main execution logic ---
document.addEventListener('DOMContentLoaded', () => {
    const page = getPageName();

    if (page === 'login.html') {
        document.getElementById('loginForm')?.addEventListener('submit', handleLogin);
    } else if (page === 'register.html') {
        document.getElementById('registerForm')?.addEventListener('submit', handleRegister);
    } else if (page === 'create-pin.html') { // For setting PIN after registration
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
    // index.html (the landing page) only needs the theme toggle

    // Setup theme toggle on all pages that have the button
    setupThemeToggle();
});

// --- Portal Dashboard Setup ---
function setupPortalDashboard() {
    document.getElementById('logoutButton')?.addEventListener('click', handleLogout);
    const username = localStorage.getItem('username');
    if (username) {
        document.getElementById('username-display').textContent = username;
    }
    fetchUserAccounts(); // This function now updates stats too
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

            // Fetch data or populate forms when tabs are clicked
            if (paneId === 'profile-pane') populateProfileForm();
            if (paneId === 'transactions-pane') fetchTransactions(true); // Fetch only for table
            if (paneId === 'overview-pane') fetchTransactions(false); // Fetch for chart too
        });
    });

    // Modal Triggers
    document.getElementById('depositBtn')?.addEventListener('click', () => showModal('depositModal'));
    document.getElementById('transferBtn')?.addEventListener('click', () => showModal('transferModal'));
    document.getElementById('payBillBtn')?.addEventListener('click', () => showModal('payBillModal'));

    // Modal Close Buttons
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault(); e.stopPropagation();
            const modal = e.target.closest('.fixed.z-50'); // Find parent modal
            if (modal) hideModal(modal.id);
        });
    });

    // Form Event Listeners
    document.getElementById('depositForm')?.addEventListener('submit', handleDepositSubmit);
    document.getElementById('transferForm')?.addEventListener('submit', handleTransferSubmit);
    document.getElementById('payBillForm')?.addEventListener('submit', handlePayBillSubmit);
    document.getElementById('updateProfileForm')?.addEventListener('submit', handleProfileUpdateSubmit);
    document.getElementById('changePasswordForm')?.addEventListener('submit', handleChangePasswordSubmit);
    document.getElementById('setPinForm')?.addEventListener('submit', handleSetPinSubmit); // Listener for updating PIN
    document.getElementById('download-csv-btn')?.addEventListener('click', handleDownloadCsv);

    // Transfer Form Specific Listeners
    document.getElementById('verifyRecipientBtn')?.addEventListener('click', handleVerifyRecipient);
    document.getElementById('transferAccountNumber')?.addEventListener('input', resetTransferForm); // Reset if account number changes

    // Initial Data Fetch
    fetchUserDetails(); // Gets account number for header
    fetchBalance();
    fetchTransactions(false); // Fetch initial data for overview chart and table
}

// --- Theme & Modal UI Functions ---
function setupThemeToggle() {
    const toggleButton = document.getElementById('theme-toggle');
    const toggleIcon = document.getElementById('theme-toggle-icon');
    // Exit if elements aren't on the current page
    if (!toggleButton || !toggleIcon) return;

    let currentTheme = localStorage.getItem('theme') || 'system';

    function applyTheme(theme) {
        const isDark = (theme === 'dark') || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

        // 1. Set class on <html>
        document.documentElement.classList.toggle('dark', isDark);

        // 2. Set the icon
        if (theme === 'dark') {
            toggleIcon.className = 'bi bi-moon-stars-fill text-xl'; // Adjusted size
        } else if (theme === 'light') {
            toggleIcon.className = 'bi bi-sun-fill text-xl'; // Adjusted size
        } else {
            toggleIcon.className = 'bi bi-display-fill text-xl'; // System icon, adjusted size
        }

        currentTheme = theme;
        localStorage.setItem('theme', theme);

        // 3. Re-render chart if needed (check if chart exists)
        if (window.myTransactionChart && typeof window.myTransactionChart.destroy === 'function') {
             // Only re-render if on a page with the chart
            if (getPageName() === 'account.html' || getPageName() === 'dashboard.html') {
                 window.myTransactionChart.destroy(); // Destroy existing chart
                 fetchTransactions(false); // Re-fetch data and render new chart
            }
        }
    }

    // Button click cycles: system -> light -> dark -> system ... (Adjust cycle if preferred)
    toggleButton.addEventListener('click', () => {
        if (currentTheme === 'system') {
            applyTheme('light');
        } else if (currentTheme === 'light') {
            applyTheme('dark');
        } else { // currentTheme is 'dark'
            applyTheme('system');
        }
    });

    // Listen for OS theme changes (if user is in 'system' mode)
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        if (currentTheme === 'system') {
            applyTheme('system'); // Re-apply system theme
        }
    });

    // Set initial icon and theme on page load
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
        // Reset specific forms if needed
        if (modalId === 'transferModal') resetTransferForm();
        // Hide any error messages within the modal
        modal.querySelectorAll('[id$="Error"]').forEach(errDiv => { // Selects divs ending with "Error"
            if(errDiv) hideModalError(errDiv);
        });
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
    // Using explicit dark mode colors for toast (consistent with dark theme)
    const toastHTML = `<div id="${toastId}" class="w-full max-w-sm p-4 text-gray-200 bg-gray-800 rounded-lg shadow-lg border ${borderColor} transition-transform duration-300 translate-x-full" role="alert"><div class="flex items-center"><div class="inline-flex items-center justify-center flex-shrink-0 w-8 h-8 rounded-lg">${icon}</div><div class="ms-3 text-sm font-normal"><div class="text-sm font-semibold text-white">${title}</div><div>${message}</div></div><button type="button" class="ms-auto -mx-1.5 -my-1.5 bg-gray-800 text-gray-400 hover:text-white hover:bg-gray-700 rounded-lg p-1.5 inline-flex items-center justify-center h-8 w-8" data-toast-dismiss="${toastId}">&times;</button></div></div>`;
    container.insertAdjacentHTML('beforeend', toastHTML);
    const toastEl = document.getElementById(toastId);
    toastEl.querySelector(`[data-toast-dismiss="${toastId}"]`).addEventListener('click', () => {
        toastEl.classList.add('opacity-0', 'scale-90');
        setTimeout(() => toastEl.remove(), 300);
    });
    // Animate in
    requestAnimationFrame(() => {
        toastEl.classList.remove('translate-x-full');
    });
    // Auto dismiss
    setTimeout(() => {
        if(document.getElementById(toastId)){ // Check if not already dismissed
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
    if (show) {
        button.classList.add('opacity-70', 'cursor-not-allowed');
        // Optional: Add spinner icon or change text
    } else {
        button.classList.remove('opacity-70', 'cursor-not-allowed');
        // Optional: Remove spinner icon or restore text
    }
}

// --- API Call Functions ---

async function fetchSecure(url, options = {}) {
    const token = localStorage.getItem('authToken');
    if (!token) {
        showToast("Authentication error. Please log in again.", true);
        window.location.href = 'login.html'; // Redirect immediately
        throw new Error("No auth token found");
    }
    // Default headers, Authorization added here
    const headers = { 'Authorization': `Bearer ${token}`, ...options.headers };

    // Set Content-Type only if there's a body and it's not explicitly set otherwise
    if (options.body && !headers['Content-Type']) {
         headers['Content-Type'] = 'application/json';
    }

    // For CSV download, ensure Content-Type is removed if accidentally set
    if (options.isCsv && headers['Content-Type']) {
         delete headers['Content-Type'];
    }

    return fetch(url, { ...options, headers });
}
async function fetchUserDetails() {
    const accountNumberDisplay = document.getElementById('userAccountNumber');
    if (!accountNumberDisplay) return;
    const accountId = document.body.dataset.accountId;
    if (!accountId) return; // Only run on account page
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/me`); // Fetch details for the logged-in user
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const user = await response.json();
        localStorage.setItem('currentUser', JSON.stringify(user)); // Store user details

        // Find the specific account details from the user's list of accounts
        const currentAccount = user.accounts?.find(acc => acc.id == accountId);
        if (currentAccount && currentAccount.accountNumber) {
            // Format account number if 10 digits
            accountNumberDisplay.textContent = currentAccount.accountNumber.length === 10
                ? currentAccount.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3')
                : currentAccount.accountNumber;
        } else {
            accountNumberDisplay.textContent = "N/A";
            console.warn("Current account details not found in user data.");
        }
    } catch (error) {
        console.error('Error fetching user details:', error);
        accountNumberDisplay.textContent = "Error";
        // Potentially handle auth errors (e.g., token expired) more gracefully
        if (error.message.includes('401') || error.message.includes('403')) {
            handleLogout(); // Log out if unauthorized
        }
    }
}
function populateProfileForm() {
    const userData = JSON.parse(localStorage.getItem('currentUser'));
    if (userData) {
        // Ensure elements exist before setting value
        const fullNameInput = document.getElementById('profileFullName');
        const emailInput = document.getElementById('profileEmail');
        if (fullNameInput) fullNameInput.value = userData.fullName || '';
        if (emailInput) emailInput.value = userData.email || '';
    }
}
async function fetchBalance() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return; // Only run on account page
    const balanceDisplay = document.getElementById('accountBalanceDisplay');
    if (!balanceDisplay) return;
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/balance`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const balance = await response.json();
        balanceDisplay.textContent = formatCurrency(balance);
    } catch (error) {
        console.error('Error fetching balance:', error);
        balanceDisplay.textContent = "₹ Error";
         if (error.message.includes('401') || error.message.includes('403')) handleLogout();
    }
}
async function fetchTransactions(tableOnly = false) {
    const accountId = document.body.dataset.accountId;
    if (!accountId) return; // Only run on account page
    const tableBody = document.getElementById('transactionsTableBody');
    // Exit if only updating table and table doesn't exist
    if (tableOnly && !tableBody) return;

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transactions`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const transactions = await response.json();

        // Update Table if element exists
        if (tableBody) {
            tableBody.innerHTML = ''; // Clear previous entries
            if (transactions.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-slate-400">No recent transactions found.</td></tr>'; // Match dark theme
            } else {
                transactions.forEach(tx => {
                    const isCredit = tx.amount >= 0;
                    const amountClass = isCredit ? 'text-green-400' : 'text-red-400'; // Dark theme colors
                    const formattedAmount = formatCurrency(tx.amount);
                    let typeBadgeClass = 'bg-slate-700 text-slate-200'; // Default dark badge
                    // Specific badge colors (adjust dark mode classes if needed)
                    if (tx.type === 'DEPOSIT') typeBadgeClass = 'bg-emerald-900/50 text-emerald-300 border border-emerald-500/30';
                    else if (tx.type === 'TRANSFER' && isCredit) typeBadgeClass = 'bg-blue-900/50 text-blue-300 border border-blue-500/30';
                    else if (tx.type === 'TRANSFER' && !isCredit) typeBadgeClass = 'bg-red-900/50 text-red-300 border border-red-500/30';
                    else if (tx.type === 'PAYMENT') typeBadgeClass = 'bg-amber-900/50 text-amber-300 border border-amber-500/30';

                    // Use dark theme border color
                    const row = `<tr class="border-b border-slate-700 hover:bg-slate-800/50"><td class="px-6 py-4">${new Date(tx.timestamp).toLocaleString()}</td><td class="px-6 py-4">${tx.description}</td><td class="px-6 py-4"><span class="px-2 py-0.5 rounded-full text-xs font-medium ${typeBadgeClass}">${tx.type}</span></td><td class="px-6 py-4 font-medium ${amountClass}">${formattedAmount}</td></tr>`;
                    tableBody.insertAdjacentHTML('beforeend', row);
                });
            }
        }

        // Render chart if NOT tableOnly and on the correct page
        if (!tableOnly && (getPageName() === 'account.html' || getPageName() === 'dashboard.html')) {
            renderTransactionChart(transactions);
        }
    } catch (error) {
        console.error('Error fetching transactions:', error);
        if (tableBody) tableBody.innerHTML = '<tr><td colspan="4" class="py-4 text-center text-red-400">Error loading transactions.</td></tr>';
        if (error.message.includes('401') || error.message.includes('403')) handleLogout();
    }
}

function renderTransactionChart(transactions) {
    const ctx = document.getElementById('transactionChart');
    if (!ctx) return; // Exit if canvas not found

    let totalIncome = 0;
    let totalExpenses = 0;
    transactions.forEach(tx => {
        if (tx.amount >= 0) totalIncome += tx.amount;
        else totalExpenses += Math.abs(tx.amount);
    });

    const hasData = totalIncome > 0 || totalExpenses > 0;
    const chartData = hasData ? [totalIncome, totalExpenses] : [1, 0]; // Dummy data if none
    const chartLabels = ['Income', 'Expenses'];

    // Destroy previous chart instance if it exists
    if (window.myTransactionChart && typeof window.myTransactionChart.destroy === 'function') {
        window.myTransactionChart.destroy();
    }

    const isDark = document.documentElement.classList.contains('dark'); // Check current theme
    // Consistent dark theme colors
    const chartTextColor = '#e2e8f0'; // slate-200
    const incomeColor = '#10b981';    // bank-success
    const expenseColor = '#ef4444';   // bank-danger
    const borderColor = '#0f172a';    // bank-dark-bg (or card bg: #1f2937)

    window.myTransactionChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: chartLabels,
            datasets: [{
                data: chartData,
                backgroundColor: [incomeColor, expenseColor],
                borderColor: borderColor, // Use dark background color for border
                borderWidth: 3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false, // Allow resizing height
            cutout: '70%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: chartTextColor, font: { family: 'Inter' } }
                },
                tooltip: {
                    backgroundColor: 'rgba(15, 23, 42, 0.9)', // Dark tooltip
                    titleColor: '#cbd5e1', // slate-300
                    bodyColor: '#e2e8f0', // slate-200
                    borderColor: 'rgba(148, 163, 184, 0.2)',
                    borderWidth: 1,
                    callbacks: {
                        label: (context) => hasData ? ` ${context.label}: ${formatCurrency(context.parsed)}` : ' No transactions yet'
                    }
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
    if (!listEl || !totalAccountsEl || !totalBalanceEl) return; // Only run on dashboard

    totalAccountsEl.textContent = '...';
    totalBalanceEl.textContent = '...';

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/all`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const accounts = await response.json();

        document.getElementById('accountsLoadingSpinner')?.classList.add('hidden');
        listEl.innerHTML = ''; // Clear

        let calculatedTotalBalance = 0;
        const numberOfAccounts = accounts.length;

        if (numberOfAccounts === 0) {
            listEl.innerHTML = `<div class="col-span-full text-center text-slate-400 glass-card rounded-3xl p-12">You haven't added any bank accounts yet.</div>`;
            totalAccountsEl.textContent = '0';
            totalBalanceEl.textContent = formatCurrency(0);
            return;
        }

        accounts.forEach(account => {
            calculatedTotalBalance += parseFloat(account.balance || 0);
            const formattedBalance = formatCurrency(account.balance);
            const formattedAccountNumber = account.accountNumber.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');

            // --- Using Bank Card styling consistently ---
            const cardHTML = `
                <a href="account.html?id=${account.id}&name=${encodeURIComponent(account.bank.name)}"
                   class="bank-card rounded-3xl p-6 block transition-all duration-300 ease-in-out relative group animate-slide-up">
                    <div class="relative z-10">
                        <div class="flex justify-between items-center mb-4">
                            <h5 class="text-2xl font-bold tracking-tight text-white group-hover:gradient-text transition-colors duration-300">${account.bank.name}</h5>
                            <i class="bi bi-arrow-right-circle text-slate-500 group-hover:text-indigo-400 text-2xl transition-colors duration-300"></i>
                        </div>
                        <p class="font-normal text-slate-400 mb-1">Acct: ${formattedAccountNumber}</p>
                        <p class="text-4xl font-extrabold text-white">${formattedBalance}</p>
                    </div>
                </a>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });

        totalAccountsEl.textContent = numberOfAccounts;
        totalBalanceEl.textContent = formatCurrency(calculatedTotalBalance);

    } catch (error) {
        console.error("Error fetching user accounts:", error);
        document.getElementById('accountsLoadingSpinner')?.classList.add('hidden');
        listEl.innerHTML = `<div class="col-span-full text-center text-red-500 glass-card rounded-3xl p-12">Error loading accounts. Please try again later.</div>`;
        totalAccountsEl.textContent = 'Error';
        totalBalanceEl.textContent = 'Error';
        if (error.message.includes('401') || error.message.includes('403')) handleLogout();
    }
}

async function fetchAllBanks() {
    const listEl = document.getElementById('availableBankList');
    if (!listEl) return; // Only run on dashboard

    try {
        const response = await fetchSecure(`${BANK_API_URL}/all`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const banks = await response.json();
        listEl.innerHTML = ''; // Clear

        if (banks.length === 0) {
             listEl.innerHTML = `<div class="col-span-full text-center text-slate-400 glass-card rounded-3xl p-12">No banks available to add right now.</div>`;
             return;
        }

        banks.forEach(bank => {
            // --- Using Bank Card styling consistently ---
            const cardHTML = `
                <div class="bank-card rounded-3xl p-6 flex flex-col justify-between transition-all duration-300 ease-in-out">
                     <div class="relative z-10">
                        <h5 class="mb-2 text-2xl font-bold tracking-tight text-white">${bank.name}</h5>
                        <p class="font-normal text-slate-400 mb-4">Open a new account with us and get a ₹50 bonus.</p>
                     </div>
                     <button onclick="handleAddBank(${bank.id}, '${bank.name}')"
                             class="add-bank-btn mt-4 w-full text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 font-medium rounded-xl text-sm px-5 py-3 text-center transform hover:scale-105 transition-transform duration-300 shadow-lg shadow-indigo-500/30 relative z-10">
                        Add Bank
                     </button>
                </div>`;
            listEl.insertAdjacentHTML('beforeend', cardHTML);
        });
    } catch (error) {
        console.error("Error fetching all banks:", error);
        listEl.innerHTML = `<div class="col-span-full text-center text-red-500 glass-card rounded-3xl p-12">Error loading available banks.</div>`;
        if (error.message.includes('401') || error.message.includes('403')) handleLogout();
    }
}

async function handleAddBank(bankId, bankName) {
    // Get the actual button element that was clicked
    const btn = event.target;
    if(!btn) return;

    toggleSpinner(btn, true);
    try {
        const response = await fetchSecure(`${BANK_API_URL}/add/${bankId}`, { method: 'POST' });
        // Check response status for specific errors
        if (response.status === 409) { // Conflict - Account likely already exists
            throw new Error(`You already have an account with ${bankName}.`);
        } else if (!response.ok) {
            throw new Error(`Failed to add account. Status: ${response.status}`);
        }
        // Assuming success if we reach here
        showToast(`Successfully opened an account at ${bankName}!`);
        // Refresh only the user accounts list, not the whole page
        const listContainer = document.getElementById('userAccountList');
        const spinner = document.getElementById('accountsLoadingSpinner');
        if(listContainer) listContainer.innerHTML = ''; // Clear existing list
        if(spinner) spinner.classList.remove('hidden'); // Show spinner
        await fetchUserAccounts(); // Re-fetch user accounts to update list and stats
    } catch (error) {
        showToast(error.message, true); // Show specific error message
        console.error("Error adding bank:", error);
    } finally {
        toggleSpinner(btn, false);
    }
}


// --- Form Handlers ---
async function handleDepositSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
    if (!accountId) return; // Should not happen on account page
    const form = event.target;
    const amountInput = document.getElementById('depositAmount');
    const amount = parseFloat(amountInput.value);
    const errorDiv = document.getElementById('depositError');
    const submitButton = document.getElementById('submitDeposit');
    hideModalError(errorDiv);
    if (isNaN(amount) || amount <= 0) {
        showModalError(errorDiv, "Please enter a valid, positive amount."); return;
    }
    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/deposit`, {
            method: 'POST',
            body: JSON.stringify({ amount })
        });
        if (!response.ok) throw new Error(await response.text() || `Deposit failed. Status: ${response.status}`);
        await refreshDashboardData(); // Await refresh before closing modal
        hideModal('depositModal');
        showToast("Deposit successful!");
    } catch (error) {
        console.error("Deposit error:", error);
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleVerifyRecipient() {
    const accountNumberInput = document.getElementById('transferAccountNumber');
    const accountNumber = accountNumberInput.value.replace(/[-\s]/g, ''); // Clean input
    const errorDiv = document.getElementById('transferError');
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    const verifiedInfoDiv = document.getElementById('recipientVerifiedInfo');
    const recipientNameEl = document.getElementById('verifiedRecipientName');
    hideModalError(errorDiv);
    verifiedInfoDiv.classList.add('hidden'); // Hide previous verification

    if (!accountNumber) {
        showModalError(errorDiv, "Please enter an account number."); return;
    }
    toggleSpinner(verifyBtn, true);
    verifyBtn.textContent = 'Verifying...';

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/verify-recipient?accountNumber=${encodeURIComponent(accountNumber)}`);
        const recipientName = await response.text(); // Name or error message
        if (!response.ok) throw new Error(recipientName || `Verification failed. Status: ${response.status}`);

        recipientNameEl.textContent = recipientName;
        verifiedInfoDiv.classList.remove('hidden');
        document.getElementById('transferDetails').disabled = false;
        document.getElementById('submitTransfer').disabled = false;
        verifyBtn.textContent = 'Verified ✓';
        // Update classes for verified state (use dark theme styles)
        verifyBtn.classList.remove('bg-slate-700', 'hover:bg-slate-600');
        verifyBtn.classList.add('bg-emerald-700', 'cursor-default'); // Green for verified
        verifyBtn.disabled = true;
    } catch (error) {
        console.error("Verification error:", error);
        showModalError(errorDiv, error.message);
        resetTransferForm(); // Reset form state on verification error
    } finally {
        // Only stop spinner if not disabled (i.e., if verification failed)
        if(!verifyBtn.disabled){
            toggleSpinner(verifyBtn, false);
            verifyBtn.textContent = 'Verify'; // Reset button text on failure
        }
    }
}

function resetTransferForm() {
    // Reset details fieldset and submit button
    document.getElementById('transferDetails').disabled = true;
    document.getElementById('submitTransfer').disabled = true;
    // Hide verification info
    document.getElementById('recipientVerifiedInfo').classList.add('hidden');
    // Reset verify button state
    const verifyBtn = document.getElementById('verifyRecipientBtn');
    if(verifyBtn){
        verifyBtn.textContent = 'Verify';
        verifyBtn.classList.remove('bg-emerald-700', 'cursor-default');
        verifyBtn.classList.add('bg-slate-700', 'hover:bg-slate-600');
        verifyBtn.disabled = false;
        toggleSpinner(verifyBtn, false); // Ensure spinner is off
    }
    // Clear recipient input (optional)
    // document.getElementById('transferAccountNumber').value = '';
    // Hide error message
    hideModalError(document.getElementById('transferError'));
}

async function handleTransferSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
    if (!accountId) return;
    const recipientAccountNumber = document.getElementById('transferAccountNumber').value.replace(/[-\s]/g, '');
    const amountInput = document.getElementById('transferAmount');
    const amount = parseFloat(amountInput.value);
    const pinInput = document.getElementById('transferPin');
    const pin = pinInput.value;
    const errorDiv = document.getElementById('transferError');
    const submitButton = document.getElementById('submitTransfer');
    hideModalError(errorDiv);

    // Basic validation
    if (!recipientAccountNumber || isNaN(amount) || amount <= 0 || !pin) {
        showModalError(errorDiv, "Please complete all fields, including amount and PIN."); return;
    }
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
         showModalError(errorDiv, "PIN must be exactly 4 digits."); return;
    }

    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/transfer`, {
            method: 'POST',
            body: JSON.stringify({ recipientAccountNumber, amount, pin })
        });
        if (!response.ok) throw new Error(await response.text() || `Transfer failed. Status: ${response.status}`);
        await refreshDashboardData();
        hideModal('transferModal');
        showToast("Transfer successful!");
    } catch (error) {
        console.error("Transfer error:", error);
        // Display specific backend errors if available
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handlePayBillSubmit(event) {
    event.preventDefault();
    const accountId = document.body.dataset.accountId;
     if (!accountId) return;
    const form = event.target;
    const billerNameInput = document.getElementById('payBillBiller');
    const billerName = billerNameInput.value;
    const amountInput = document.getElementById('payBillAmount');
    const amount = parseFloat(amountInput.value);
    const pinInput = document.getElementById('payBillPin');
    const pin = pinInput.value;
    const errorDiv = document.getElementById('payBillError');
    const submitButton = document.getElementById('submitPayBill');
    hideModalError(errorDiv);

    // Basic validation
    if (!billerName || isNaN(amount) || amount <= 0 || !pin) {
        showModalError(errorDiv, "Please complete all fields, including biller, amount and PIN."); return;
    }
    if (pin.length !== 4 || !/^\d{4}$/.test(pin)) {
         showModalError(errorDiv, "PIN must be exactly 4 digits."); return;
    }

    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/paybill`, {
            method: 'POST',
            body: JSON.stringify({ billerName, amount, pin })
        });
        if (!response.ok) throw new Error(await response.text() || `Bill payment failed. Status: ${response.status}`);
        await refreshDashboardData();
        hideModal('payBillModal');
        showToast("Bill payment successful!");
    } catch (error) {
        console.error("Pay bill error:", error);
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleProfileUpdateSubmit(event) {
    event.preventDefault();
    const fullNameInput = document.getElementById('profileFullName');
    const emailInput = document.getElementById('profileEmail');
    const fullName = fullNameInput.value;
    const email = emailInput.value;
    const errorDiv = document.getElementById('profileUpdateError');
    const submitButton = document.getElementById('submitProfileUpdate');
    hideModalError(errorDiv);
    if (!fullName || !email) {
        showModalError(errorDiv, "Full name and email are required."); return;
    }
    // Simple email validation
    if (!/\S+@\S+\.\S+/.test(email)) {
         showModalError(errorDiv, "Please enter a valid email address."); return;
    }

    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/profile`, {
            method: 'PUT',
            body: JSON.stringify({ fullName, email })
        });
        if (!response.ok) throw new Error(await response.text() || `Profile update failed. Status: ${response.status}`);
        const updatedUser = await response.json();
        // Update local storage and potentially the displayed username if needed
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        const usernameDisplay = document.getElementById('username-display');
        if(usernameDisplay) usernameDisplay.textContent = updatedUser.username || localStorage.getItem('username'); // Update display if needed

        showToast("Profile updated successfully!");
    } catch (error) {
        console.error("Profile update error:", error);
        showModalError(errorDiv, error.message);
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleChangePasswordSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const currentPasswordInput = document.getElementById('currentPassword');
    const newPasswordInput = document.getElementById('newPassword');
    const currentPassword = currentPasswordInput.value;
    const newPassword = newPasswordInput.value;
    const errorDiv = document.getElementById('passwordChangeError');
    const submitButton = document.getElementById('submitPasswordChange');
    hideModalError(errorDiv);

    if (!currentPassword || !newPassword) {
        showModalError(errorDiv, "Both password fields are required."); return;
    }
     if (newPassword.length < 6) {
        showModalError(errorDiv, "New password must be at least 6 characters."); return;
    }
     if (currentPassword === newPassword) {
        showModalError(errorDiv, "New password cannot be the same as the current password."); return;
    }


    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/change-password`, {
            method: 'POST',
            body: JSON.stringify({ currentPassword, newPassword })
        });
         if (!response.ok) {
            let errorText = await response.text() || `Password change failed. Status: ${response.status}`;
             // Handle specific "Invalid password" error
            if (response.status === 400 || response.status === 401 || errorText.toLowerCase().includes("invalid password")) {
                 showModalError(errorDiv, "Incorrect current password provided.");
            } else {
                 showModalError(errorDiv, errorText);
            }
             throw new Error(errorText); // Prevent success toast
        }
        form.reset(); // Clear form on success
        showToast("Password changed successfully!");
    } catch (error) {
        console.error("Change password error:", error);
        // Error already displayed by showModalError
    } finally {
        toggleSpinner(submitButton, false);
    }
}
async function handleSetPinSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const currentPasswordInput = document.getElementById('pinCurrentPassword');
    const newPinInput = document.getElementById('newPin');
    const currentPassword = currentPasswordInput.value;
    const newPin = newPinInput.value;
    const errorDiv = document.getElementById('setPinError');
    const submitButton = document.getElementById('submitSetPin');
    hideModalError(errorDiv);

    if (!currentPassword || !newPin) {
        showModalError(errorDiv, "Please fill in both fields."); return;
    }
    if (newPin.length !== 4 || !/^\d{4}$/.test(newPin)) {
        showModalError(errorDiv, "PIN must be exactly 4 digits."); return;
    }

    toggleSpinner(submitButton, true);
    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/set-pin`, {
            method: 'POST',
            body: JSON.stringify({ password: currentPassword, pin: newPin })
        });

        if (!response.ok) {
             let errorText = await response.text() || `Failed to set PIN. Status: ${response.status}`;
            if (response.status === 401 || errorText.toLowerCase().includes("invalid password")) {
                 showModalError(errorDiv, "Invalid current password provided.");
            } else {
                 showModalError(errorDiv, errorText);
            }
             throw new Error(errorText); // Prevent success toast
        }

        form.reset();
        showToast("Security PIN updated successfully!");

    } catch (error) {
        console.error("Error setting/updating PIN:", error.message);
        // Error already displayed
    } finally {
        toggleSpinner(submitButton, false);
    }
}


// --- Handler for CSV Download ---
async function handleDownloadCsv() {
    const accountId = document.body.dataset.accountId;
    if (!accountId) {
        showToast("Could not find account ID to export.", true);
        return;
    }
    const btn = document.getElementById('download-csv-btn');
    toggleSpinner(btn, true);

    try {
        const response = await fetchSecure(`${ACCOUNT_API_URL}/${accountId}/export/csv`, { isCsv: true }); // Mark as CSV request

        if (!response.ok) {
            let errorMsg = 'Could not download statement.';
            try { // Attempt to read error details from the response
                const errText = await response.text();
                if (errText) errorMsg += ` Server said: ${errText}`;
            } catch (_) { /* Ignore if reading text fails */ }
            throw new Error(errorMsg);
        }

        const blob = await response.blob();
        // Use browser's download mechanism
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = `statement-${accountId}.csv`; // Dynamic filename
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url); // Clean up
        a.remove(); // Clean up

    } catch (error) {
        console.error('Error downloading statement:', error);
        showToast(error.message, true); // Show detailed error
    } finally {
        toggleSpinner(btn, false);
    }
}


// --- Auth Functions ---
async function handleLogin(event) {
    event.preventDefault();
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const username = usernameInput.value;
    const password = passwordInput.value;
    const errorMessage = document.getElementById('errorMessage');
    hideModalError(errorMessage);
    const submitButton = event.target.querySelector('button[type="submit"]'); // Find submit button
    toggleSpinner(submitButton, true);

    try {
        const response = await fetch(`${AUTH_API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('authToken', data.accessToken);
            localStorage.setItem('username', username); // Store username for display
            window.location.href = 'dashboard.html'; // Redirect on success
        } else {
            const errorText = await response.text();
            showModalError(errorMessage, errorText || 'Invalid username or password.');
        }
    } catch (error) {
        console.error("Login error:", error);
        showModalError(errorMessage, 'Login failed. Cannot connect to the server.');
    } finally {
        toggleSpinner(submitButton, false);
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
    const submitButton = event.target.querySelector('button[type="submit"]');
    hideModalError(errorMessage);
    successMessage.classList.add('hidden');

    // Basic validation
    if (!fullName || !username || !email || !password) {
        showModalError(errorMessage, "Please fill in all fields."); return;
    }
     if (password.length < 6) {
        showModalError(errorMessage, "Password must be at least 6 characters."); return;
    }
     if (!/\S+@\S+\.\S+/.test(email)) {
         showModalError(errorMessage, "Please enter a valid email address."); return;
    }

    toggleSpinner(submitButton, true);
    try {
        const response = await fetch(`${AUTH_API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username, email, password })
        });

        const responseText = await response.text();
        if (response.ok) {
            // Save temp credentials and redirect to create-pin page
            localStorage.setItem('tempUser', JSON.stringify({ username, password }));
            window.location.href = 'create-pin.html';
        } else {
            showModalError(errorMessage, responseText || `Registration failed. Status: ${response.status}`);
        }
    } catch (error) {
        console.error("Registration error:", error);
        showModalError(errorMessage, 'Registration failed. Cannot connect to the server.');
    } finally {
        toggleSpinner(submitButton, false);
    }
}

async function handlePinSetup(event) {
    event.preventDefault();
    const passwordInput = document.getElementById('password');
    const newPinInput = document.getElementById('newPin');
    const confirmNewPinInput = document.getElementById('confirmNewPin');
    const password = passwordInput.value;
    const newPin = newPinInput.value;
    const confirmNewPin = confirmNewPinInput.value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');
    const submitButton = document.getElementById('submitPinSetup');
    hideModalError(errorMessage);
    successMessage.classList.add('hidden');

    if (!password || !newPin || !confirmNewPin) {
        showModalError(errorMessage, "Please fill in all fields."); return;
    }
    if (newPin.length !== 4 || !/^\d{4}$/.test(newPin)) {
        showModalError(errorMessage, "PIN must be exactly 4 digits."); return;
    }
    if (newPin !== confirmNewPin) {
        showModalError(errorMessage, "PINs do not match."); return;
    }

    const tempUserData = JSON.parse(localStorage.getItem('tempUser'));
    if (!tempUserData || !tempUserData.username || !tempUserData.password) {
        showModalError(errorMessage, "Session expired or invalid. Please register again or log in.");
        localStorage.removeItem('tempUser');
        return;
    }
    if (password !== tempUserData.password) {
        showModalError(errorMessage, "Incorrect password. Please enter the password you used during registration.");
        return;
    }

    toggleSpinner(submitButton, true);
    try {
        // 1. Log in temporarily to get token
        const loginResponse = await fetch(`${AUTH_API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: tempUserData.username, password: tempUserData.password })
        });
        if (!loginResponse.ok) throw new Error("Authentication failed before setting PIN.");
        const loginData = await loginResponse.json();
        const tempToken = loginData.accessToken;

        // 2. Use token to call set-pin endpoint
        const setPinResponse = await fetch(`${ACCOUNT_API_URL}/set-pin`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${tempToken}`
            },
            body: JSON.stringify({ password: tempUserData.password, pin: newPin })
        });
        if (!setPinResponse.ok) {
             let errorText = await setPinResponse.text() || `Failed to set PIN. Status: ${setPinResponse.status}`;
             throw new Error(errorText);
        }

        // Success
        localStorage.removeItem('tempUser'); // Clean up
        successMessage.textContent = "PIN set successfully! Redirecting to login...";
        successMessage.classList.remove('hidden');
        setTimeout(() => { window.location.href = 'login.html'; }, 2000);

    } catch (error) {
        console.error("PIN Setup error:", error);
        showModalError(errorMessage, error.message);
        localStorage.removeItem('tempUser'); // Clean up on error too
        // Consider longer delay or no redirect on error
        // setTimeout(() => { window.location.href = 'login.html'; }, 4000);
    } finally {
        toggleSpinner(submitButton, false);
    }
}

function checkAuth() {
    // Check if token exists
    if (!localStorage.getItem('authToken')) {
        // Redirect to index/login page if not authenticated
        const currentPage = getPageName();
        // Allow access only to non-protected pages if not logged in
        if (currentPage !== 'index.html' && currentPage !== 'login.html' && currentPage !== 'register.html' && currentPage !== 'create-pin.html') {
             window.location.href = 'index.html'; // Or 'login.html'
        }
    } else {
        // If logged in, update username display if available
        const username = localStorage.getItem('username');
        const usernameDisplay = document.getElementById('username-display');
        if (usernameDisplay && username) {
            usernameDisplay.textContent = username;
        }
        // If logged in, maybe redirect away from login/register? (Optional)
        // const currentPage = getPageName();
        // if (currentPage === 'login.html' || currentPage === 'register.html') {
        //     window.location.href = 'dashboard.html';
        // }
    }
}
function handleLogout() {
    // Clear stored credentials and redirect
    localStorage.removeItem('authToken');
    localStorage.removeItem('username');
    localStorage.removeItem('currentUser'); // Also clear detailed user data
    localStorage.removeItem('tempUser'); // Clear any temp registration data
    window.location.href = 'index.html'; // Redirect to home/login page
}
async function refreshDashboardData() {
    // Await ensures balance is fetched before transactions/chart
    await fetchBalance();
    // Fetch for table first (if needed), then fetch for chart
    await fetchTransactions(true); // Update table content
    // No need to call fetchTransactions(false) separately if renderTransactionChart is called inside fetchTransactions
}