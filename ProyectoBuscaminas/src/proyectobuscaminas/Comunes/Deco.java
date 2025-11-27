/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas.Comunes;

import java.awt.Cursor;
import java.awt.Insets;
import javax.swing.JButton;

/**
 *
 * @author davek
 */
public class Deco {
    public void agregarAnimacionHover(JButton boton) {


        final int posOriginalY = boton.getY();
        final int offset = 5; 

        boton.addMouseListener(new java.awt.event.MouseAdapter() {

            javax.swing.Timer animUp;
            javax.swing.Timer animDown;

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {

            
                boton.setCursor(new Cursor(Cursor.HAND_CURSOR));

               
                if (animDown != null && animDown.isRunning())
                    animDown.stop();

                animUp = new javax.swing.Timer(10, ev -> {
                    int y = boton.getY();

                    if (y > posOriginalY - offset) {
                        boton.setLocation(boton.getX(), y - 1);
                    } else {
                        ((javax.swing.Timer) ev.getSource()).stop();
                    }
                });

                animUp.start();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {

                
                boton.setCursor(Cursor.getDefaultCursor());

       
                if (animUp != null && animUp.isRunning())
                    animUp.stop();

                animDown = new javax.swing.Timer(10, ev -> {
                    int y = boton.getY();

                    if (y < posOriginalY) {
                        boton.setLocation(boton.getX(), y + 1);
                    } else {
                        boton.setLocation(boton.getX(), posOriginalY);
                        ((javax.swing.Timer) ev.getSource()).stop();
                    }
                });

                animDown.start();
            }
        });
    }
}
