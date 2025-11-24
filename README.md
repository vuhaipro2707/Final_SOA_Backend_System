# **Chat Application (SOA/CQRS)**

This repository contains a comprehensive backend implementation for a real-time chat application leveraging **Service-Oriented Architecture (SOA)** with **Command Query Responsibility Segregation (CQRS)** pattern and event-driven communication via Apache Kafka.

The system provides:
- **Authentication & Authorization** with JWT (HttpOnly cookies)
- **Customer Management** (profile, password reset with OTP)
- **Chat Command Operations** (write operations: create rooms, send messages, update read markers)
- **Chat Query Operations** (read operations: fetch rooms, messages, online status, read markers)
- **Real-time Communication** via WebSockets (STOMP protocol)

## **🚀 Architecture Overview**

The system follows a microservices architecture with clear separation of concerns:

| Component | Technology | Role |
| :---- | :---- | :---- |
| **API Gateway** | Python (FastAPI) | Single entry point for all HTTP requests. Validates JWT tokens, extracts customer ID, and routes requests to appropriate backend services. |
| **Nginx** | Reverse Proxy | External load balancer routing traffic to API Gateway and WebSocket Service. |
| **Auth Service** | Spring Boot (Java 21) + PostgreSQL | Handles login/logout operations. Issues RSA-signed JWT tokens stored in HttpOnly cookies. |
| **Customer Management Service** | Spring Boot (Java 21) + PostgreSQL | Manages customer profiles, account creation, password changes, and password reset with OTP verification. |
| **OTP Service** | Spring Boot (Java 21) + In-Memory Storage (Redis) | Generates, validates, and manages time-limited OTP codes for password reset flows. |
| **Mail Service** | Spring Boot (Java 21) + SMTP | Sends transactional emails (OTP codes, notifications). |
| **Chat Command Service** | Spring Boot (Java 21) + PostgreSQL + Kafka Producer | Handles all **write operations** (create rooms, send messages, update read markers). Publishes domain events to Kafka. |
| **Chat Query Service** | Spring Boot (Java 21) + MongoDB + Redis + Kafka Consumer | Handles all **read operations** (fetch rooms, messages, online status). Maintains read-optimized projections by consuming Kafka events. |
| **WebSocket Service** | Spring Boot (Java 21) + STOMP/WebSocket + Redis + Kafka Consumer | Manages real-time bidirectional connections. Pushes live updates (messages, typing indicators, online status) to connected clients. |
| **PostgreSQL** | Relational Database | Source of truth for write models (customers, chat rooms, messages). |
| **MongoDB** | Document Database | Read-optimized projections for chat views and message history. |
| **Redis** | In-Memory Cache | Provide high-speed storage for transient data: 1. OTP & Reset Flow: Store time-limited OTP codes (TTL 300s) and password reset confirmation status (TTL 600s). 2. Real-time Presence: Track user Online Status (TTL 120s) and manage Typing Indicators (TTL 2s). 3. TTL Events: Utilize Keyspace Notifications to trigger automatic Offline and Stop Typing logic. |
| **Apache Kafka** | Event Streaming Platform | Asynchronous communication between services via domain events. |


## **🛠️ Local Development Setup**

The entire stack is containerized and orchestrated with Docker Compose.

### **Prerequisites**

- **Docker** and **Docker Compose** installed
- **Python 3.x** (for generating RSA key pairs)

### **Step 1: Configure Environment Variables**

Create a file named `.env` in the project root directory using the `.env example` content and replace the placeholders with your actual Gmail details. You must use a Gmail **App Password** for `MAIL_PASSWORD`.

```bash:.env example:.env
MAIL_USERNAME=your_sender_email@gmail.com
MAIL_PASSWORD=your_app_specific_password # Use App Password for Gmail
```

### **Step 2: Generate RSA Key Pairs**

The system uses asymmetric RSA keys for JWT signing and verification:
- **Private Key**: Used by Auth Service to sign JWTs
- **Public Key**: Used by API Gateway and WebSocket Service to verify JWTs

