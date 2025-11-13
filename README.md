🚆 Train Ticket Office System

This is a web application project built with Java Spring Boot, simulating a complete train ticket management and booking system. The system supports two main user roles: CUSTOMER and STAFF, each with distinct business logic.

The project integrates a full range of business flows, from searching, booking, and payment (via VNPay), to ticket management and refund/cancellation processing.

🚀 Key Features

The system is divided into two main modules:

1. Customer Module

Registration & Login: Manages customer accounts.

Trip Search:

Searches for one-way and round-trip tickets between stations.

Displays search results including minimum prices and available seat counts.

Booking Flow:

Step 1 (Seat Selection): Displays an intuitive visual seat map. Seats are categorized by status: Sold, Held (awaiting payment), and Available.

Step 2 (Enter Information): A form for passenger details (Name, ID Card, Date of Birth) with age validation logic (Adult, Child, Infant, Senior) to apply automatic discounts.

Step 3 (Order Creation): The system creates one or more Order objects (e.g., 2 Orders for a round-trip) and groups them using a roundTripGroupId.

Payment:

Integrated with the VNPay payment gateway.

Handles combined payments for round-trip orders (one-time payment for both legs).

Automatically cancels a Booking if not paid within 15 minutes.

Ticket Management:

Customers can review all their booked tickets (/bookings).

Cancel tickets (if in "Awaiting Payment" status).

Refund Request:

Customers can request a refund for paid tickets (status PAID).

The request is moved to PENDING_REFUND status to await admin approval.

2. Administrator (Admin/Staff) Module

Dashboard: An overview panel displaying key management functions.

Station Management: Add/edit/delete train stations. Each station has a distanceKm attribute (KM from the origin station) used as a basis for price calculation.

Route Management: Connects two Stations (origin, destination) to create a route.

Seat Type Management: Defines seat categories and, most importantly, the price per KM (pricePerKm). E.g., "Soft Seat" (700 VND/km), "VIP Sleeper" (1100 VND/km).

Train Management: Manages the list of train fleets (SE1, SE2...).

Carriage Management: Add/edit/delete carriages, assign a carriage to a specific Train, and select its SeatType.

Seat Management: Create seats (A1, A2...) for a specific Carriage.

Trip Management:

Creates trips by combining a Train (which train), a Route (where it's going), and departureTime/arrivalTime (when).

Admins can update trip statuses (Upcoming, In Progress, Completed, Cancelled).

Refund Management:

Admins see a list of pending refund requests (PENDING_REFUND).

Approve: The ticket status changes to REFUNDED, the seat is released back for sale (AVAILABLE), and the amount is deducted from the AdminWallet.

Reject: The ticket status reverts to PAID.

Revenue Management: The system features an AdminWallet to log total revenue from successful payments and deduct money when approving refunds.

⚙️ Technology Stack

Backend: Java 17, Spring Boot 3.5.5

Database: MySQL (JDBC) and Spring Data JPA (Hibernate)

Frontend: Thymeleaf, Thymeleaf Layout Dialect

Styling: Bootstrap 5, Custom CSS (with backdrop-filter: blur effects)

Payment: VNPay (sandbox) integration

Build: Maven

Other: Lombok

📁 Project Structure (Highlights)

/src/main/
├── java/com/example/trainticketoffice/
│   ├── common/         # (Enums: BookingStatus, TripStatus, etc.)
│   ├── config/         # (AuthenticationFilter, DataInitializer)
│   ├── controller/     # (Separated admin and customer logic)
│   │   ├── AdminController.java
│   │   ├── AdminRefundController.java
│   │   ├── BookingController.java  # (Main booking logic)
│   │   ├── PaymentController.java  # (VNPay logic)
│   │   ├── RefundController.java   # (Customer refund logic)
│   │   ├── StationController.java
│   │   ├── TrainController.java
│   │   ├── TripController.java
│   │   └── ...
│   ├── model/          # (Entities: User, Train, Trip, Booking, Order...)
│   ├── repository/     # (JpaRepositories)
│   ├── service/        # (Interfaces)
│   │   └── impl/       # (Service Implementations)
│   │       ├── BookingServiceImpl.java
│   │       ├── PaymentServiceImpl.java
│   │       └── TripServiceImpl.java
│   └── util/           # (VnpayUtils.java)
│
└── resources/
    ├── static/         # (CSS, Images)
    ├── templates/      # (Thymeleaf HTML files)
    │   ├── admin/
    │   ├── customer/
    │   ├── fragments/  # (Shared layouts)
    │   ├── payment/    # (Payment forms, invoice)
    │   ├── refund/
    │   ├── ticket/     # (Seat map, passenger form)
    │   └── ...
    └── application.properties # (DB & VNPay Config)


📈 Key Business Flows

1. Fare Calculation Flow

The ticket price is calculated automatically based on several factors:

Distance: distanceKm (from the Station).

Base Rate: pricePerKm (from the carriage's SeatType).

Surcharge: HOLIDAY_SURCHARGE_RATE (e.g., 1.2) if the departure date is a public holiday (defined in BookingServiceImpl).

Discount: A percentage discount (e.g., for Children, Seniors) is applied based on the passengerType from the info step.

Simple Formula: Price = (distanceKm * pricePerKm * Surcharge) * %Discount

2. Round-Trip Booking Flow

A customer selects "Round Trip" and searches.

The system saves the return leg info (RoundTripInfo) into the HttpSession.

The customer selects a ticket for the Outbound leg (context="outbound").

BookingController creates Order 1 with a new roundTripGroupId (UUID).

The system redirects the customer back to the search results for the Return leg.

The customer selects a ticket for the Return leg (context="inbound").

BookingController creates Order 2 using the same roundTripGroupId from the session.

The customer is sent to the payment page. PaymentController sees the roundTripGroupId and calculates the total price for both Order 1 and Order 2.

The customer pays a single time via VNPay for both orders.

🚀 How to Run the Project

Clone the repository:

git clone [YOUR_REPO_URL]
cd Train-Ticket-Office


Configure the Database:

Open MySQL Workbench or your DB manager and create a new schema, e.g., core_tto.

Open src/main/resources/application.properties.

Update spring.datasource.url, spring.datasource.username, and spring.datasource.password to match your MySQL configuration.

Configure VNPay:

In application.properties, replace the vnpay.tmn-code and vnpay.secret-key values with your own VNPay sandbox credentials.

Ensure vnpay.return-url is set to http://localhost:8080/payments/vnpay-return (or your application's port).

Run the application:

Open the project in your IDE (IntelliJ/Eclipse).

Run the TrainTicketOfficeApplication.java file.

The system will automatically create the tables (due to spring.jpa.hibernate.ddl-auto=create-drop) and add sample data (from DataInitializer.java).

Access:

Customer Homepage: http://localhost:8080/

Login Page: http://localhost:8080/login

👨‍💻 Sample Accounts

These accounts are automatically created by DataInitializer.java:

Admin (STAFF):

Email: staff@example.com

Password: password123

Customer (CUSTOMER):

Email: customer@example.com

Password: password123
