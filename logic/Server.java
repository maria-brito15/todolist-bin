package logic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import logic.dao.TarefaDAO;
import logic.dao.UsuarioDAO;
import logic.model.Tarefa;
import logic.model.Usuario;

/**
 * Backend Server using Java's built-in HttpServer.
 * Routes HTTP requests to the appropriate DAO (Data Access Object) logic.
 */
public class Server {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Ensure the directory for binary files exists
        new File("files").mkdirs();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoint Mapping - connects URLs to Java methods
        server.createContext("/api/login",           Server::handleLogin);
        server.createContext("/api/register",        Server::handleRegister);
        server.createContext("/api/tarefas/concluir",Server::handleConcluir);
        server.createContext("/api/tarefas/editar",  Server::handleEditar);
        server.createContext("/api/tarefas/deletar", Server::handleDeletar);
        server.createContext("/api/tarefas",         Server::handleTarefas);

        // Fallback context for serving index.html, styles, and scripts
        server.createContext("/",                    Server::handleStatic);

        server.setExecutor(null); // Uses a default implementation
        server.start();
        System.out.println("Servidor rodando em http://localhost:" + PORT);
    }

    /**
     * Serves static files (HTML, CSS, JS). 
     * Translates URL paths to local file system paths.
     */
    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        File file = new File("." + path);
        if (!file.exists() || file.isDirectory()) {
            byte[] body = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
            return;
        }

        // Set MIME type so the browser knows how to process the file
        String mime = "text/plain";
        if (path.endsWith(".html")) mime = "text/html; charset=UTF-8";
        else if (path.endsWith(".css")) mime = "text/css";
        else if (path.endsWith(".js"))  mime = "application/javascript";

        byte[] bytes = Files.readAllBytes(file.toPath());
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /**
     * Authenticates a user.
     */
    private static void handleLogin(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, ""); return; }

        String body = readBody(ex);
        String email = parseJson(body, "email");
        String senha = parseJson(body, "senha");

        try {
            Usuario u = UsuarioDAO.login(email, senha);
            if (u == null) {
                send(ex, 401, "{\"erro\":\"email ou senha incorretos.\"}");
            } else {
                send(ex, 200, String.format(
                    "{\"id\":%d,\"nome\":\"%s\",\"email\":\"%s\"}",
                    u.id, esc(u.nome), esc(u.email)
                ));
            }
        } catch (Exception e) {
            send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * Registers a new user account.
     */
    private static void handleRegister(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, ""); return; }

        String body = readBody(ex);
        String nome  = parseJson(body, "nome");
        String email = parseJson(body, "email");
        String senha = parseJson(body, "senha");

        try {
            int id = UsuarioDAO.inserir(nome, email, senha);
            if (id == -1) {
                send(ex, 409, "{\"erro\":\"email já cadastrado.\"}");
            } else {
                send(ex, 200, String.format(
                    "{\"id\":%d,\"nome\":\"%s\",\"email\":\"%s\"}",
                    id, esc(nome), esc(email)
                ));
            }
        } catch (Exception e) {
            send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * GET: Lists tasks for a user.
     * POST: Creates a new task.
     */
    private static void handleTarefas(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;

        if (ex.getRequestMethod().equalsIgnoreCase("GET")) {
            String query = ex.getRequestURI().getQuery();
            int idUsuario = parseQueryInt(query, "idUsuario");

            try {
                List<Tarefa> lista = TarefaDAO.listarPorUsuario(idUsuario);
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < lista.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(tarefaToJson(lista.get(i)));
                }
                sb.append("]");
                send(ex, 200, sb.toString());
            } catch (Exception e) {
                send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
            }

        } else if (ex.getRequestMethod().equalsIgnoreCase("POST")) {
            String body      = readBody(ex);
            int    idUsuario = Integer.parseInt(parseJson(body, "idUsuario"));
            String descricao = parseJson(body, "descricao");
            int    dia       = Integer.parseInt(parseJson(body, "dia"));
            int    mes       = Integer.parseInt(parseJson(body, "mes"));
            int    ano       = Integer.parseInt(parseJson(body, "ano"));

            try {
                int id = TarefaDAO.inserir(idUsuario, descricao, dia, mes, ano);
                Tarefa t = TarefaDAO.buscarPorId(id);
                send(ex, 200, tarefaToJson(t));
            } catch (Exception e) {
                send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
            }
        } else {
            send(ex, 405, "");
        }
    }

    /**
     * Toggles a task between completed and pending.
     */
    private static void handleConcluir(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, ""); return; }

        String body = readBody(ex);
        int id = Integer.parseInt(parseJson(body, "id"));

        try {
            Tarefa t = TarefaDAO.buscarPorId(id);
            if (t == null) { send(ex, 404, "{\"erro\":\"tarefa não encontrada.\"}"); return; }
            if (t.concluida) TarefaDAO.desmarcarConcluida(id);
            else             TarefaDAO.marcarConcluida(id);
            send(ex, 200, "{\"ok\":true}");
        } catch (Exception e) {
            send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * Updates the description text of an existing task.
     */
    private static void handleEditar(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, ""); return; }

        String body      = readBody(ex);
        int    id        = Integer.parseInt(parseJson(body, "id"));
        String descricao = parseJson(body, "descricao");

        try {
            boolean ok = TarefaDAO.atualizarDescricao(id, descricao);
            send(ex, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"erro\":\"não encontrada.\"}");
        } catch (Exception e) {
            send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * Soft-deletes a task (marks as inactive in the binary file).
     */
    private static void handleDeletar(HttpExchange ex) throws IOException {
        if (handleOptions(ex)) return;
        if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { send(ex, 405, ""); return; }

        String body = readBody(ex);
        int id = Integer.parseInt(parseJson(body, "id"));

        try {
            boolean ok = TarefaDAO.deletar(id);
            send(ex, ok ? 200 : 404, ok ? "{\"ok\":true}" : "{\"erro\":\"não encontrada.\"}");
        } catch (Exception e) {
            send(ex, 500, "{\"erro\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles CORS Pre-flight. Crucial for web browsers to allow cross-origin requests.
     */
    private static boolean handleOptions(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            ex.sendResponseHeaders(204, -1);
            ex.getResponseBody().close();
            return true;
        }
        return false;
    }

    /**
     * Helper method to send a JSON response to the client.
     */
    private static void send(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().set("Content-Type",                 "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    /**
     * Reads the entire string from the request input stream.
     */
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Manual JSON parser logic to extract values by key.
     */
    private static String parseJson(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        idx += search.length();
        while (idx < json.length() && (json.charAt(idx) == ':' || json.charAt(idx) == ' ')) idx++;

        if (json.charAt(idx) == '"') {
            int start = idx + 1;
            int end   = json.indexOf('"', start);
            return json.substring(start, end);
        } else {
            int start = idx, end = start;
            while (end < json.length() && ",} \n\r\t".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    /**
     * Extracts an integer from the URL query string.
     */
    private static int parseQueryInt(String query, String key) {
        if (query == null) return 0;
        for (String part : query.split("&")) {
            String[] kv = part.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return Integer.parseInt(kv[1]);
        }
        return 0;
    }

    /**
     * Manual serialization of a Task object to a JSON string.
     */
    private static String tarefaToJson(Tarefa t) {
        return String.format(
            "{\"id\":%d,\"idUsuario\":%d,\"descricao\":\"%s\",\"concluida\":%b,\"ativo\":%b,\"dia\":%d,\"mes\":%d,\"ano\":%d}",
            t.id, t.idUsuario, esc(t.descricao), t.concluida, t.ativo, t.dia, t.mes, t.ano
        );
    }

    /**
     * Escapes backslashes and double quotes for safe JSON inclusion.
     */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}