Generate the keys using the provided script:

```bash
python key.py
```

This creates:
- `auth-service/src/main/resources/keys/private_key.pem`
- `ApiGateway/app/public_key.pem`
- `websocket-service/src/main/resources/keys/public_key.pem`

⚠️ **Important**: Run this before starting the services, as authentication will fail without proper keys.

### **Step 3: Start All Services**

Build and launch all containers in detached mode:

```bash
docker-compose up --build -d
```

**Startup time**: Allow 1-2 minutes for Kafka, databases, and Spring Boot applications to fully initialize.

### **Step 4: Access Points**

| Service | URL | Description |
| :---- | :---- | :---- |
| **API Gateway** | http://localhost:8080 | Main REST API endpoint (all HTTP requests) |
| **WebSocket Endpoint** | ws://localhost:8080/ws/chat | STOMP WebSocket connection for real-time updates |
| **Frontend** | http://localhost:5500 | Chat application UI (if running `index.html` via live server) |
| **Mongo Express** | http://localhost:8087 | MongoDB GUI (view chat projections) |
| **Adminer** | http://localhost:8088 | PostgreSQL GUI (view source data) |

### **Step 5: Test Accounts**

Pre-seeded accounts (created by Auth Service DataInitializer):

| Username | Password | Customer ID | Purpose |
| :---- | :---- | :---- | :---- |
| `user1` | `123` | 1 | Test user 1 |
| `user2` | `1234` | 2 | Test user 2 |
| `user3` | `12345` | 3 | Test user 3 |


---

## **🔐 Authentication & Security**

### **JWT Cookie-Based Authentication**

The system uses **JWT tokens stored in HttpOnly cookies** for secure, stateless authentication:

1. **Login Process**:
   - Client sends credentials to `POST /auth/login`
   - Auth Service validates credentials against PostgreSQL
   - On success, generates RSA-signed JWT containing `customerId`
   - Sets JWT in `jwt_token` HttpOnly cookie (prevents XSS attacks)

2. **Request Authentication**:
   - API Gateway extracts `jwt_token` cookie from incoming requests
   - Validates JWT signature using public key
   - Extracts `customerId` and forwards to backend via `X-Customer-Id` header
   - Backend services use custom `GatewayAuthFilter` to reconstruct authentication context

3. **Logout Process**:
   - Client calls `POST /auth/logout`
   - API Gateway clears the `jwt_token` cookie

### **Endpoint Security Levels**

| Security Level | Endpoints | Description |
| :---- | :---- | :---- |
| **Public** | `/auth/login`, `/auth/logout`, `/customer/create/account`, `/customer/forgetPass/**` | No authentication required |
| **Authenticated** | All `/customer/**`, `/command/**`, `/query/**` endpoints | Requires valid JWT cookie |
| **Internal** | All `/internal/**` endpoints | Service-to-service communication only (protected by internal filters) |

---

## **📡 API Documentation**

Base URL: `http://localhost:8080`

> 📖 For detailed request/response schemas, see `ApiGateway/app/openapi.yaml`

### **1. Authentication Endpoints**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| POST | `/auth/login` | 🔓 Public | Login and receive JWT cookie |
| POST | `/auth/logout` | 🔓 Public | Clear JWT cookie and logout |

---

### **2. Customer Management Endpoints**

#### **Account Management**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| POST | `/customer/create/account` | 🔓 Public | Create a new customer account |
| GET | `/customer/info` | 🔒 Authenticated | Get current user's profile information |
| POST | `/customer/info` | 🔒 Authenticated | Update current user's profile |
| POST | `/customer/changePass` | 🔒 Authenticated | Change password for authenticated user |

#### **Password Reset Flow**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| POST | `/customer/forgetPass/initiate` | 🔓 Public | Start password reset by sending OTP to email |
| POST | `/customer/forgetPass/resend` | 🔓 Public | Resend OTP code to email |
| POST | `/customer/forgetPass/confirm` | 🔓 Public | Verify OTP code |
| POST | `/customer/forgetPass/reset` | 🔓 Public | Reset password after OTP confirmation |

