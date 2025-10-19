# Full-Stack Banking Application

This is a complete full-stack banking application prototype, built with a modern technology stack. It simulates core banking operations like user authentication, account management, and financial transactions within a secure, API-centric architecture.

This project was developed as a practical exploration of enterprise-level application development principles, showcasing a clear separation of concerns between the backend API and the frontend user interface.

## Core Features

- **Secure User Authentication:** JWT-based stateless authentication and registration with password encryption (BCrypt).
- **Account Management:** Every registered user is automatically assigned a unique, randomly generated account number and a balance of zero.
- **Professional UI Workflow:**
    - A clean, responsive, **dark-themed UI** built with Bootstrap 5.
    - All actions (Deposit, Transfer, Pay Bills) are handled through **Bootstrap Modals**, replacing browser `alert` and `prompt` boxes.
    - Forms include loading spinners and display specific error messages from the backend.
- **Transaction Verification:** High-value operations (Transfers, Bill Pay) require the user to **re-enter their password** for enhanced security.
- **Recipient Verification:** Before a transfer, the user must click "Verify" on the recipient's account number. The backend confirms the recipient's full name, which is then displayed in the UI to prevent accidental transfers.
- **Financial Transactions:**
    - **Deposit:** Users can deposit funds into their own account.
    - **Transfer:** Securely transfer funds to another user's verified account.
    - **Bill Pay:** Simulate paying bills to a named biller, authorized by password.
- **Recent Transactions:** The user dashboard fetches and displays a real-time list of the 10 most recent transactions.

## Technology Stack

### Backend (Spring Boot Project)
- **Framework:** Spring Boot 3
- **Security:** Spring Security 6 (JWT for stateless authentication)
- **Database:** Spring Data JPA (Hibernate) with MySQL
- **Language:** Java 17
- **Build Tool:** Maven

### Frontend (`/frontend` folder)
- **Core:** HTML5, CSS3, JavaScript (ES6+ Async/Await)
- **Framework:** Bootstrap 5.3 (for components, modals, and dark theme)
- **Architecture:** Communicates with the backend via REST API calls (`fetch`).

## How to Run

### Prerequisites
1.  **Java JDK 17+**
2.  **Apache Maven**
3.  **MySQL Database Server** (like MySQL Workbench)
4.  **Git** (for cloning)

---

### Step 1: Clone the Repository
Clone this repository to your local machine:
```sh
git clone <your-repository-url>
cd <your-repository-folder>
