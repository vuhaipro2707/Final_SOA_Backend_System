# **Chat Application Backend (CQRS/SOA/Event-Driven Architecture)**

This repository contains the backend implementation for a real-time chat application leveraging **Command Query Responsibility Segregation (CQRS)**, built on a Service-Oriented Architecture (SOA), and utilizing event-driven principles with Kafka.

The application uses multiple specialized microservices to handle authentication, user management, command processing (writes), query processing (reads), and real-time WebSocket communication.

## **🚀 Architecture Overview**

The system is structured around an API Gateway, several Spring Boot microservices (Java 21), PostgreSQL, MongoDB, Redis, and Apache Kafka.

| Component | Technology / Language | Role |
| :---- | :---- | :---- |
| **API Gateway** | Python (FastAPI) | Single entry point, JWT validation, and intelligent routing to backend services. |
| **Auth Service** | Spring Boot (Java) \+ PostgreSQL | Handles customer sign-in/out, manages Customer entities, and issues RSA-signed JWTs. |
| **Customer Management Service** | Spring Boot (Java) \+ PostgreSQL | Manages customer profile information (read/write operations on the Customer entity). |
| **Chat Command Service** | Spring Boot (Java) \+ PostgreSQL \+ Kafka Producer | Handles all **write** operations (sending messages, creating rooms, updating read markers). Publishes events (e.g., MessageSentEvent) to Kafka. |
| **Chat Query Service** | Spring Boot (Java) \+ MongoDB \+ Redis \+ Kafka Consumer | Handles all **read** operations (fetching rooms, messages, read markers). Consumes events from Kafka to maintain read-optimized projections in MongoDB (CQRS pattern). Uses Redis for real-time online status checks. |
| **WebSocket Service** | Spring Boot (Java) \+ WebSockets/STOMP \+ Redis \+ Kafka Consumer | Manages real-time connections, handles typing indicators, and pushes real-time updates (messages, online status, read markers) to connected users by consuming Kafka events. |
| **Nginx** | Reverse Proxy | Routes external HTTP traffic to the API Gateway and WebSocket traffic to the WebSocket Service. |
| **Datastores** | PostgreSQL, MongoDB, Redis, Kafka | Persistent data, read models, caching, and inter-service communication. |

## **🛠️ Local Development Setup**

The entire environment is containerized using Docker Compose.

### **Prerequisites**

1. Docker and Docker Compose installed.  
2. Python 3.x (required only for generating asymmetric keys).

### **1\. Generate Asymmetric Keys**

The system uses RSA asymmetric keys for JWT signing and verification. The private key is for the **Auth Service** (signing), and the public key is shared with the **API Gateway** and **WebSocket Service** (verification).

Run the provided Python script to generate the necessary PEM files:

python key.py

This command will create:

* auth-service/src/main/resources/keys/private\_key.pem  
* ApiGateway/app/public\_key.pem  
* websocket-service/src/main/resources/keys/public\_key.pem

***Note: The key.py script is essential for the services to authenticate correctly.***

### **2\. Build and Start Services**

The docker-compose.yml file defines the full production-like environment (including Kafka, databases, and UI tools).

\# Build the Docker images and start all containers in detached mode  
docker-compose up \--build \-d

Allow about 1-2 minutes for all services (especially Kafka and the Spring Boot apps) to fully initialize.

### **3\. Accessing the Application**

| Component | URL | Purpose |
| :---- | :---- | :---- |
| **Frontend** | http://localhost:5500 (Assumes you are running index.html via a live server on this port) | The main chat application UI. |
| **Nginx Gateway** | http://localhost:8080 | All external API calls (REST & WS handshake) go here. |
| **Mongo Express UI** | http://localhost:8087 | MongoDB visual interface. |
| **Adminer UI** | http://localhost:8088 | PostgreSQL visual interface. |

## **🔑 Authentication and Test Users**

### **Test Credentials (Created by Auth Service/DataInitializer)**

| Username | Password | Customer ID |
| :---- | :---- | :---- |
| user1 | 123 | 1 |
| user2 | 1234 | 2 |
| user3 | 12345 | 3 |

### **Login Flow**

