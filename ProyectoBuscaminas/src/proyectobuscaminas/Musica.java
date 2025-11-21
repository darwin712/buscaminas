/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
/**
 *
 * @author davek
 */
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

    /**
     * Convierte cualquier formato (incluyendo OGG) a PCM_SIGNED.
     * Esto es obligatorio porque Clip NO puede reproducir directamente Vorbis.
     */
    private AudioInputStream getPCMStream(String filePath) throws Exception {
        File file = new File(filePath);

        // Stream original (OGG, WAV, etc.)
        AudioInputStream originalStream = AudioSystem.getAudioInputStream(file);
        AudioFormat baseFormat = originalStream.getFormat();

        // Convertir a formato compatible
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

    /**
     * Reproduce música en bucle.
     */
    public void playMusic(String filePath) {
        try {
            AudioInputStream pcmStream = getPCMStream(filePath);

            audioClip = AudioSystem.getClip();
            audioClip.open(pcmStream);

            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            audioClip.start();

            isPlaying = true;
            wasPlayedOnce = true;

        } catch (Exception e) {
            System.err.println("Error al reproducir música OGG/WAV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reproduce efectos de sonido (sin loop)
     */
    public void playSFX(String filePath) {
        try {
            AudioInputStream pcmStream = getPCMStream(filePath);

            Clip sfx = AudioSystem.getClip();
            sfx.open(pcmStream);
            sfx.start();

        } catch (Exception e) {
            System.err.println("Error al reproducir SFX OGG/WAV: " + e.getMessage());
        }
    }

    /**
     * Detener música
     */
    public void stopMusic() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            isPlaying = false;
        }
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
