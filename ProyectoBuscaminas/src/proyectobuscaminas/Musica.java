/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas;
import java.io.BufferedInputStream;
import java.net.URL;
import javazoom.jl.player.Player;
/**
 *
 * @author davek
 */
public class Musica {
    private static Musica instance;
    private Player player;
    private boolean isPlaying;

    private Musica() {
        isPlaying = false;
    }

    public static Musica getInstance() {
        if (instance == null) {
            instance = new Musica();
        }
        return instance;
    }

    public void playFromURL(String urlString) {
        try {
            stopMusic(); // Para evitar 2 canciones a la vez

            URL url = new URL(urlString);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());

            player = new Player(bis);

            isPlaying = true;

            // Reproducir en hilo aparte
            new Thread(() -> {
                try {
                    player.play();
                } catch (Exception e) {
                    System.err.println("Error reproduciendo música: " + e.getMessage());
                }
            }).start();

        } catch (Exception ex) {
            System.err.println("No se pudo reproducir desde URL: " + ex.getMessage());
        }
    }

    public void stopMusic() {
        try {
            if (player != null) {
                player.close();
            }
            isPlaying = false;
        } catch (Exception e) {
            System.err.println("Error al detener música: " + e.getMessage());
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
