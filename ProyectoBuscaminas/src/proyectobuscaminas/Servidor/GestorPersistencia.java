package proyectobuscaminas.Servidor;


import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import proyectobuscaminas.Cliente.Jugador;

public class GestorPersistencia {
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static final String ARCHIVO_HISTORIAL = "historial.txt";

    public GestorPersistencia() {
        try {
            
            new File(ARCHIVO_USUARIOS).createNewFile();
            new File(ARCHIVO_HISTORIAL).createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
    }


    
    public synchronized boolean registrarUsuario(String usuario, String password) {
        if (usuarioExiste(usuario)) return false;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            writer.write(usuario + ":" + password);
            writer.newLine();
            return true;
        } catch (IOException e) { return false; }
    }

    public synchronized boolean validarUsuario(String usuario, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length >= 2 && partes[0].equals(usuario) && partes[1].equals(password)) return true;
            }
        } catch (IOException e) {}
        return false;
    }
    
    public Jugador login(String u, String p) { 
        return validarUsuario(u, p) ? new Jugador(u, p) : null; 
    }

    private boolean usuarioExiste(String usuario) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.startsWith(usuario + ":")) return true;
            }
        } catch (IOException e) {}
        return false;
    }

    

   
    public synchronized void guardarMovimiento(String nombreArchivoLog, String jugador, String accion, String detalle) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nombreArchivoLog, true))) {
            String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
            
            String linea = String.format("[%s] %s: %s -> %s", hora, jugador, accion, detalle);
            writer.write(linea);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error escribiendo log: " + e.getMessage());
        }
    }
 
    public synchronized void guardarResultado(String ganador, String perdedor, String mapa, String tiempo, String archivoLog) {
        String fecha = new SimpleDateFormat("dd/MM HH:mm").format(new Date());
        
               String registro = String.format("[%s] %s venció a %s (%s) - %s|%s", 
                                        fecha, ganador, perdedor, mapa, tiempo, archivoLog);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_HISTORIAL, true))) {
            writer.write(registro);
            writer.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

  
    public synchronized List<String> obtenerHistorialRaw(String usuarioFiltro) {
        List<String> resultados = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_HISTORIAL))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
               
                if (linea.contains(usuarioFiltro)) {
                    resultados.add(linea);
                }
            }
        } catch (IOException e) {}
        Collections.reverse(resultados); 
        return resultados;
    }
    
  
    public synchronized String leerLogPartida(String nombreArchivo) {
        StringBuilder sb = new StringBuilder();
        File f = new File(nombreArchivo);
        if(!f.exists()) return "Error: El archivo de registro de esta partida no se encuentra.";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                sb.append(linea).append("\n");
            }
        } catch (IOException e) { return "Error leyendo archivo de log."; }
        return sb.toString();
    }
    
  
    public String obtenerHistorial() { return ""; }
}