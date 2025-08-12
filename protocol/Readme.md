# μLearn Protocol Server

## Overview

The μLearn Protocol Server is a Spring Boot application that serves as the primary back-end for the μLearn ecosystem. It is responsible for handling business logic, managing data, and interacting with other services, such as the ACA-Py client for Self-Sovereign Identity (SSI) operations.

## Key Features

*   **DID Resolution:** Delegates DID resolution to the ACA-Py client, providing a unified interface for resolving various DID methods.
*   **Messaging:** (Future) Will handle secure messaging between participants in the μLearn ecosystem.
*   **Credential Management:** (Future) Will manage the issuance, verification, and revocation of Verifiable Credentials (VCs).

## Setup and Configuration

### Prerequisites

*   Java 17
*   Maven
*   Docker
*   Docker Compose

### Configuration

The protocol server is configured through the `application.properties` file located in `src/main/resources`. The following properties are required:

*   `acapy.admin.url`: The URL of the ACA-Py admin API (e.g., `http://localhost:8031`).
*   `acapy.admin.api-key`: The API key for the ACA-Py admin API.

### Running the Server

1.  **Navigate to the `Framework/protocol` directory:**
    ```bash
    cd Framework/protocol
    ```

2.  **Build the project:**
    ```bash
    mvn clean install
    ```

3.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

## Testing

The protocol server includes a suite of unit and integration tests.

### Running the Tests

1.  **Ensure the ACA-Py service is running.** Refer to the `Framework/ssi/Readme.md` for instructions on how to start the ACA-Py service.

2.  **Navigate to the `Framework/protocol` directory:**
    ```bash
    cd Framework/protocol
    ```

3.  **Run the tests:**
    ```bash
    mvn test
    ```

The tests will verify the connection to the ACA-Py service and the functionality of the DID resolver.