#### **Customer Search**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| GET | `/customer/info/customerId/{customerId}` | 🔒 Authenticated | Get customer details by ID |
| GET | `/customer/info/phoneNumber/{phoneNumber}` | 🔒 Authenticated | Search customers by phone number (contains) |
| GET | `/customer/info/fullName/{fullName}` | 🔒 Authenticated | Search customers by full name (contains) |
| GET | `/customer/fullName/customerId/{customerId}` | 🔒 Authenticated | Get customer's full name by ID |

---

### **3. Chat Command Endpoints (Write Operations)**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| POST | `/command/room` | 🔒 Authenticated | Create a new chat room |
| POST | `/command/message` | 🔒 Authenticated | Send a message to a room |
| POST | `/command/read` | 🔒 Authenticated | Update read marker (mark messages as read) |

---

### **4. Chat Query Endpoints (Read Operations)**

| Method | Endpoint | Auth | Description |
| :---- | :---- | :---- | :---- |
| GET | `/query/rooms` | 🔒 Authenticated | Get all chat rooms for current user |
| GET | `/query/message/roomId/{roomId}` | 🔒 Authenticated | Get the latest 20 messages in a room |
| GET | `/query/message/roomId/{roomId}/index/{indexMessageId}` | 🔒 Authenticated | Get next batch of older messages (pagination) |
| GET | `/query/onlineStatus/roomId/{roomId}` | 🔒 Authenticated | Get online status of all room participants |
| GET | `/query/readMarkers/roomId/{roomId}` | 🔒 Authenticated | Get all read markers for a room |

---

### **5. WebSocket Communication**

#### **Connection Setup**

- **Endpoint**: `ws://localhost:8080/ws/chat`
- **Protocol**: STOMP over WebSocket
- **Authentication**: Include `jwt_token` cookie in handshake

#### **Client Subscriptions (Receive)**

| Destination | Type | Description |
| :---- | :---- | :---- |
| `/user/topic/rooms` | Private | Receive updated room views (unread status, last message) |
| `/user/topic/readStatus` | Private | Receive room-specific unread status changes |
| `/topic/message/roomId/{roomId}` | Public | Receive new messages in real-time |
| `/topic/onlineStatus/roomId/{roomId}` | Public | Receive online/offline status updates |
| `/topic/readMarkers/roomId/{roomId}` | Public | Receive read marker updates |
| `/topic/typing/roomId/{roomId}` | Public | Receive typing indicators |

#### **Client Sends (Publish)**

| Destination | Description |
| :---- | :---- |
| `/app/extendOnline` | Extend user's online presence (refresh TTL in Redis) |
| `/app/typing` | Send typing/stop-typing indicator to room |

---

### **6. Internal Endpoints (Service-to-Service Only)**

⚠️ **DO NOT call these from client applications**

| Method | Endpoint | Service | Description |
| :---- | :---- | :---- | :---- |
| POST | `/internal/generate/email` | OTP Service | Generate OTP code for email |
| POST | `/internal/resend/email` | OTP Service | Resend OTP code |
| POST | `/internal/validate/email` | OTP Service | Validate OTP code |
| POST | `/internal/send` | Mail Service | Send email |
| GET | `/internal/rooms/customerId/{id}` | Query Service | Get rooms for specific customer |
| GET | `/internal/valid/roomId/{roomId}/customerId/{id}` | Query Service | Check if customer is room participant |

---

## **🔄 CQRS Architecture & Event Flow**

The chat system implements **CQRS** (Command Query Responsibility Segregation) for optimal read/write performance:

### **Write Side (Command)**

1. Client sends command (e.g., `POST /command/message`)
2. **Chat Command Service**:
   - Validates request and authorization
   - Writes to **PostgreSQL** (source of truth)
   - Publishes domain event to **Kafka** (e.g., `MessageSentEvent`)
   - Returns success response immediately

### **Read Side (Query)**

