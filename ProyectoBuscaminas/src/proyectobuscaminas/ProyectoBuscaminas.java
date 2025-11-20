/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package proyectobuscaminas;

import javax.swing.SwingUtilities;

/**
 *
 * @author davek
 */
public class ProyectoBuscaminas {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Menu frame = new Menu();
        
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
        });
    }
    
}
