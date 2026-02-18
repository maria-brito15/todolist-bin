package logic;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Launcher class for the application.
 * Responsibility: Setup environment, start backend, and open frontend.
 */
public class Main {

    public static void main(String[] args) {
        // Ensure the 'files' directory exists for data storage
        new File("files").mkdirs();

        // Start the server in a separate Daemon thread so it doesn't 
        // block the main thread and closes when the app stops
        Thread serverThread = new Thread(() -> {
            try {
                Server.main(new String[]{});
            } catch (IOException e) {
                System.err.println("Erro ao iniciar servidor: " + e.getMessage());
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        System.out.println("Iniciando servidor...");
        // Brief pause to allow the server socket to bind
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        String url = "http://localhost:8080/view/index.html";

        try {
            // Attempt to open the default system browser
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback: Manually trigger OS-specific commands to open the URL
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if      (os.contains("linux")) pb = new ProcessBuilder("xdg-open", url);
                else if (os.contains("mac"))   pb = new ProcessBuilder("open", url);
                else                           pb = new ProcessBuilder("cmd", "/c", "start", url);
                pb.start();
            }
            System.out.println("Aplicação aberta em: " + url);
        } catch (Exception e) {
            System.out.println("Abra manualmente: " + url);
        }

        System.out.println("Pressione Ctrl+C para encerrar.");
        // Keep the main thread alive while the server runs
        try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
    }
}