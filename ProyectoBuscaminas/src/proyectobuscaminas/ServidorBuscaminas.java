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
    // Lista segura para hilos
    private static final List<ManejadorCliente> clientesEnEspera = Collections.synchronizedList(new ArrayList<>());
    
    // Tus clases auxiliares
    private static final GeneradorTableros generador = new GeneradorTableros();
    private static final GestorPersistencia persistencia = new GestorPersistencia();
    
    // Pool de hilos para no saturar
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("--- SERVIDOR BUSCAMINAS ACTIVO ---");
        System.out.println("Esperando jugadores en puerto: " + PUERTO);
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nueva conexión entrante: " + socket.getInetAddress());
                poolHilos.execute(new ManejadorCliente(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método sincronizado para emparejar jugadores
    private static void intentarEmparejar() {
        synchronized(clientesEnEspera) {
            // Filtramos solo los que están realmente conectados y logueados
            List<ManejadorCliente> listos = new ArrayList<>();
            for(ManejadorCliente c : clientesEnEspera) {
                if(c.estaLogueado && !c.socket.isClosed()) {
                    listos.add(c);
                }
            }

            // Si hay 2 o más, sacamos a los dos primeros
            if (listos.size() >= 2) {
                ManejadorCliente j1 = listos.get(0);
                ManejadorCliente j2 = listos.get(1);
                
                // Los quitamos de la sala de espera
                clientesEnEspera.remove(j1);
                clientesEnEspera.remove(j2);

                System.out.println(">>> ¡MATCH! " + j1.nombreUsuario + " vs " + j2.nombreUsuario);
                
                // Creamos la partida y la lanzamos
                Partida nuevaPartida = new Partida(j1, j2);
                j1.setPartidaActual(nuevaPartida);
                j2.setPartidaActual(nuevaPartida);
                
                // AVISO IMPORTANTE: Desbloquea al cliente del botón "Jugar"
                try {
                    j1.enviarMensaje(new Mensaje("RIVAL_ENCONTRADO", null));
                    j2.enviarMensaje(new Mensaje("RIVAL_ENCONTRADO", null));
                } catch(Exception e) { e.printStackTrace(); }

                poolHilos.execute(nuevaPartida);
            }
        }
    }

    // --- CLASE PARTIDA ---
    static class Partida implements Runnable {
        private final ManejadorCliente j1;
        private final ManejadorCliente j2;
        
        // Variables para manejar la votación asíncrona
        private String votoJ1 = null;
        private String votoJ2 = null;
        
        private boolean turnoJ1 = true; 
        private Tablero[] tableros;
        private String dificultadFinal;

        public Partida(ManejadorCliente j1, ManejadorCliente j2) {
            this.j1 = j1;
            this.j2 = j2;
        }
        
        public synchronized void recibirVoto(ManejadorCliente jugador, String voto) {
            if (jugador == j1) votoJ1 = voto;
            if (jugador == j2) votoJ2 = voto;
            
            System.out.println("Voto recibido de " + jugador.nombreUsuario + ": " + voto);
            
            // Si ya tenemos los dos votos, despertamos al hilo principal de la partida
            if (votoJ1 != null && votoJ2 != null) {
                notifyAll();
            }
        }

        @Override
        public void run() {
            try {
                // 1. FASE DE VOTACIÓN
                j1.enviarMensaje(new Mensaje("VOTACION", null));
                j2.enviarMensaje(new Mensaje("VOTACION", null));
                
                // Pausamos hasta recibir votos
                synchronized(this) {
                    while(votoJ1 == null || votoJ2 == null) {
                        wait(); 
                    }
                }
                
                // 2. CALCULAR RESULTADO DE VOTACIÓN
                if (votoJ1.equals(votoJ2)) {
                    dificultadFinal = votoJ1;
                    System.out.println("Acuerdo mutuo. Tamaño: " + dificultadFinal);
                } else {
                    dificultadFinal = Math.random() < 0.5 ? votoJ1 : votoJ2;
                    System.out.println("Desacuerdo. Ganó por azar: " + dificultadFinal);
                }
                
                // 3. GENERAR TABLEROS Y EMPEZAR 
                // Genera tableros distintos (si actualizaste GeneradorTableros)
                tableros = generador.generarTableros(dificultadFinal);

                // Enviar configuración inicial
                j1.enviarMensaje(new Mensaje("TAMANO", dificultadFinal));
                j2.enviarMensaje(new Mensaje("TAMANO", dificultadFinal));

                j1.enviarMensaje(new Mensaje("INICIO", tableros[0]));
                j2.enviarMensaje(new Mensaje("INICIO", tableros[1]));
                
                // Iniciar turnos
                notificarTurnos();
                
            } catch(Exception e) {
                System.err.println("Error iniciando partida: " + e.getMessage());
                finalizarPorDesconexion();
            }
        }

        public synchronized void procesarJugada(ManejadorCliente jugador, Mensaje jugada) {
            try {
                boolean esJ1 = (jugador == j1);
                
                // Validar turno
                if ((esJ1 && !turnoJ1) || (!esJ1 && turnoJ1)) return; 

                ManejadorCliente jugadorActual = jugador;
                ManejadorCliente oponente = esJ1 ? j2 : j1;
                Tablero tableroActual = esJ1 ? tableros[0] : tableros[1];
                String tipo = jugada.getTipo();

                if (tipo.equals("CLICK")) {
                    String contenido = (String) jugada.getContenido();
                    String[] coords = contenido.split(",");
                    int f = Integer.parseInt(coords[0]);
                    int c = Integer.parseInt(coords[1]);

                    Casilla cas = tableroActual.getCasilla(f, c);
                    cas.setRevelado(true);

                    // Enviamos casilla actualizada
                    jugadorActual.enviarMensaje(new Mensaje("CASILLA_ACTUALIZADA", f + "," + c + "," + cas.getVecinos()));
                    
                    // Cambio de turno
                    turnoJ1 = !turnoJ1;
                    notificarTurnos();

                } else if (tipo.equals("PERDIO")) {
                    terminarPartida(oponente, jugadorActual, "El oponente piso una mina");
                } else if (tipo.equals("GANO")) {
                    terminarPartida(jugadorActual, oponente, "Has completado el nivel");
                }
            } catch (Exception e) {
                finalizarPorDesconexion();
            }
        }

        private void notificarTurnos() throws IOException {
            j1.enviarMensaje(new Mensaje("TURNO", turnoJ1));
            j2.enviarMensaje(new Mensaje("TURNO", !turnoJ1));
        }

        // --- MÉTODO PARA OBTENER MINAS DE UN TABLERO ESPECÍFICO ---
        private String obtenerMinasDeTablero(Tablero t) {
            StringBuilder minasStr = new StringBuilder();
            int size = Integer.parseInt(dificultadFinal);
            for(int i=0; i<size; i++){
                for(int j=0; j<size; j++){
                    if(t.getCasilla(i, j).isMina()){
                        minasStr.append(i).append(",").append(j).append(";");
                    }
                }
            }
            return minasStr.toString();
        }

        private void terminarPartida(ManejadorCliente ganador, ManejadorCliente perdedor, String razon) {
            try {
                // --- CORRECCIÓN FINAL: ENVIAR A CADA JUGADOR SUS PROPIAS MINAS ---
                
                // 1. Obtener minas del tablero del Jugador 1 y enviárselas SOLO a él
                String minasJ1 = obtenerMinasDeTablero(tableros[0]);
                j1.enviarMensaje(new Mensaje("MINAS", minasJ1));

                // 2. Obtener minas del tablero del Jugador 2 y enviárselas SOLO a él
                String minasJ2 = obtenerMinasDeTablero(tableros[1]);
                j2.enviarMensaje(new Mensaje("MINAS", minasJ2));
                
                // Pequeña pausa para asegurar orden de llegada
                Thread.sleep(100); 

                // Enviar resultado final
                ganador.enviarMensaje(new Mensaje("GANASTE", razon));
                perdedor.enviarMensaje(new Mensaje("PERDISTE", "Has perdido"));
                
                // Guardar en historial
                try {
                    persistencia.guardarResultado(ganador.nombreUsuario, perdedor.nombreUsuario, "Tablero " + dificultadFinal, "00:00");
                } catch (Exception e) { System.out.println("Error guardando historial: " + e.getMessage()); }

            } catch(Exception e) { } 
            finally { limpiarReferencias(); }
        }

        public void finalizarPorDesconexion() {
            try {
                if (!j1.socket.isClosed()) j1.enviarMensaje(new Mensaje("OPONENTE_DESCONECTADO", null));
                if (!j2.socket.isClosed()) j2.enviarMensaje(new Mensaje("OPONENTE_DESCONECTADO", null));
            } catch(Exception e) {}
            limpiarReferencias();
        }
        
        private void limpiarReferencias() {
            j1.setPartidaActual(null);
            j2.setPartidaActual(null);
        }
    }

    // --- CLASE MANEJADOR CLIENTE ---
    static class ManejadorCliente implements Runnable {
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;
        String nombreUsuario;
        boolean estaLogueado = false;
        Partida partidaActual = null;

        public ManejadorCliente(Socket s) {
            this.socket = s;
        }
        
        public void setPartidaActual(Partida p) {
            this.partidaActual = p;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                out.flush();
                in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                while (true) {
                    Mensaje msj = (Mensaje) in.readObject();
                    
                    // 1. INTERCEPTAMOS EL VOTO
                    if (msj.getTipo().equals("ENVIAR_VOTO")) {
                        if (partidaActual != null) {
                            String voto = (String) msj.getContenido();
                            partidaActual.recibirVoto(this, voto);
                        }
                        continue; 
                    }

                    // 2. MENSAJES DE JUEGO
                    if (partidaActual != null && esMensajeJuego(msj.getTipo())) {
                        partidaActual.procesarJugada(this, msj);
                        continue;
                    }
                    
                    // 3. MENSAJES DE MENÚ
                    switch (msj.getTipo()) {
                        case "LOGIN":
                            procesarLogin(msj);
                            break;
                        case "REGISTRO":
                            procesarRegistro(msj);
                            break;
                        case "HISTORIAL":
                            String h = persistencia.obtenerHistorial();
                            enviarMensaje(new Mensaje("HISTORIAL_OK", h));
                            break;
                        case "BUSCAR_PARTIDA":
                            if (estaLogueado) {
                                if (!clientesEnEspera.contains(this)) {
                                    System.out.println(nombreUsuario + " se unió a la cola de espera.");
                                    clientesEnEspera.add(this);
                                    intentarEmparejar();
                                }
                            } else {
                                enviarMensaje(new Mensaje("ERROR", "Debes iniciar sesion"));
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Cliente desconectado: " + (nombreUsuario != null ? nombreUsuario : "Anónimo"));
            } finally {
                if(partidaActual != null) partidaActual.finalizarPorDesconexion();
                clientesEnEspera.remove(this);
                try { socket.close(); } catch(IOException e) {}
            }
        }
        
        private boolean esMensajeJuego(String tipo) {
            return tipo.equals("CLICK") || tipo.equals("PERDIO") || tipo.equals("GANO");
        }

        private void procesarLogin(Mensaje msj) throws IOException {
            String[] datos = ((String) msj.getContenido()).split(":");
            Jugador j = persistencia.login(datos[0], datos[1]);
            if (j != null) {
                nombreUsuario = j.getNombre();
                estaLogueado = true;
                enviarMensaje(new Mensaje("LOGIN_OK", "Bienvenido " + nombreUsuario));
            } else {
                enviarMensaje(new Mensaje("ERROR", "Datos incorrectos"));
            }
        }

        private void procesarRegistro(Mensaje msj) throws IOException {
            String[] datos = ((String) msj.getContenido()).split(":");
            boolean exito = persistencia.registrarUsuario(datos[0], datos[1]);
            if (exito) {
                enviarMensaje(new Mensaje("REGISTRO_OK", "Cuenta creada"));
            } else {
                enviarMensaje(new Mensaje("ERROR", "Usuario ya existe"));
            }
        }
        
        public synchronized void enviarMensaje(Mensaje m) throws IOException {
            out.writeObject(m);
            out.flush();
            out.reset();
        }
    }
}