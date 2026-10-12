package cl.duoc.sumativa2.bff.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP minimo del JDK que simula una instancia de un microservicio (ms-backend,
 * producer-service o auth-service) y registra las solicitudes recibidas.
 */
public final class ServidorHttpFalso {

    public record Solicitud(String metodo, String ruta, Map<String, List<String>> cabeceras, String cuerpo) {

        public String cabecera(String nombre) {
            return cabeceras.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(nombre))
                    .map(e -> e.getValue().get(0))
                    .findFirst().orElse(null);
        }
    }

    public record Respuesta(int status, String json) {
    }

    private final HttpServer server;
    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();
    private volatile Function<Solicitud, Respuesta> responder = s -> new Respuesta(200, "{}");

    public ServidorHttpFalso() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", this::atender);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private void atender(HttpExchange ex) throws IOException {
        String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Solicitud s = new Solicitud(ex.getRequestMethod(), ex.getRequestURI().getRawPath(),
                Map.copyOf(ex.getRequestHeaders()), cuerpo);
        solicitudes.add(s);
        Respuesta r = responder.apply(s);
        byte[] bytes = r.json().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(r.status(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
        ex.close();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void responder(Function<Solicitud, Respuesta> responder) {
        this.responder = responder;
    }

    public List<Solicitud> solicitudes() {
        return solicitudes;
    }

    public void limpiar() {
        solicitudes.clear();
    }
}
