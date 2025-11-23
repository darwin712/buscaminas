/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.SwingUtilities;
/**
 *
 * @author davek
 */
public class ProyectoBuscaminas {
    public static void main(String[] args) {
        
        try {
            FlatLightLaf.setup();
        } catch(Exception e) {
            System.err.println("No se pudo cargar FlatLaf, usando diseño por defecto.");
        }

        
        SwingUtilities.invokeLater(() -> {
            
            Ventana frame = new Ventana();
            frame.setVisible(true);
        });
    }
}
