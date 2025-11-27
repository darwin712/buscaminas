package proyectobuscaminas;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class GestorPersistencia {
    // Archivo binario (.dat)
    private static final String ARCHIVO_DATOS = "buscaminas_data.dat";
    
    // Estructuras en memoria
    private Map<String, Jugador> jugadores;
    private List<String> historialPartidas; 

    public GestorPersistencia() {
        cargarDatos();
    }

    public synchronized boolean registrarUsuario(String nombre, String pass) {
        if (jugadores.containsKey(nombre)) {
            return false;
        }
        jugadores.put(nombre, new Jugador(nombre, pass));
        guardarDatos(); // Guardado binario
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
        // Actualizar estadísticas de los objetos Jugador
        if (jugadores.containsKey(ganador)) jugadores.get(ganador).registrarVictoria();
        if (jugadores.containsKey(perdedor)) jugadores.get(perdedor).registrarDerrota();
        
        // Crear el registro de texto
        String fecha = new SimpleDateFormat("dd/MM HH:mm").format(new Date());
        
        // Formato: [FECHA] Ganador venció a Perdedor (Mapa) - Tiempo
        String registro = String.format("[%s] %s venció a %s (%s) - %s", 
                fecha, ganador, perdedor, dificultad, tiempo);
        
        // Agregar al inicio de la lista (más reciente primero)
        historialPartidas.add(0, registro);
        
        System.out.println("Guardando en binario: " + registro);
        guardarDatos(); // Guardar todo el objeto lista en el .dat
    }
    
    // --- MÉTODO MODIFICADO PARA FILTRAR ---
    // Ahora recibe el nombre del usuario que pide el historial
    public synchronized String obtenerHistorial(String usuarioFiltro) {
        if (historialPartidas.isEmpty()) return "No hay partidas en el sistema.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== HISTORIAL DE ").append(usuarioFiltro.toUpperCase()).append(" ===\n\n");
        
        boolean encontroAlgo = false;
        
        for (String partida : historialPartidas) {
            // FILTRO: Solo agregamos la línea si contiene el nombre del usuario
            if (partida.contains(usuarioFiltro)) {
                sb.append(partida).append("\n");
                encontroAlgo = true;
            }
        }
        
        if (!encontroAlgo) {
            return "No has jugado ninguna partida todavía.";
        }
        
        return sb.toString();
    }

    public synchronized String obtenerRanking() {
        if (jugadores.isEmpty()) return "Sin datos";
        
        List<Jugador> ranking = new ArrayList<>(jugadores.values());
        Collections.sort(ranking, (j1, j2) -> Integer.compare(j2.getPartidasGanadas(), j1.getPartidasGanadas()));
        
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Jugador j : ranking) {
            if (i > 10) break; 
            sb.append(i).append(". ").append(j.getNombre())
              .append(" - Victorias: ").append(j.getPartidasGanadas()).append("\n");
            i++;
        }
        return sb.toString();
    }

    // --- MÉTODOS DE SERIALIZACIÓN BINARIA (NO TOCAR) ---
    
    private void guardarDatos() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_DATOS))) {
            oos.writeObject(jugadores);
            oos.writeObject(historialPartidas);
        } catch (IOException e) {
            System.err.println("Error guardando binario: " + e.getMessage());
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
            Object obj1 = ois.readObject();
            Object obj2 = ois.readObject();
            
            if (obj1 instanceof Map) jugadores = (Map<String, Jugador>) obj1;
            else jugadores = new HashMap<>();
            
            if (obj2 instanceof List) historialPartidas = (List<String>) obj2;
            else historialPartidas = new ArrayList<>();
            
        } catch (Exception e) {
            System.err.println("Error cargando binario (se reiniciaran datos): " + e.getMessage());
            jugadores = new HashMap<>();
            historialPartidas = new ArrayList<>();
        }
    }
}