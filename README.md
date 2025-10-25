# 💳 Unified Banking System - Full-Stack Bank Aggregator Portal

A sophisticated full-stack banking portal that simulates a **real-world multi-bank aggregation platform**.
This project demonstrates a **secure, modern, and scalable banking system** built with **Spring Boot (Java)** and a professional **Tailwind CSS + Vanilla JS frontend** — ideal for showcasing academic or professional skills.

---

## ✨ Features Overview

### 🎯 **Architecture & User Experience**

* ✅ **Multi-Bank Aggregation:** Users register once and can link/manage accounts from multiple simulated banks (ICICI, HDFC, etc.).
* 🎨 **Modern UI/UX:** Custom-designed with **Tailwind CSS**, **glassmorphism**, **navy/blue gradients**, and **Inter font** for professional aesthetics.
* 🌗 **Dark / Light Mode:** Theme toggle with preference saved in `localStorage`.
* 📊 **Smart Dashboard:** Displays **income vs expense visual charts** using Chart.js.
* 🚀 **Smooth Page Flow:** Landing page → Dashboard → Individual bank account pages.

---

### 🔐 **Security & Authentication**

| Feature                      | Description                                                          |
| ---------------------------- | -------------------------------------------------------------------- |
| **JWT Authentication**       | Stateless Spring Security + JSON Web Tokens                          |
| **Encrypted PIN Setup**      | 4-digit PIN created post-registration and stored securely            |
| **Password Re-Verification** | Required for high-risk actions like fund transfer and bill payment   |
| **Recipient Verification**   | Validates account number and displays recipient name before transfer |

---

### 🏦 **Core Banking Functionalities**

* 💸 **Transactions:** Deposit, Transfer (with PIN), Bill Payment (with PIN).
* 💳 **Debit Card Management:**
    * View Card Details (Number, Holder, Expiry).
    * Interactive Card Flip to reveal CVV (fetched securely on demand).
    * Freeze / Unfreeze Card (Master Toggle).
    * Enable / Disable Online Transactions.
    * Enable / Disable International Transactions.
* 💰 **Loan Application:** Simple form within account view to submit basic loan requests (frontend simulation).
* 📊 **Real-Time Feeds:** View recent transactions on account pages.
* 📝 **Activity Log:** Track important account-specific changes (card settings) and user-level changes (profile updates, password/PIN changes) with timestamps.
* ⚙️ **Profile & Settings:** Update personal details (name, email, phone, DOB, address, nominee) and securely change password/PIN.
* 🔗 **Account Linking:** Add accounts from available banks via the dashboard.
* 📄 **Statement Download:** Export account transactions as a CSV file.
---

## 📂 Project Structure

```
/
├── frontend/                     # All UI files (HTML, CSS, JS)
│   ├── account.html
│   ├── create-pin.html
│   ├── dashboard.html
│   ├── index.html
│   ├── login.html
│   ├── register.html
│   ├── app.js
│   └── style.css
│
├── src/main/java/com/example/bankingapp/
│   ├── config/                   # Spring Security configuration
│   ├── controller/               # REST API controllers
│   ├── dto/                      # Data Transfer Objects
│   ├── model/                    # JPA Entities (User, Account, Bank)
│   ├── repository/               # Spring Data JPA repositories
│   ├── security/                 # JWT and UserDetails logic
│   └── service/                  # Business logic
│
├── .gitignore
├── pom.xml                       # Maven project configuration
└── README.md                     # You are here!
```

---

## 🛠️ Tech Stack

| Layer           | Technology                                                     |
| --------------- | -------------------------------------------------------------- |
| **Backend**     | Spring Boot 3, Java 17, Spring Security (JWT), Spring Data JPA ,Lombok|
| **Frontend**    | Tailwind CSS, HTML5, Vanilla JS (ES6), Bootstrap Icons         |
| **Database**    | MySQL                                                          |
| **Other Tools** | Chart.js, Maven, Google Fonts (Inter)                          |

---

## 🚀 Getting Started

### ✅ **Prerequisites**

* Java JDK 17+ installed and configured (check `java -version`).
* Maven installed and configured (check `mvn -v`).
* MySQL Server running.
* Git installed.
* IDE with Lombok plugin support recommended (e.g., IntelliJ IDEA, VS Code with extensions).
---

### 📌 **1. Clone the Repository**

```bash
git clone <your-repository-url>
cd <your-repository-folder>
```

---

### 📌 **2. Configure Database**

```sql
CREATE DATABASE banking_db;
```

Update credentials in `src/main/resources/application.properties`:

```
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

---

### 📌 **3. Run the Backend**

**On Windows:**

```bash
.\mvnw.cmd spring-boot:run
```

**On Mac/Linux:**

```bash
./mvnw spring-boot:run
```

Server runs at:
👉 [http://localhost:8080](http://localhost:8080)

---

### 📌 **4. Launch the Frontend**

Open:

```
/frontend/index.html
```

Start by clicking **Register → Login → Dashboard.**

---

## 🧠 Future Enhancements

* 💰 AI-based expense categorization
* 🔔 Email & SMS notifications
* 🌍 Integration with real bank APIs (e.g., Razorpay Sandbox)

---

## 📜 License

This project is released under the **MIT License** — feel free to fork, modify, and improve!

---

**Developed with ❤️ using Java + Tailwind CSS**
