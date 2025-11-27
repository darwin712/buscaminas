package proyectobuscaminas.Comunes;

import java.io.File;
import javax.sound.sampled.*;

public class Musica {
    private static Musica instance;
    private boolean isPlaying;
    private boolean wasPlayedOnce;
    private Clip audioClip;

    private Musica() {
        isPlaying = false;
        wasPlayedOnce = false;
    }

    public static Musica getInstance() {
        if (instance == null) {
            instance = new Musica();
        }
        return instance;
    }

    // Tu método original para leer OGG/WAV
    private AudioInputStream getPCMStream(String filePath) throws Exception {
        File file = new File(filePath);
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(file);
        AudioFormat baseFormat = originalStream.getFormat();

        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
        );

        return AudioSystem.getAudioInputStream(decodedFormat, originalStream);
    }

    public void playMusic(String filePath) {
        // 1. Detener cualquier música anterior antes de empezar una nueva
        stopMusic(); 

        try {
            AudioInputStream pcmStream = getPCMStream(filePath);

            audioClip = AudioSystem.getClip();
            audioClip.open(pcmStream);

            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            audioClip.start();

            isPlaying = true;
            wasPlayedOnce = true;

        } catch (Exception e) {
            System.err.println("Error al reproducir música: " + e.getMessage());
            // Si falla, aseguramos que el estado sea false
            isPlaying = false; 
        }
    }

    public void playSFX(String filePath) {
        try {
            AudioInputStream pcmStream = getPCMStream(filePath);
            Clip sfx = AudioSystem.getClip();
            sfx.open(pcmStream);
            sfx.start();
        } catch (Exception e) {
            System.err.println("Error SFX: " + e.getMessage());
        }
    }

    public void stopMusic() {
        if (audioClip != null) {
            try {
                // Detener y cerrar para liberar memoria
                audioClip.stop();
                audioClip.close(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 2. CORRECCIÓN CRÍTICA: Forzar el estado a false SIEMPRE
        isPlaying = false; 
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean wasPlayedOnce() {
        return wasPlayedOnce;
    }

    public void setWasPlayedOnce(boolean value) {
        this.wasPlayedOnce = value;
    }
}