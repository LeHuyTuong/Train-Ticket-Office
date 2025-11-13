# 🚆 Train Ticket Office System

> Full-stack web application built with **Java Spring Boot** that simulates a complete **train ticket management and booking system**.  
> The system supports two main roles: **CUSTOMER** and **STAFF (Admin)**, each with dedicated business flows.

The project covers the end-to-end lifecycle:  
**Search → Booking → Payment (VNPay) → Ticket Management → Refund / Cancellation → Revenue Tracking**.

---

## 📚 Table of Contents

1. [System Overview](#-system-overview)  
2. [Feature Breakdown](#-feature-breakdown)  
   - [Customer Module](#1-customer-module)  
   - [Administrator / Staff Module](#2-administrator-staff-module)  
3. [Technology Stack](#-technology-stack)  
4. [Project Structure](#-project-structure-highlights)  
5. [Key Business Flows](#-key-business-flows)  
   - [Fare Calculation Flow](#1-fare-calculation-flow)  
   - [Round-trip Booking Flow](#2-round-trip-booking-flow)  
6. [Getting Started](#-getting-started)  
   - [Prerequisites](#prerequisites)  
   - [Clone & Setup](#1-clone-the-repository)  
   - [Database Configuration](#2-configure-the-database)  
   - [VNPay Configuration](#3-configure-vnpay)  
   - [Run the Application](#4-run-the-application)  
7. [Access URLs](#-access-urls)  
8. [Sample Accounts](#-sample-accounts)

---

## 🌐 System Overview

This system provides:

- A **customer-facing portal** to search trips, book tickets (one-way or round-trip), pay online, manage tickets, and request refunds.
- An **admin-facing portal** to manage stations, routes, trains, carriages, seats, trips, refund approvals, and overall revenue.

The application is built using **Spring Boot + Thymeleaf** with **VNPay sandbox integration** for payment processing.

---

## 🧩 Feature Breakdown

### 1. Customer Module

#### 🔐 Registration & Login

- Manage customer accounts with basic authentication.
- Login is required to create bookings and manage tickets.

#### 🔍 Trip Search

- Search trips by:
  - **Route** (origin station → destination station)
  - **Travel type**: one-way or round-trip
- Display:
  - Minimum price per trip
  - Available seat count

#### 🎟️ Booking Flow

**Step 1 – Seat Selection**

- Visual seat map per carriage.
- Seat status classification:
  - `Sold` – ticket already purchased
  - `Held` – seat is reserved, awaiting payment
  - `Available` – seat can be booked

**Step 2 – Passenger Information**

- Form includes:
  - Full name
  - ID Card
  - Date of Birth
- System derives **passenger type** (Adult / Child / Infant / Senior)  
  → Used to apply **automatic discounts**.

**Step 3 – Order Creation**

- System creates:
  - **1 Order** for one-way booking
  - **2 Orders** for round-trip booking
- Round-trip Orders are grouped via a shared `roundTripGroupId` (UUID).

#### 💳 Payment (VNPay)

- Integrated with **VNPay Sandbox**.
- Supports:
  - **Single payment** for both legs in a round-trip (using `roundTripGroupId`).
- Auto-cancellation:
  - If payment is not completed within **15 minutes**, related bookings are cancelled.

#### 🎫 Ticket Management (Customer)

- View all bookings at `/bookings`.
- Actions:
  - **Cancel** ticket if status = `AWAITING_PAYMENT`.
  - **Request refund** if status = `PAID` → moved to `PENDING_REFUND`.

#### 💸 Refund Request (Customer)

- Customer can request a refund for paid tickets.
- After request:
  - Ticket moves from `PAID` → `PENDING_REFUND`.
  - Awaiting approval/rejection by Admin.

---

### 2. Administrator / Staff Module

#### 📊 Dashboard

- Central view to navigate to:
  - Stations, Routes, Trains, Carriages, Trips
  - Refund Requests
  - Revenue (AdminWallet)

#### 🏙️ Station Management

- Add / edit / delete **Stations**.
- Each station has:
  - `distanceKm`: distance from the origin station (used in fare calculation).

#### 🛣️ Route Management

- Define **routes** by linking:
  - `originStation` → `destinationStation`.

#### 💺 Seat Type Management

- Configure **seat types** with:
  - Name: e.g., `Soft Seat`, `VIP Sleeper`
  - `pricePerKm`: base price per kilometer  
    - Example:
      - Soft Seat: `700 VND/km`
      - VIP Sleeper: `1100 VND/km`

#### 🚆 Train Management

- Manage train fleets (e.g. `SE1`, `SE2`, ...).

#### 🚃 Carriage Management

- Add / edit / delete **Carriages**.
- Assign carriage to:
  - A specific **Train**
  - A specific **SeatType**.

#### 🪑 Seat Management

- Create seat codes (e.g. `A1`, `A2`, ...) inside a carriage.

#### 🧭 Trip Management

- Define **Trips** as:
  - Train + Route + departureTime + arrivalTime.
- Manage trip status:
  - `UPCOMING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.

#### 💵 Refund Management

- View list of **pending refund requests** (`PENDING_REFUND`).
- Actions:
  - **Approve**:
    - Ticket → `REFUNDED`
    - Seat returned to `AVAILABLE`
    - Amount deducted from **AdminWallet**
  - **Reject**:
    - Ticket status reverts to `PAID`.

#### 💰 Revenue Management (AdminWallet)

- Logs:
  - Revenue from successful payments.
  - Deduction for approved refunds.
- Acts as a simple **wallet ledger** for the administrator side.

---

## ⚙️ Technology Stack

- **Backend**:  
  - Java 17  
  - Spring Boot 3.5.5  
  - Spring Web, Spring Data JPA (Hibernate)  
- **Database**:  
  - MySQL (JDBC)  
- **Frontend**:  
  - Thymeleaf  
  - Thymeleaf Layout Dialect  
- **Styling**:  
  - Bootstrap 5  
  - Custom CSS (including `backdrop-filter: blur` effects)  
- **Payment**:  
  - VNPay (Sandbox)  
- **Build Tool**:  
  - Maven  
- **Other**:  
  - Lombok  

---

## 📁 Project Structure (Highlights)

```text
/src/main/
├── java/com/example/trainticketoffice/
│   ├── common/         # Enums: BookingStatus, TripStatus, etc.
│   ├── config/         # AuthenticationFilter, DataInitializer, etc.
│   ├── controller/     # Separated admin and customer controllers
│   │   ├── AdminController.java
│   │   ├── AdminRefundController.java
│   │   ├── BookingController.java      # Main booking logic
│   │   ├── PaymentController.java      # VNPay integration
│   │   ├── RefundController.java       # Customer refund logic
│   │   ├── StationController.java
│   │   ├── TrainController.java
│   │   ├── TripController.java
│   │   └── ...
│   ├── model/          # Entities: User, Train, Trip, Booking, Order, ...
│   ├── repository/     # JpaRepositories
│   ├── service/        # Service interfaces
│   │   └── impl/       # Service implementations
│   │       ├── BookingServiceImpl.java
│   │       ├── PaymentServiceImpl.java
│   │       └── TripServiceImpl.java
│   └── util/           # VnpayUtils.java
│
└── resources/
    ├── static/         # CSS, Images
    ├── templates/      # Thymeleaf HTML templates
    │   ├── admin/
    │   ├── customer/
    │   ├── fragments/  # Shared layouts
    │   ├── payment/    # Payment forms, invoice
    │   ├── refund/
    │   ├── ticket/     # Seat map, passenger form
    │   └── ...
    └── application.properties  # DB & VNPay configuration
```

---

## 📈 Key Business Flows

### 1. Fare Calculation Flow

Ticket price is calculated automatically based on:

- **Distance**: distanceKm (from Station configuration)
- **Base Rate**: pricePerKm (from SeatType)
- **Surcharge**: `HOLIDAY_SURCHARGE_RATE` (e.g. 1.2) if departure date is a configured public holiday
- **Discount**: based on passengerType (Child, Senior, etc.)

**Formula:**

```
Price = distanceKm * pricePerKm * Surcharge * DiscountRate
```

Where:
- `Surcharge` = 1.0 or holiday multiplier (e.g. 1.2)
- `DiscountRate` = e.g. 0.8 for Child, 1.0 for Adult, etc.

---

### 2. Round-trip Booking Flow

1. Customer selects **Round Trip** and performs a search.
2. System stores return leg data (RoundTripInfo) in HttpSession.
3. Customer chooses ticket for the **Outbound leg** (`context="outbound"`).
4. **BookingController:**
   - Creates Order 1
   - Generates `roundTripGroupId` (UUID) and stores in session.
5. System redirects to search results for the **Return leg**.
6. Customer selects ticket for the **Return leg** (`context="inbound"`).
7. **BookingController:**
   - Creates Order 2
   - Reuses the same `roundTripGroupId`.
8. Customer is redirected to the **payment page**.
9. **PaymentController:**
   - Detects `roundTripGroupId`
   - Aggregates both orders
   - Processes one VNPay payment for the total amount.

---

## 🚀 Getting Started

### Prerequisites

- JDK 17+
- MySQL 8.x
- Maven 3.8+
- VNPay Sandbox account (TMN code & Secret key)

### 1. Clone the Repository

```bash
git clone [YOUR_REPO_URL]
cd Train-Ticket-Office
```

### 2. Configure the Database

Open MySQL Workbench (or your DB client).

Create a new schema:

```sql
CREATE DATABASE core_tto CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update your DB credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/core_tto
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=create-drop
```

⚠️ For production, change `ddl-auto` to `update` or `validate`.

### 3. Configure VNPay

Add the following in `application.properties`:

```properties
vnpay.tmn-code=YOUR_VNPAY_TMN_CODE
vnpay.secret-key=YOUR_VNPAY_SECRET_KEY
vnpay.return-url=http://localhost:8080/payments/vnpay-return
```

Adjust the return URL if running on a different port or context path.

### 4. Run the Application

Run via IDE or Maven:

**Using IDE:**
```
Run TrainTicketOfficeApplication.java as a Spring Boot app
```

**Using CLI:**
```bash
mvn spring-boot:run
```

On startup, the system will:
- Auto-generate tables (via JPA)
- Insert demo data via `DataInitializer.java`

---

## 🌍 Access URLs

- **Customer Homepage:**  [http://localhost:8080/](http://localhost:8080/)
- **Login Page:**  [http://localhost:8080/login](http://localhost:8080/login)

(Admin features are accessible post-login as STAFF.)

---

## 👨‍💻 Sample Accounts

### Admin / Staff
```
Email: staff@example.com
Password: password123
```

### Customer
```
Email: customer@example.com
Password: password123
```

---

If you need a section for screenshots, API docs, or known issues, it can be appended later for demo or submission purposes.

