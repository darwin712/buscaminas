package proyectobuscaminas;

import java.io.Serializable;

public class Jugador implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nombre;
    private String password;
    private int partidasGanadas;
    private int partidasJugadas;

    public Jugador(String nombre, String password) {
        this.nombre = nombre;
        this.password = password;
        this.partidasGanadas = 0;
        this.partidasJugadas = 0;
    }

    public void registrarVictoria() {
        partidasGanadas++;
        partidasJugadas++;
    }

    public void registrarDerrota() {
        partidasJugadas++;
    }

    public String getNombre() { return nombre; }
    public String getPassword() { return password; }
    public int getPartidasGanadas() { return partidasGanadas; }
    public int getPartidasJugadas() { return partidasJugadas; }

    @Override
    public String toString() {
        return nombre + " (Ganadas: " + partidasGanadas + " / Jugadas: " + partidasJugadas + ")";
    }
}