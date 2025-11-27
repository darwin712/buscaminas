package proyectobuscaminas;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorBuscaminas {
    private static final int PUERTO = 12345;
    private static final List<ManejadorCliente> clientesEnEspera = Collections.synchronizedList(new ArrayList<>());
    
    private static final GeneradorTableros generador = new GeneradorTableros();
    private static final GestorPersistencia persistencia = new GestorPersistencia();
    
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("--- SERVIDOR MAESTRO ACTIVO ---");
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = serverSocket.accept();
                pool.execute(new ManejadorCliente(socket));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void intentarEmparejar() {
        synchronized(clientesEnEspera) {
            clientesEnEspera.removeIf(c -> c.socket.isClosed() || !c.conectado);
            if (clientesEnEspera.size() >= 2) {
                ManejadorCliente j1 = clientesEnEspera.remove(0);
                ManejadorCliente j2 = clientesEnEspera.remove(0);
                
                Partida partida = new Partida(j1, j2);
                j1.setPartidaActual(partida);
                j2.setPartidaActual(partida);
                pool.execute(partida);
            }
        }
    }

    // =========================================================================
    // CLASE PARTIDA
    // =========================================================================
    static class Partida implements Runnable {
        private final ManejadorCliente j1;
        private final ManejadorCliente j2;
        private String voto1, voto2, dificultad;
        private boolean turnoJ1 = true; 
        private Tablero[] tableros;
        private volatile boolean activa = true;
        private long tiempoInicio;

        public Partida(ManejadorCliente j1, ManejadorCliente j2) {
            this.j1 = j1;
            this.j2 = j2;
        }

        public synchronized void recibirVoto(ManejadorCliente j, String voto) {
            if (j == j1) voto1 = voto; else voto2 = voto;
            notifyAll();
        }

        @Override
        public void run() {
            try {
                // Enviar nombres de rivales
                j1.enviar(new Mensaje("RIVAL_ENCONTRADO", j2.nombre));
                j2.enviar(new Mensaje("RIVAL_ENCONTRADO", j1.nombre));
                
                Thread.sleep(500);
                broadcast(new Mensaje("VOTACION", null));
                
                synchronized(this) { while (voto1 == null || voto2 == null) wait(); }
                
                dificultad = voto1.equals(voto2) ? voto1 : (Math.random() < 0.5 ? voto1 : voto2);
                tableros = generador.generarTableros(dificultad);
                
                broadcast(new Mensaje("TAMANO", dificultad));
                j1.enviar(new Mensaje("INICIO", tableros[0]));
                j2.enviar(new Mensaje("INICIO", tableros[1]));
                
                tiempoInicio = System.currentTimeMillis();
                actualizarTurnos();
            } catch (Exception e) { finalizarPorError(); }
        }

        public synchronized void procesarAccion(ManejadorCliente jugador, Mensaje msg) {
            if (!activa) return;
            try {
                boolean esJ1 = (jugador == j1);
                if ((esJ1 && !turnoJ1) || (!esJ1 && turnoJ1)) return;

                Tablero tActual = esJ1 ? tableros[0] : tableros[1];
                
                if (msg.getTipo().equals("CLICK")) {
                    String[] c = ((String)msg.getContenido()).split(",");
                    revelarRecursivo(tActual, Integer.parseInt(c[0]), Integer.parseInt(c[1]), jugador);
                    turnoJ1 = !turnoJ1;
                    actualizarTurnos();
                } else if (msg.getTipo().equals("PERDIO")) {
                    terminarPartida(esJ1 ? j2 : j1, jugador, "El rival pisó una mina.");
                }
            } catch (Exception e) { finalizarPorError(); }
        }

        private void revelarRecursivo(Tablero t, int f, int c, ManejadorCliente j) throws IOException {
            if (f < 0 || f >= t.getFilas() || c < 0 || c >= t.getColumnas()) return;
            
            Casilla cas = t.getCasilla(f, c);
            if (cas.esRevelado() || cas.isMina()) return;

            cas.setRevelado(true);
            j.enviar(new Mensaje("CASILLA_ACTUALIZADA", f + "," + c + "," + cas.getVecinos()));

            if (cas.getVecinos() == 0) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (x != 0 || y != 0) revelarRecursivo(t, f + x, c + y, j);
                    }
                }
            }
        }

        private void actualizarTurnos() throws IOException {
            j1.enviar(new Mensaje("TURNO", turnoJ1));
            j2.enviar(new Mensaje("TURNO", !turnoJ1));
        }

        private void terminarPartida(ManejadorCliente g, ManejadorCliente p, String r) {
            activa = false;
            try {
                long duracion = System.currentTimeMillis() - tiempoInicio;
                long seg = (duracion / 1000) % 60;
                long min = (duracion / 60000);
                String tiempoFinal = String.format("%02d:%02d", min, seg);

                enviarMinas(j1, tableros[0]); enviarMinas(j2, tableros[1]);
                Thread.sleep(200);
                
                g.enviar(new Mensaje("GANASTE", r));
                p.enviar(new Mensaje("PERDISTE", "Has perdido."));
                
                persistencia.guardarResultado(g.nombre, p.nombre, "Tablero " + dificultad, tiempoFinal);
            } catch(Exception e){} finally { limpiar(); }
        }
        
        private void enviarMinas(ManejadorCliente j, Tablero t) throws IOException {
             StringBuilder sb = new StringBuilder();
             for(int i=0; i<t.getFilas(); i++) for(int k=0; k<t.getColumnas(); k++)
                 if(t.getCasilla(i, k).isMina()) sb.append(i).append(",").append(k).append(";");
             j.enviar(new Mensaje("MINAS", sb.toString()));
        }

        private void broadcast(Mensaje m) throws IOException {
            j1.enviar(m); j2.enviar(m);
        }

        private void finalizarPorError() {
            if(!activa) return;
            activa = false;
            try { broadcast(new Mensaje("OPONENTE_DESCONECTADO", null)); } catch(Exception e){}
            limpiar();
        }

        private void limpiar() {
            j1.setPartidaActual(null); j2.setPartidaActual(null);
        }
    }

    // =========================================================================
    // CLASE MANEJADOR CLIENTE
    // =========================================================================
    static class ManejadorCliente implements Runnable {
        Socket socket; ObjectOutputStream out; ObjectInputStream in;
        String nombre = "Anónimo"; boolean logueado = false; boolean conectado = true;
        Partida partidaActual = null;

        public ManejadorCliente(Socket s) { this.socket = s; }
        public void setPartidaActual(Partida p) { this.partidaActual = p; }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                out.flush();
                in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                while (conectado) {
                    Mensaje msg = (Mensaje) in.readObject();
                    
                    if (partidaActual != null) {
                         String t = msg.getTipo();
                         if (t.equals("CLICK") || t.equals("PERDIO")) { partidaActual.procesarAccion(this, msg); continue; }
                         if (t.equals("ENVIAR_VOTO")) { partidaActual.recibirVoto(this, (String)msg.getContenido()); continue; }
                    }
                    
                    procesarMenu(msg);
                }
            } catch (Exception e) { 
                // Desconexión
            } finally { 
                conectado = false; 
                clientesEnEspera.remove(this); 
                if(partidaActual!=null) partidaActual.finalizarPorError();
                try { socket.close(); } catch(IOException e){}
            }
        }
        
        private void procesarMenu(Mensaje msg) throws IOException {
            switch(msg.getTipo()) {
                case "LOGIN":
                    String[] c = ((String)msg.getContenido()).split(":");
                    Jugador j = persistencia.login(c[0], c[1]);
                    if (j != null) { nombre = j.getNombre(); logueado = true; enviar(new Mensaje("LOGIN_OK", nombre)); } 
                    else enviar(new Mensaje("ERROR", "Datos incorrectos"));
                    break;
                    
                case "REGISTRO":
                    String[] r = ((String)msg.getContenido()).split(":");
                    if(persistencia.registrarUsuario(r[0], r[1])) enviar(new Mensaje("REGISTRO_OK", "Éxito"));
                    else enviar(new Mensaje("ERROR", "Ya existe"));
                    break;
                    
                case "BUSCAR_PARTIDA":
                    if (!logueado) enviar(new Mensaje("ERROR", "Inicia sesión"));
                    else if (!clientesEnEspera.contains(this)) {
                        clientesEnEspera.add(this);
                        enviar(new Mensaje("BUSCANDO", null));
                        intentarEmparejar();
                    }
                    break;
                    
                // --- AQUÍ ESTÁ EL CAMBIO DEL FILTRO ---
                case "HISTORIAL":
                     // Obtenemos todo el historial
                     String historialCompleto = persistencia.obtenerHistorial();
                     StringBuilder sb = new StringBuilder();
                     
                     if (historialCompleto != null && !historialCompleto.isEmpty()) {
                         String[] lineas = historialCompleto.split("\n");
                         // Recorremos línea por línea buscando el nombre de ESTE usuario
                         for (String linea : lineas) {
                             if (linea.contains(this.nombre)) {
                                 sb.append(linea).append("\n");
                             }
                         }
                     }
                     
                     // Si no encontró nada o el historial estaba vacío
                     String resultado = sb.length() > 0 ? sb.toString() : "No se encontraron partidas para: " + this.nombre;
                     enviar(new Mensaje("HISTORIAL_OK", resultado));
                     break;
            }
        }

        public synchronized void enviar(Mensaje m) throws IOException {
            if(!socket.isClosed()) { out.writeObject(m); out.flush(); out.reset(); }
        }
    }
}