1. **Chat Query Service** (Kafka Consumer):
   - Listens to Kafka topics for events
   - Projects events into **MongoDB** (denormalized read models)
   - Updates chat room views, message collections, etc.

2. Client queries data via `GET /query/*` endpoints
   - Reads directly from MongoDB (fast, optimized for queries)

### **Real-time Updates**

1. **WebSocket Service** (Kafka Consumer):
   - Consumes events from Kafka
   - Pushes updates to connected clients via STOMP topics
   - Updates Redis for online status tracking

### **Event Flow Example: Send Message**

```
┌─────────┐     POST /command/message      ┌──────────────────┐
│ Client  │ ───────────────────────────>   │ Command Service  │
└─────────┘                                └──────────────────┘
                                                    │
                                                    │ 1. Write to PostgreSQL
                                                    ▼
                                           ┌──────────────────┐
                                           │   PostgreSQL     │
                                           │  (Source of      │
                                           │   Truth)         │
                                           └──────────────────┘
                                                    │
                                                    │ 2. Publish Event
                                                    ▼
                                           ┌──────────────────┐
                                           │  Kafka Topic:    │
                                           │ MessageSentEvent │
                                           └──────────────────┘
                                                    │
                                          ┌─────────────────────┐
                                          │                     │ 
                                          ▼                     ▼ 
                               ┌──────────────────┐  ┌──────────────────┐ 
                               │  Query Service   │  │ WebSocket Service│ 
                               │                  │  │                  │ 
                               │ 3. Update MongoDB│  │ 4. Push to WS    │ 
                               │    (Read Model)  │  │    Clients       │ 
                               └──────────────────┘  └──────────────────┘ 
                                          │                     │
                                          │                     │
                                          ▼                     ▼
                               ┌──────────────────┐  ┌──────────────────┐
                               │    MongoDB       │  │  Connected       │
                               │  (Optimized for  │  │  WebSocket       │
                               │   Queries)       │  │  Clients         │
                               └──────────────────┘  └──────────────────┘
```

### **Benefits of CQRS**

✅ **Scalability**: Read and write sides can scale independently
✅ **Performance**: Read models optimized for specific query patterns
✅ **Flexibility**: Different data stores for different needs (PostgreSQL for consistency, MongoDB for performance)
✅ **Resilience**: Asynchronous processing via Kafka ensures loose coupling
✅ **Audit Trail**: All changes captured as events in Kafka

---

## **🐛 Troubleshooting**

### **JWT Authentication Issues**

**Problem**: `401 Unauthorized` on protected endpoints

**Solutions**:
- Ensure you've run `python key.py` to generate RSA keys
- Verify JWT cookie is being set after login
- Check cookie is included in subsequent requests
- Verify API Gateway can read `public_key.pem`

### **Kafka Connection Issues**

**Problem**: Events not being processed, read models not updating

**Solutions**:
- Wait 1-2 minutes after `docker-compose up` for Kafka to initialize
- Check Kafka broker logs: `docker logs final_soa-kafka-1`
- Verify Kafka topics exist: `docker exec -it final_soa-kafka-1 kafka-topics --list --bootstrap-server localhost:9092`

### **Database Connection Issues**

**Problem**: Services failing to connect to PostgreSQL/MongoDB

**Solutions**:
- Ensure databases are fully started: `docker ps`
- Check database logs for errors
- Verify connection strings in `application.properties` files

---

## **📚 Additional Resources**

- **OpenAPI Specification**: See `ApiGateway/app/openapi.yaml` for complete API documentation
- **Database Access**:
  - PostgreSQL: http://localhost:8089 (Adminer)
  - MongoDB: http://localhost:8090 (Mongo Express)
- **Service Ports** (internal):
  - WebSocket: 8082
  - Auth Service: 8083
  - Customer Service: 8084
  - Chat Command: 8085
  - Chat Query: 8086
  - OTP Service: 8087
  - Mail Service: 8088

---

## **📝 License**

This project is developed for educational purposes as part of a Service-Oriented Architecture course.