1. Client submits credentials to POST /auth/login via Nginx/API Gateway.  
2. **Auth Service** authenticates, generates a JWT, and sets it as an HttpOnly cookie (jwt\_token).  
3. For subsequent requests, the **API Gateway** extracts the jwt\_token cookie, validates it using the public key, extracts the customerId, and forwards it to the upstream service via the X-Customer-Id header.  
4. The backend services (Customer, Command, Query) use the custom GatewayAuthFilter to reconstruct the Authentication context from this header.

## **🗺️ API Endpoints and WebSocket Topics**

The system exposes both REST API endpoints (via the Nginx/API Gateway on port 8080\) and STOMP-over-WebSocket topics (via Nginx/WebSocket Service on /ws/chat).

### **1\. REST API Endpoints (HTTP Requests via http://localhost:8080/)**

| Service | Method | Path | Description |
| :---- | :---- | :---- | :---- |
| **Auth** | POST | /auth/login | Authenticate and set JWT cookie. (Public) |
| **Auth** | POST | /auth/logout | Clear JWT cookie. (Public) |
| **Customer** | GET | /customer/info | Get authenticated user's profile. |
| **Customer** | POST | /customer/info | Update authenticated user's profile (fullName, email, phoneNumber, avatarColor). |
| **Customer (Read)** | GET | /customer/fullName/customerId/{id} | Get a customer's full name (used internally by chat services). |
| **Customer (Search)** | GET | /customer/info/phoneNumber/{number} | Search customers by phone number (for creating rooms). |
| **Query** | GET | /query/rooms | Get list of rooms for the authenticated user. |
| **Command** | POST | /command/room | Create a new chat room. |
| **Query** | GET | /query/message/roomId/{id} | Get the latest 20 messages for a room. |
| **Query (Pagination)** | GET | /query/message/roomId/{id}/index/{indexId} | Get the next batch of older messages. |
| **Command** | POST | /command/message | Send a new message. |
| **Query** | GET | /query/onlineStatus/roomId/{id} | Get online status of all participants in a room. |
| **Query** | GET | /query/readMarkers/roomId/{id} | Get all read markers for a room. |
| **Command** | POST | /command/read | Update the user's last read message ID (marks as read). |

### **2\. STOMP WebSocket Topics and Destinations**

The WebSocket connection is established at ws://localhost:8080/ws/chat.

| Type | Destination/Topic | Purpose |
| :---- | :---- | :---- |
| **Subscription (Private)** | /user/topic/rooms | Receive **updated room views** (including last message and unread status) relevant to the user. |
| **Subscription (Private)** | /user/topic/readStatus | Receive granular updates on the room's global unread status for the current user. |
| **Subscription (Public)** | /topic/message/roomId/{id} | Receive new **messages** in real-time for the specific room. |
| **Subscription (Public)** | /topic/onlineStatus/roomId/{id} | Receive real-time **online/offline status** changes for all participants in the room. |
| **Subscription (Public)** | /topic/readMarkers/roomId/{id} | Receive real-time **read marker** updates (who read up to which message) for the specific room. |
| **Subscription (Public)** | /topic/typing/roomId/{id} | Receive real-time **typing/stop-typing indicators** for the specific room. |
| **Message Mapping (Send)** | /app/extendOnline | Command to **extend the user's online presence** (TTL in Redis). |
| **Message Mapping (Send)** | /app/typing | Command to send a **typing or stop-typing indicator** to the room. |

## **🔄 CQRS and Event Flow**

The chat core adheres to a Command Query Responsibility Segregation (CQRS) and Event Sourcing pattern:

1. **Command (Write):** A user sends a message (POST /command/message).  
   * **Chat Command Service** writes the message to PostgreSQL (Source of Truth).  
   * It publishes a MessageSentEvent to the chat-message-sent Kafka topic.  
2. **Projection (Read Model Update):**  
   * **Chat Query Service** consumes the MessageSentEvent.  
   * It projects the new message into the MongoDB messages collection and updates the corresponding chatRoomViews document (updating lastMessage and updatedAt).  
3. **Real-Time Update:**  
   * The updated ChatRoomView is published by the **Chat Query Service** as a RoomUpdatedEvent to Kafka.  
   * **WebSocket Service** consumes this RoomUpdatedEvent and pushes the updated room data to all relevant users' private /user/topic/rooms WebSocket destinations.  
   * The original MessageSentEvent is also pushed by the **WebSocket Service** directly to the room's public topic (/topic/message/roomId/{id}).