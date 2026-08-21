# ServiceMate — Smart Car Service Management System 🚗🔧

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21.2-DD0031?logo=angular&logoColor=white)](https://angular.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Spring Security](https://img.shields.io/badge/Security-JWT%20%2B%20BCrypt-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![WebSocket](https://img.shields.io/badge/Real--Time-WebSocket%20%2F%20STOMP-010101?logo=socketdotio&logoColor=white)](https://spring.io/guides/gs/messaging-stomp-websocket/)
[![Google Gemini](https://img.shields.io/badge/AI-Google%20Gemini%20Flash-4285F4?logo=google&logoColor=white)](https://ai.google.dev/)
[![Tests](https://img.shields.io/badge/Tests-98%20Passing%20%2F%200%20Failures-brightgreen)](#-automated-testing--quality-assurance)

> **An AI-assisted, full-stack car service management platform connecting car owners and automotive service centers through capacity-controlled digital booking, real-time 7-stage workflow tracking, digital repair authorization, persistent notifications, tamper-evident PDF service passports, and Google Gemini AI.**

---

## 📌 Project Snapshot

| Category | Implementation & Technology |
| :--- | :--- |
| **Domain** | Automotive Service Center & Workshop Workflow Management |
| **Frontend** | Angular 21.2 (Standalone Components, Signals, Computed Signals, RxJS 7.8) |
| **Backend** | Spring Boot 3.3.6 (REST APIs, Layered Architecture, Maven 3.9) |
| **Security** | Spring Security 6 + JJWT 0.12.6 (Stateless HMAC-SHA256) + BCrypt (Strength 10) |
| **Database & ORM** | MySQL 8.0 (InnoDB, UTF8mb4) via Spring Data JPA / Hibernate |
| **Real-Time Layer** | Spring WebSocket + STOMP Messaging Protocol (`/topic`, `/queue`, SockJS) |
| **AI Integration** | Google Gemini API (`gemini-2.5-flash` with dynamic fallback cascade) |
| **Document Engine** | OpenPDF 2.0.3 (Vector-table PDF service receipts & lifetime history passports) |
| **Testing** | 98 Automated Tests (JUnit 5, Mockito, MockMvc, H2 In-Memory DB — 100% Pass) |
| **Detailed Docs** | 📘 [Complete Documentation & Interview Handbook (README.md)](./README.md) |

---

## 💡 Why ServiceMate? (Problem vs. Solution)

```
┌──────────────────────────────────────────────┐       ┌──────────────────────────────────────────────┐
│          TRADITIONAL WORKSHOP PAIN           │       │             SERVICEMATE SOLUTION             │
├──────────────────────────────────────────────┤       ├──────────────────────────────────────────────┤
│ ❌ Overbooking causes massive shop delays    │ ───>  │ ✅ Strict 2-cars-per-slot capacity engine    │
│ ❌ Unauthorized repairs & surprise charges   │ ───>  │ ✅ Digital repair authorization by customer  │
│ ❌ Constant "Is my car ready?" phone calls   │ ───>  │ ✅ Real-time 7-stage WebSocket status tracker│
│ ❌ Lost paper receipts ruin car resale value │ ───>  │ ✅ Tamper-evident OpenPDF Service Passports  │
│ ❌ Confusing mechanical jargon on bills      │ ───>  │ ✅ Gemini AI symptom advice & plain summaries│
└──────────────────────────────────────────────┘       └──────────────────────────────────────────────┘
```

---

## 🌟 Key Features

### 👤 Customer Portal
- **Vehicle Registry:** Register and manage multiple customer-owned vehicles with automated license plate normalization and odometer tracking.
- **AI Service Advisor (Google Gemini):** Describe vehicle symptoms in natural language (e.g., *"squealing noise when braking"*) to get affected systems, urgency levels, and mapped ServiceMate packages.
- **Capacity-Controlled Booking Wizard:** Select single or multiple service packages, choose from 4 daily time slots (max 2 cars per slot), add optional valet pickup/drop (₹300), and view instant pricing.
- **Real-Time Milestone Tracking:** Live tracking card updating across the 7-stage service workflow via STOMP WebSockets.
- **Digital Additional Repair Authorization:** Review newly discovered defects (estimated cost, reason) and approve or decline with a single click.
- **Persistent In-App Notification Bell:** Live unread count badge and persistent notification history.
- **Vehicle Service Passport & PDF Download:** View complete chronological maintenance logs and export professional PDF service records.

### 🏢 Service Center Cockpit
- **Master Workshop Operations Queue:** Overview of all active and completed workshop bookings with real-time status indicators.
- **7-Stage Workflow Controller:** Advance vehicles sequentially: `CAR_RECEIVED` → `INSPECTION` → `SERVICE_IN_PROGRESS` → `AWAITING_APPROVAL` → `QUALITY_CHECK` → `READY_FOR_DELIVERY` → `COMPLETED`.
- **Additional Repair Request Engine:** Generate itemized repair estimates during inspection requiring mandatory customer consent.
- **Itemized Service Record Entry:** Record 40-point diagnostic findings, replaced parts, fluids, labour line items, and finalized mileage during Quality Check.
- **AI Service Summarizer:** Automatically generate factual, customer-friendly service visit summaries using Google Gemini.

---

## 🚀 What Makes This More Than a CRUD Project?

ServiceMate is an enterprise workflow system designed around real-world business logic and defensive software engineering:

- 🛡️ **Atomic Capacity Locking:** Concurrency-safe slot allocation using synchronized interned monitors preventing overbooking under high traffic.
- ⚙️ **Strict Workshop State Machine:** Invalid workflow state jumps are blocked at the service layer (e.g., cannot complete a vehicle without 40-point diagnostics and invoice itemization).
- 🔐 **Zero-Trust Customer Data Isolation:** Every database read/write verifies customer resource ownership (`vehicle.getOwner().getId() == currentUser.getId()`), preventing IDOR (Insecure Direct Object Reference) vulnerabilities.
- 🤖 **Defensive AI Engineering:** The Gemini API key is isolated on the backend. A 5-level hierarchical resolver maps LLM outputs to Java enums, guaranteeing that **AI cannot hallucinate prices or unsupported packages**.
- ⚡ **Dual-Layer Notification Architecture:** Combines durable MySQL persistence with instant, sub-millisecond WebSocket STOMP broadcasting.
- 📄 **Programmatic PDF Compilation:** Generates structured, vector-formatted single-visit receipts and multi-visit lifetime vehicle passports in-memory using OpenPDF 2.0.3.

---

## 🏗️ System Architecture

```
                                  ┌─────────────────────────┐
                                  │     Client Browser      │
                                  │   (Customer / Admin)    │
                                  └────────────┬────────────┘
                                               │
                                 ┌─────────────┴─────────────┐
                                 │ HTTP REST (Bearer JWT)    │ WebSocket / STOMP
                                 ▼                           ▼
                ┌─────────────────────────────────────────────────────────────┐
                │             Spring Boot 3.3.6 Application Server            │
                │                                                             │
                │  ┌───────────────────────────────────────────────────────┐  │
                │  │ Security: JwtAuthenticationFilter + BCrypt + RBAC     │  │
                │  └───────────────────────────┬───────────────────────────┘  │
                │                              │                              │
                │  ┌───────────────────────────▼───────────────────────────┐  │
                │  │ Controllers: Auth, Vehicle, Booking, Workflow, AI...  │  │
                │  └───────────────────────────┬───────────────────────────┘  │
                │                              │                              │
                │  ┌───────────────────────────▼───────────────────────────┐  │
                │  │ Services: State Machine, Capacity Lock, AI Resolver   │  │
                │  └───┬────────────────┬──────────────┬────────────────┬──┘  │
                └──────┼────────────────┼──────────────┼────────────────┼─────┘
                       │                │              │                │
                       ▼                ▼              ▼                ▼
              ┌────────────────┐┌──────────────┐┌──────────────┐┌──────────────┐
              │ Spring Data JPA││ STOMP Broker ││OpenPDF Engine││Google Gemini │
              │  Repositories  ││(/topic,/user)││(Byte Stream) ││ REST Gateway │
              └────────┬───────┘└──────────────┘└──────────────┘└───────┬──────┘
                       │                                                │
                       ▼                                                ▼
              ┌────────────────┐                               ┌────────────────┐
              │ MySQL Database │                               │ Google AI Cloud│
              │(servicemate_db)│                               │(gemini-2.5-fl) │
              └────────────────┘                               └────────────────┘
```

---

## 🔄 Core Workflow & 7-Stage State Machine

```
[ Customer Books Service (Max 2 / Slot) ] ──> [ Status: CONFIRMED ]
                                                     │
                                                     ▼
                                       1. CAR_RECEIVED (Vehicle Intake)
                                                     │
                                                     ▼
                                       2. INSPECTION (40-Point Diagnostics)
                                                     │
                                 ┌───────────────────┴───────────────────┐
                                 │ (Unexpected defect found?)            │ (No extra repairs)
                                 ▼                                       │
                       3. AWAITING_APPROVAL                              │
                                 │                                       │
                         ┌───────┴───────┐                               │
                         ▼               ▼                               │
                   [ Approved ]    [ Rejected ]                          │
                         │               │                               │
                         └───────┬───────┘                               │
                                 │                                       │
                                 ▼ ◄─────────────────────────────────────┘
                       4. SERVICE_IN_PROGRESS (Repairs & Maintenance)
                                 │
                                 ▼
                       5. QUALITY_CHECK (Road Test & Itemized Billing Entry)
                                 │
                                 ▼
                       6. READY_FOR_DELIVERY (Handover Notice Pushed)
                                 │
                                 ▼
                       7. COMPLETED (Odometer Updated & PDF Record Finalized)
```

---

## 🤖 AI Service Advisor & Package Mapping

### The Flow
1. Customer describes a symptom: *"Car vibrates and squeaks when stopping."*
2. Spring Boot constructs a context-aware prompt with vehicle specifications (Make, Model, Year, Fuel, Odometer).
3. Google Gemini analyzes the symptom and returns a structured JSON payload with affected system, explanation, urgency, and recommended package.
4. **5-Level Hierarchical Resolver:** Validates and normalizes Gemini's output against the fixed `ServiceType` catalog:

```
Gemini Output: "Brake pad replacement" ──> Level 4 Keyword Match ──> ServiceType.BRAKE_SERVICE (₹1,799)
```

| Issue / Symptom Category | Mapped `ServiceType` | Base Price | What is Included |
| :--- | :--- | :---: | :--- |
| **Brake noise, pad wear, fluid flush** | `BRAKE_SERVICE` | **₹1,799** | Pad overhaul, rotor clean, line bleeding |
| **AC weak airflow, odor, low gas** | `AC_SERVICE` | **₹1,299** | Gas top-up, cabin filter clean, condenser check |
| **Dead battery, slow crank, alternator**| `BATTERY_SERVICE` | **₹499** | Voltage load test, terminal de-corrosion |
| **Scheduled engine oil & filter renewal** | `OIL_CHANGE` | **₹999** | Synthetic oil replacement & filter renewal |
| **Transmission, suspension, general checks**| `GENERAL_SERVICE` | **₹1,499** | 40-point full vehicle diagnostic inspection |

> [!IMPORTANT]
> **Pricing Source of Truth:** Gemini **never** creates bookings and **never** controls prices. All prices are calculated deterministically by the backend.

---

## 🔐 Security & Data Isolation Architecture

- **Stateless JWT Authentication:** Signed with HMAC-SHA256 (`app.jwt.secret`), validated on every request via `JwtAuthenticationFilter`.
- **BCrypt Password Hashing:** Salted, one-way encryption with cost factor 10. Passwords are never stored or logged in plaintext.
- **Role-Based Access Control (RBAC):**
  - `@PreAuthorize("hasRole('CUSTOMER')")`: Vehicle CRUD, booking creation, repair authorization.
  - `@PreAuthorize("hasRole('SERVICE_CENTER')")`: Workshop intake, stage transitions, repair creation, invoice entry.
- **IDOR Protection:** All vehicle and booking endpoints enforce `vehicle.getOwner().getId() == authUser.getId()`. Unauthorized requests are rejected with **HTTP 403 Forbidden**.
- **Secure Gemini API Gateway:** The `GEMINI_API_KEY` is loaded strictly via environment variables and never transmitted to the browser.

---

## 🗄️ Database & Entity Architecture

```
  users (1) ───────────< (N) vehicles (1) ───────────< (N) service_bookings (1)
    │                                                            │
    │ (1:N)                                               ┌──────┴──────┐
    ▼                                                     │ (1:1)       │ (1:1)
  notifications                                           ▼             ▼
                                                   service_workflows   service_records
                                                          │                 │ (1:N)
                                                          │ (1:N)           ├─< service_items
                                                          ▼                 └─< inspection_findings
                                                   additional_repairs
```

---

## 🌐 REST API Catalog

<details>
<summary><b>👉 Click to expand full REST API endpoints</b></summary>

### Authentication & User Profiles (`/api/auth`)
- `POST /api/auth/register` — Register new customer account (returns JWT)
- `POST /api/auth/login` — Authenticate and receive signed JWT
- `GET /api/auth/me` — Get authenticated profile & role

### Customer Vehicle Management (`/api/vehicles`)
- `POST /api/vehicles` — Add new vehicle (unique plate validation)
- `GET /api/vehicles` — List logged-in customer's vehicles
- `GET /api/vehicles/{id}` — Get single vehicle (ownership checked)
- `PUT /api/vehicles/{id}` — Update vehicle specifications
- `DELETE /api/vehicles/{id}` — Delete vehicle

### Capacity-Controlled Service Bookings (`/api/bookings`)
- `GET /api/bookings/availability?date=YYYY-MM-DD` — Check 4 slots (max 2 limit)
- `POST /api/bookings` — Create booking with multi-service support
- `GET /api/bookings/my` — Get customer booking history
- `GET /api/bookings/{id}` — Get booking details
- `PATCH /api/bookings/{id}/cancel` — Cancel booking & release slot capacity

### Customer Live Tracking & Repair Authorization (`/api`)
- `GET /api/bookings/{bookingId}/workflow` — Live service tracking & milestones
- `GET /api/bookings/{bookingId}/repairs` — List additional repair findings
- `POST /api/repairs/{repairId}/approve` — Authorize additional repair
- `POST /api/repairs/{repairId}/reject` — Decline additional repair

### Service Center Operations Cockpit (`/api/service-center/bookings`)
- `GET /api/service-center/bookings` — Master workshop queue
- `POST /api/service-center/bookings/{id}/receive` — Advance to `CAR_RECEIVED`
- `POST /api/service-center/bookings/{id}/start-inspection` — Advance to `INSPECTION`
- `POST /api/service-center/bookings/{id}/start-service` — Advance to `SERVICE_IN_PROGRESS`
- `POST /api/service-center/bookings/{id}/start-quality-check` — Advance to `QUALITY_CHECK`
- `POST /api/service-center/bookings/{id}/mark-ready` — Advance to `READY_FOR_DELIVERY`
- `POST /api/service-center/bookings/{id}/complete` — Finalize & mark `COMPLETED`
- `POST /api/service-center/bookings/{id}/repairs` — Create additional repair estimate
- `POST /api/service-center/bookings/{id}/service-record` — Save itemized completion details

### Digital Service History & PDF Export (`/api`)
- `GET /api/vehicles/{vehicleId}/service-history` — Complete vehicle history dossier
- `GET /api/vehicles/{vehicleId}/service-history/pdf` — Download multi-visit history PDF
- `GET /api/service-records/{recordId}` — Single finalized service record
- `GET /api/service-records/{recordId}/pdf` — Download single-visit invoice PDF

### Persistent In-App Notifications (`/api/notifications`)
- `GET /api/notifications/my` — Fetch notifications for logged-in user
- `GET /api/notifications/unread-count` — Get count of unread notifications
- `PATCH /api/notifications/{id}/read` — Mark single notification as read
- `PATCH /api/notifications/read-all` — Mark all notifications as read

### Google Gemini AI Features (`/api`)
- `POST /api/ai/service-advisor` — Natural language symptom-to-package diagnosis
- `GET /api/service-records/{recordId}/ai-summary` — Plain-English finalized service summary

</details>

---

## 🧪 Automated Testing & Quality Assurance

ServiceMate is backed by **98 automated unit and integration tests** executing offline via in-memory H2 database:

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 98, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

```
AiServiceTest (23)                  ████████████████████ 100% Passed
ServiceWorkflowTest (20)            ████████████████████ 100% Passed
ServiceHistoryTest (15)             ████████████████████ 100% Passed
ServiceBookingControllerTest (14)   ████████████████████ 100% Passed
NotificationTest (9)                ████████████████████ 100% Passed
AuthControllerTest (8)              ████████████████████ 100% Passed
VehicleControllerTest (8)           ████████████████████ 100% Passed
ServiceMateApplicationTests (1)     ████████████████████ 100% Passed
─────────────────────────────────────────────────────────────────
Total: 98 / 98 Tests Passing (0 Failures, 0 Errors, 0 Skipped)
```

---

## 📂 Project Structure

```
ServiceMate/
├── backend/                              # Spring Boot 3.3.6 Application
│   ├── src/main/java/com/example/carservice/
│   │   ├── config/                       # SecurityConfig, WebSocketConfig, DataInitializer
│   │   ├── controller/                   # REST Controllers (Auth, Booking, Workflow, AI...)
│   │   ├── dto/                          # Strongly-typed Request & Response objects
│   │   ├── entity/                       # JPA Entities (User, Vehicle, Booking, Record...)
│   │   ├── exception/                    # GlobalExceptionHandler & Custom Exceptions
│   │   ├── repository/                   # Spring Data JPA Repositories
│   │   ├── security/                     # JwtService, JwtFilter, CustomUserDetailsService
│   │   └── service/                      # State Machine, Booking, AI Advisor, OpenPDF...
│   ├── src/main/resources/
│   │   └── application.properties        # Port (8085), MySQL, JWT secret, Gemini config
│   └── src/test/                         # 98 Automated Tests (H2 In-Memory DB)
│
└── frontend/                             # Angular 21.2 Single Page Application
    ├── proxy.conf.json                   # Dev proxy to http://localhost:8085
    └── src/app/
        ├── auth/                         # Login & Registration components
        ├── core/                         # AuthGuard, AuthInterceptor, Services, Models
        ├── customer/                     # Dashboard, Vehicle List, Booking Wizard
        ├── features/                     # Live Tracker, History Passport, Bay Cockpit
        ├── service-center/               # Workshop Queue & Admin Dashboard
        └── shared/                       # Navbar, Notification Bell, Badges
```

---

## ⚡ Local Setup & Execution

### Prerequisites
- **JDK:** Version 17+
- **Node.js & npm:** Node 18+ and npm 9+
- **Database:** MySQL Server 8.0+
- **Google Gemini API Key:** (Free tier key from Google AI Studio)

### 1. Database Setup
```sql
CREATE DATABASE servicemate_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Environment Configuration
```bash
# Windows PowerShell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:GEMINI_API_KEY="AIzaSyYourGeminiApiKeyHere"

# Linux / macOS
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export GEMINI_API_KEY="AIzaSyYourGeminiApiKeyHere"
```
> [!CAUTION]
> Never commit your real Gemini API key to version control.

### 3. Run Backend (Port 8085)
```bash
cd backend
.\mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run             # Linux / macOS
```
*Preloaded accounts:*
- **Service Center Admin:** `admin@servicemate.com` / `admin123`
- **Sample Customer:** `rahul@example.com` / `password123`

### 4. Run Frontend (Port 4200)
```bash
cd frontend
npm install
npm start
```
*Access application at `http://localhost:4200` (automatically proxied to backend port 8085).*

### 5. Run Test Suite
```bash
cd backend
.\mvnw.cmd test
```

---

## 📸 UI Gallery / Demo Preview

> *Visual demonstration previews of ServiceMate's responsive interface:*

| Customer AI Advisor & Booking Wizard | Service Bay Operations Cockpit |
| :---: | :---: |
| *(AI Symptom Diagnosis & Multi-Package Slot Selection)* | *(7-Stage Milestone Advancer & Repair Approvals)* |
| **Live Service Milestone Tracker** | **Portable PDF Service Passport** |
| *(Real-Time STOMP WebSocket bay updates)* | *(Vector OpenPDF single & multi-visit history)* |

---

## 💼 Highlights for Technical Recruiters

- **Full-Stack Competence:** Modern Java 17 + Spring Boot 3 backend integrated with modern Angular 21 + TypeScript frontend.
- **Real-Time Systems:** Production-ready WebSocket/STOMP event streaming with persistent fallback.
- **Enterprise Security:** Stateless JWT security, BCrypt salting, method-level authorization, and IDOR customer data isolation.
- **Defensive AI Engineering:** Backend LLM gateway with strict JSON schemas, 5-level package resolver hierarchy, price protection, and automated 503 fallback.
- **High Test Coverage:** 98 comprehensive automated integration and unit tests passing with 0 failures.

---

## 📝 Resume-Ready Summary

### One-Line Project Description:
> **ServiceMate:** An enterprise car service management platform engineered with Spring Boot 3, Angular 21, MySQL, WebSocket/STOMP, OpenPDF, and Google Gemini AI.

### Recommended Resume Bullet Points:
- Architected a full-stack car service management system using **Spring Boot 3**, **Angular 21 (Signals)**, **Spring Security 6 (JWT & BCrypt)**, and **MySQL 8**.
- Built a concurrency-safe booking engine enforcing a hard **2-cars-per-slot limit** and a **7-stage workshop state machine** with real-time **WebSocket/STOMP** event broadcasting.
- Integrated **Google Gemini AI** for natural language symptom diagnosis with a 5-level package resolver and implemented vector-formatted PDF service passports using **OpenPDF 2.0.3**.
- Authored **98 automated unit and integration tests** achieving **100% pass rate** across security, concurrency, workflow, and AI fallback modules.

---

## 🔮 Future Scope

> [!NOTE]
> The following items represent planned future enhancements:
- 💳 Online Payment Gateway integration (Razorpay / Stripe)
- 📲 SMS & WhatsApp customer notification delivery
- 👨‍🔧 Dynamic bay and mechanic allocation algorithms
- 📦 Warehouse spare parts inventory auto-deduction
- 📱 Cross-platform mobile applications (Flutter / React Native)

---

## 📚 Documentation & References

- 📘 **Complete Technical Documentation & Interview Handbook:** [`README.md`](./README.md)
- 📄 **Recruiter Project Overview:** [`README2.md`](./README2.md)

---

## 📄 License
Developed for educational, demonstration, and technical recruitment evaluation purposes.  
All rights reserved © 2026 ServiceMate Team.
