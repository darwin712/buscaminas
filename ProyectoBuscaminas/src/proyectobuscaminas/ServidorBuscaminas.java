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
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("--- SERVIDOR BUSCAMINAS (MODO CONTROLADOR) ---");
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = serverSocket.accept();
                poolHilos.execute(new ManejadorCliente(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void intentarEmparejar() {
        List<ManejadorCliente> listos = new ArrayList<>();
        synchronized(clientesEnEspera) {
            for(ManejadorCliente c : clientesEnEspera) {
                if(c.estaLogueado && !c.socket.isClosed()) listos.add(c);
            }
            
            if (listos.size() >= 2) {
                ManejadorCliente j1 = listos.get(0);
                ManejadorCliente j2 = listos.get(1);
                clientesEnEspera.remove(j1);
                clientesEnEspera.remove(j2);
                
                // Crear partida y asignarla a los jugadores
                Partida nuevaPartida = new Partida(j1, j2);
                j1.setPartidaActual(nuevaPartida);
                j2.setPartidaActual(nuevaPartida);
                
                System.out.println("Partida iniciada: " + j1.nombreUsuario + " vs " + j2.nombreUsuario);
                poolHilos.execute(nuevaPartida); // Inicia la lógica de envío de tableros
            }
        }
    }

    // CLASE PARTIDA: Ahora actúa como un controlador sincronizado, no lee directamente.
    static class Partida implements Runnable {
        private final ManejadorCliente j1;
        private final ManejadorCliente j2;
        private boolean turnoJ1 = true; // true = turno de j1
        private Tablero[] tableros;

        public Partida(ManejadorCliente j1, ManejadorCliente j2) {
            this.j1 = j1;
            this.j2 = j2;
        }

        @Override
        public void run() {
            // Fase de inicialización
            tableros = generador.generarTableros();
            try {
                j1.enviarMensaje(new Mensaje("INICIO", tableros[0]));
                j2.enviarMensaje(new Mensaje("INICIO", tableros[1]));
                notificarTurnos();
            } catch(Exception e) {
                finalizarPorDesconexion();
            }
        }

        // Método centralizado para procesar jugadas
        public synchronized void procesarJugada(ManejadorCliente jugador, Mensaje jugada) {
            try {
                // Validar turno
                boolean esJ1 = (jugador == j1);
                if ((esJ1 && !turnoJ1) || (!esJ1 && turnoJ1)) {
                    System.out.println("Jugada ignorada: No es el turno de " + jugador.nombreUsuario);
                    return; 
                }

                ManejadorCliente oponente = esJ1 ? j2 : j1;
                Tablero tableroOponente = esJ1 ? tableros[1] : tableros[0];

                if (jugada.getTipo().equals("CLICK")) {
                    String[] coords = ((String) jugada.getContenido()).split(",");
                    int f = Integer.parseInt(coords[0]);
                    int c = Integer.parseInt(coords[1]);

                    // Lógica del juego
                    Casilla cas = tableroOponente.getCasilla(f, c);
                    cas.setRevelado(true);

                    // Notificar al oponente
                    oponente.enviarMensaje(new Mensaje("CASILLA_ACTUALIZADA", f + "," + c + "," + cas.getVecinos()));

                    // Cambio de turno
                    turnoJ1 = !turnoJ1;
                    notificarTurnos();

                } else if (jugada.getTipo().equals("PERDIO")) {
                    // El jugador actual pisó una mina
                    terminarPartida(oponente, jugador, "El oponente pisó una mina");
                } else if (jugada.getTipo().equals("GANO")) {
                    // El jugador actual completó el tablero
                    terminarPartida(jugador, oponente, "El oponente completó el nivel");
                }
            } catch (Exception e) {
                finalizarPorDesconexion();
            }
        }

        private void notificarTurnos() throws IOException {
            j1.enviarMensaje(new Mensaje("TURNO", turnoJ1));
            j2.enviarMensaje(new Mensaje("TURNO", !turnoJ1));
        }

        private void terminarPartida(ManejadorCliente ganador, ManejadorCliente perdedor, String razon) {
            try {
                ganador.enviarMensaje(new Mensaje("GANASTE", razon));
                // IMPORTANTE: Avisar al que perdió también
                perdedor.enviarMensaje(new Mensaje("PERDISTE", "Has perdido"));
                
                persistencia.guardarResultado(ganador.nombreUsuario, perdedor.nombreUsuario, "Normal", "00:00");
                
                // Liberar jugadores para nueva partida
                j1.setPartidaActual(null);
                j2.setPartidaActual(null);
            } catch(Exception e) { e.printStackTrace(); }
        }

        public void finalizarPorDesconexion() {
            try {
                if (!j1.socket.isClosed()) j1.enviarMensaje(new Mensaje("OPONENTE_DESCONECTADO", null));
                if (!j2.socket.isClosed()) j2.enviarMensaje(new Mensaje("OPONENTE_DESCONECTADO", null));
            } catch(Exception e) {}
            j1.setPartidaActual(null);
            j2.setPartidaActual(null);
        }
    }

    // Manejador del Cliente
    static class ManejadorCliente implements Runnable {
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;
        String nombreUsuario;
        boolean estaLogueado = false;
        Partida partidaActual = null; // Referencia a la partida activa

        public ManejadorCliente(Socket s) {
            this.socket = s;
            try {
                // ORDEN CRÍTICO: OutputStream PRIMERO en el servidor
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush(); 
                in = new ObjectInputStream(socket.getInputStream());
            } catch(IOException e) { e.printStackTrace(); }
        }
        
        public void setPartidaActual(Partida p) {
            this.partidaActual = p;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Mensaje msj = (Mensaje) in.readObject();
                    
                    // Si estamos en partida, delegamos el mensaje
                    if (partidaActual != null && (msj.getTipo().equals("CLICK") || msj.getTipo().equals("PERDIO") || msj.getTipo().equals("GANO"))) {
                        partidaActual.procesarJugada(this, msj);
                        continue; // Saltamos el resto de lógica
                    }
                    
                    // Lógica de Lobby
                    if (msj.getTipo().equals("LOGIN")) {
                        String[] datos = ((String) msj.getContenido()).split(":");
                        Jugador j = persistencia.login(datos[0], datos[1]);
                        if (j != null) {
                            nombreUsuario = j.getNombre();
                            estaLogueado = true;
                            enviarMensaje(new Mensaje("LOGIN_OK", "Hola " + nombreUsuario));
                        } else {
                            enviarMensaje(new Mensaje("ERROR", "Datos incorrectos"));
                        }
                    } 
                    else if (msj.getTipo().equals("BUSCAR_PARTIDA") && estaLogueado) {
                        if (!clientesEnEspera.contains(this)) {
                            clientesEnEspera.add(this);
                            System.out.println(nombreUsuario + " busca partida.");
                            intentarEmparejar();
                        }
                    }
                }
            } catch (Exception e) {
                if(partidaActual != null) partidaActual.finalizarPorDesconexion();
                clientesEnEspera.remove(this);
            }
        }
        
        public synchronized void enviarMensaje(Mensaje m) throws IOException {
            out.writeObject(m);
            out.flush();
            out.reset(); // CRÍTICO: Resetear para evitar corrupción
        }
    }
}