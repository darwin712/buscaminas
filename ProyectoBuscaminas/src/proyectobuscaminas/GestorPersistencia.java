package proyectobuscaminas;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorPersistencia {
    private static final String ARCHIVO_DATOS = "buscaminas_data.dat";
    private Map<String, Jugador> jugadores;
    private List<String> historialPartidas; // Lista de cadenas para el historial general

    public GestorPersistencia() {
        cargarDatos();
    }

    public synchronized boolean registrarUsuario(String nombre, String pass) {
        if (jugadores.containsKey(nombre)) {
            return false;
        }
        jugadores.put(nombre, new Jugador(nombre, pass));
        guardarDatos();
        return true;
    }

    public synchronized Jugador login(String nombre, String pass) {
        Jugador j = jugadores.get(nombre);
        if (j != null && j.getPassword().equals(pass)) {
            return j;
        }
        return null;
    }

    public synchronized void guardarResultado(String ganador, String perdedor, String dificultad, String tiempo) {
        if (jugadores.containsKey(ganador)) jugadores.get(ganador).registrarVictoria();
        if (jugadores.containsKey(perdedor)) jugadores.get(perdedor).registrarDerrota();
        
        // Formato: [HORA] Ganador vs Perdedor (Dificultad) - Tiempo
        String registro = String.format("[%s] %s vencio a %s (%s) en %s", 
                new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()), 
                ganador, perdedor, dificultad, tiempo);
        
        historialPartidas.add(0, registro); // Agregar al inicio para que sea el más reciente
        System.out.println("Persistencia: " + registro);
        
        guardarDatos();
    }
    
    // Devuelve el historial completo como un solo String para mostrar en el JTextArea
    public synchronized String obtenerHistorial() {
        if (historialPartidas.isEmpty()) return "No hay partidas registradas.";
        StringBuilder sb = new StringBuilder();
        sb.append("=== HISTORIAL DE PARTIDAS ===\n\n");
        for (String s : historialPartidas) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    // Método nuevo para obtener el Top de jugadores (Ranking)
    // Esto se usará para llenar la lista de "Posiciones" en el juego
    public synchronized String obtenerRanking() {
        if (jugadores.isEmpty()) return "Sin datos";
        
        List<Jugador> ranking = new ArrayList<>(jugadores.values());
        // Ordenar por partidas ganadas descendente
        Collections.sort(ranking, (j1, j2) -> Integer.compare(j2.getPartidasGanadas(), j1.getPartidasGanadas()));
        
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Jugador j : ranking) {
            if (i > 6) break; // Solo los top 6
            sb.append(i).append("ro. ").append(j.getNombre())
              .append(" (").append(j.getPartidasGanadas()).append(" wins)\n");
            i++;
        }
        return sb.toString();
    }

    private void guardarDatos() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_DATOS))) {
            oos.writeObject(jugadores);
            oos.writeObject(historialPartidas);
        } catch (IOException e) {
            System.err.println("Error guardando datos: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void cargarDatos() {
        File f = new File(ARCHIVO_DATOS);
        if (!f.exists()) {
            jugadores = new HashMap<>();
            historialPartidas = new ArrayList<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            jugadores = (Map<String, Jugador>) ois.readObject();
            historialPartidas = (List<String>) ois.readObject();
        } catch (Exception e) {
            System.err.println("Error cargando datos (iniciando vacio): " + e.getMessage());
            jugadores = new HashMap<>();
            historialPartidas = new ArrayList<>();
        }
    }
}