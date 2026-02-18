# To-Do List Application

## Project Description
This is a simple To-Do List application developed in Java with a web-based frontend. It allows users to register, log in, and manage their tasks, including adding, listing, editing, marking as complete/incomplete, and deleting tasks. The application utilizes a custom binary file-based persistence mechanism for storing user and task data.

## Features
*   User Registration and Login.
*   Add new tasks with description and due date.
*   List all tasks for a user.
*   Filter tasks by status (all, pending, completed).
*   Mark tasks as complete or incomplete.
*   Edit task descriptions.
*   Delete tasks.
*   Data persistence using binary files.

## Technologies Used
*   **Backend**: Java (JDK 11+)
    *   `com.sun.net.httpserver`: For creating a lightweight HTTP server.
*   **Frontend**: HTML, CSS, JavaScript
    *   Responsive design for various screen sizes.
    *   Dynamic content updates without page reloads.

## Architecture
The application follows a client-server architecture:

*   **Server (Java)**: Handles all business logic, API requests, and data persistence. It serves the static frontend files (HTML, CSS, JS) and exposes REST-like API endpoints for user authentication and task management.
*   **Client (Web Browser)**: The frontend is a single-page application (SPA) built with HTML, CSS, and JavaScript. It interacts with the Java backend via HTTP requests to perform operations.

### Data Persistence
User and task data are stored in custom binary files (`files/usuarios.bin` and `files/tarefas.bin`). The `DAO` (Data Access Object) classes (`UsuarioDAO.java` and `TarefaDAO.java`) manage reading from and writing to these binary files directly. This approach provides a lightweight persistence solution without relying on external databases.

## Setup and Installation

### Prerequisites
*   Java Development Kit (JDK) 11 or higher.

### Steps
1.  **Clone the repository** (or extract the provided archive):
    ```bash
    # Assuming the project is extracted to /home/ubuntu/project/memoria-secundaria-todo-list
    cd /home/ubuntu/project/memoria-secundaria-todo-list
    ```
2.  **Compile the Java source code**:
    ```bash
    javac -d out logic/**/*.java
    ```
3.  **Run the application**:
    ```bash
    java -cp out logic.Main
    ```
    This will start the Java backend server and attempt to open the web frontend in your default browser at `http://localhost:8080/view/index.html`.

## Usage

1.  **Access the application**: Open your web browser and navigate to `http://localhost:8080/view/index.html`.
2.  **Register/Login**: On the initial screen, you can either register a new account or log in with existing credentials.
3.  **Manage Tasks**: After logging in, you can:
    *   Add new tasks by entering a description and an optional due date.
    *   View your tasks, filter them by status.
    *   Click on a task to mark it as complete/incomplete.
    *   Edit a task's description.
    *   Delete a task.

## API Endpoints
The backend server exposes the following API endpoints:

| Endpoint                | Method | Purpose                            |
| :---------------------- | :----- | :--------------------------------- |
| `/api/register`         | `POST` | Create a new user account          |
| `/api/login`            | `POST` | Authenticate an existing user      |
| `/api/tarefas`          | `GET`  | List all tasks for a specific user |
| `/api/tarefas`          | `POST` | Create a new task                  |
| `/api/tarefas/concluir` | `POST` | Toggle a task's completion status  |
| `/api/tarefas/editar`   | `POST` | Update the description of a task   |
| `/api/tarefas/deletar`  | `POST` | Logically delete a task            |                                                                                                                                          