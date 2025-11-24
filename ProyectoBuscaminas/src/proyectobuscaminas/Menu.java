/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package proyectobuscaminas;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author davek
 */
public class Menu extends Fondo { 
    private Socket socketTemp;
    private ObjectOutputStream outTemp;
    private ObjectInputStream inTemp;

    public Menu() {
        super("/proyectobuscaminas/imagenes/abstractbg.jpg");
        
        initComponents();
        
        Deco deco = new Deco();
        
        deco.agregarAnimacionHover(btnJugar);
        deco.agregarAnimacionHover(btnLogin);
        deco.agregarAnimacionHover(btnRegistro);
        deco.agregarAnimacionHover(btnRecords);

        if (Musica.getInstance().isPlaying()) {
            btnMusic.setText("ON");
        } else {
            btnMusic.setText("OFF");
        }
    }
    
    // --- UTILIDADES ---

    public void solicitarPartidaAutomatica() {
        SwingUtilities.invokeLater(() -> {
            btnJugar.doClick();
        });
    }

    private boolean conectarServidor() {
        try {
            if (socketTemp != null && !socketTemp.isClosed() && socketTemp.isConnected()) {
                return true; 
            }

            socketTemp = new Socket("localhost", 12345);
            outTemp = new ObjectOutputStream(socketTemp.getOutputStream());
            inTemp = new ObjectInputStream(socketTemp.getInputStream());
            return true;

        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "No se puede conectar al servidor.");
            return false;
        }
    }
    
    private void irAlJuego(Socket s, ObjectOutputStream o, ObjectInputStream i) {
        Juego panelJuego = (Juego) getParent().getComponent(1);
        panelJuego.iniciarJuegoConConexion(s, o, i);
        
        this.socketTemp = null; 
        
        java.awt.CardLayout cl = (java.awt.CardLayout) getParent().getLayout();
        cl.show(getParent(), "juego");
    }
    
    private void restaurarBotonJugar() {
        btnJugar.setText("Jugar");
        btnJugar.setEnabled(true);
    }


    // **************************************************************
    // ***          BOTÓN JUGAR — COMPLETAMENTE REPARADO          ***
    // **************************************************************
    
    private void btnJugarActionPerformed(java.awt.event.ActionEvent evt) {                                         

        System.out.println("--- Inicio proceso Jugar ---");

        if (socketTemp == null || socketTemp.isClosed()) {
            JOptionPane.showMessageDialog(this, "Debes Iniciar Sesión primero para jugar.");
            return;
        }

        // --- FEEDBACK VISUAL INMEDIATO ---
        btnJugar.setEnabled(false);
        btnJugar.setText("Esperando oponente...");
        btnJugar.paintImmediately(btnJugar.getVisibleRect());
        System.out.println("Botón cambiado a 'Esperando oponente...'");

        // --- HILO DE RED ---
        new Thread(() -> {
            try {
                // Enviar mensaje al servidor
                outTemp.writeObject(new Mensaje("BUSCAR_PARTIDA", "Clasico"));
                outTemp.flush();
                System.out.println("Solicitud BUSCAR_PARTIDA enviada.");

                // Esperar respuesta
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                System.out.println("Respuesta recibida: " + respuesta.getTipo());

                SwingUtilities.invokeLater(() -> {

                    switch (respuesta.getTipo()) {

                        case "RIVAL_ENCONTRADO":
                            System.out.println("Rival encontrado → iniciando juego.");

                            restaurarBotonJugar();
                            irAlJuego(socketTemp, outTemp, inTemp);

                            Juego panelJuego = (Juego) getParent().getComponent(1);
                            panelJuego.procesarMensaje(respuesta);
                            break;

                        case "BUSCANDO":
                            // Si algún día lo agregas en tu servidor
                            btnJugar.setText("Esperando oponente...");
                            btnJugar.setEnabled(false);
                            break;

                        default:
                            JOptionPane.showMessageDialog(
                                Menu.this,
                                "Respuesta del servidor: " + respuesta.getTipo()
                            );
                            restaurarBotonJugar();
                            break;
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        Menu.this,
                        "Error de conexión: " + e.getMessage()
                    );
                    restaurarBotonJugar();
                });
            }
        }).start();
    }                                        


    // **************************************************************
    // ***            RESTO DE TUS BOTONES (SIN CAMBIOS)          ***
    // **************************************************************

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if (!conectarServidor()) return;

        JTextField nombreField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] inputs = {"Usuario:", nombreField, "Contraseña:", passField};

        int op = JOptionPane.showConfirmDialog(this, inputs, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (op == JOptionPane.OK_OPTION) {
            try {
                String creds = nombreField.getText() + ":" + new String(passField.getPassword());
                outTemp.writeObject(new Mensaje("LOGIN", creds));
                outTemp.flush();
                
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                if (respuesta.getTipo().equals("LOGIN_OK")) {
                    JOptionPane.showMessageDialog(this, "Bienvenido " + nombreField.getText());
                } else {
                    JOptionPane.showMessageDialog(this, "Error: " + respuesta.getContenido(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }                                        

    private void btnRegistroActionPerformed(java.awt.event.ActionEvent evt) {                                            
        if (!conectarServidor()) return;

        JTextField nombreField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] inputs = {"Nuevo Usuario:", nombreField, "Nueva Contraseña:", passField};

        int op = JOptionPane.showConfirmDialog(this, inputs, "Registro", JOptionPane.OK_CANCEL_OPTION);
        if (op == JOptionPane.OK_OPTION) {
            try {
                String creds = nombreField.getText() + ":" + new String(passField.getPassword());
                outTemp.writeObject(new Mensaje("REGISTRO", creds));
                outTemp.flush();
                
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                JOptionPane.showMessageDialog(this, respuesta.getContenido());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }                                           

    private void btnRecordsActionPerformed(java.awt.event.ActionEvent evt) {                                           
        if (!conectarServidor()) {
            JOptionPane.showMessageDialog(this, "Conectate primero.");
            return;
        }
        
        btnRecords.setEnabled(false);
        btnRecords.setText("Cargando...");
        btnRecords.paintImmediately(btnRecords.getVisibleRect()); 

        new Thread(() -> {
            try {
                outTemp.writeObject(new Mensaje("HISTORIAL", null));
                outTemp.flush();
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                
                SwingUtilities.invokeLater(() -> {
                    if (respuesta.getTipo().equals("HISTORIAL_OK") || respuesta.getTipo().equals("RECORDS_DATA")) {
                        JTextArea textArea = new JTextArea((String) respuesta.getContenido());
                        textArea.setEditable(false);
                        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 300));
                        JOptionPane.showMessageDialog(Menu.this, scrollPane, "Historial", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(Menu.this, "Error: " + respuesta.getContenido());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(Menu.this, "Error obteniendo records"));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnRecords.setText("Records");
                    btnRecords.setEnabled(true);
                });
            }
        }).start();
    }                                          

    private void btnMusicActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if(Musica.getInstance().isPlaying()){
            Musica.getInstance().stopMusic();
            btnMusic.setText("OFF");
        }else{
            Musica.getInstance().playMusic("recursos/Tobu-Infectious.ogg");
            btnMusic.setText("ON");
        }
    }                                        


    // ****************************************************************
    // ***       CÓDIGO GENERADO POR NETBEANS (SIN EDITAR)          ***
    // ****************************************************************

    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        btnMusic = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        btnJugar = new javax.swing.JButton();
        btnLogin = new javax.swing.JButton();
        btnRegistro = new javax.swing.JButton();
        btnRecords = new javax.swing.JButton();

        setBackground(new java.awt.Color(92, 103, 125));
        setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(92, 103, 125));
        jPanel1.setOpaque(false);
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 3, 48)); 
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/proyectobuscaminas/imagenes/intentodelogo1.png"))); 
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipady = 39;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 88, 0, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        btnMusic.setBackground(new java.awt.Color(255, 255, 255));
        btnMusic.setFont(new java.awt.Font("Comic Sans MS", 1, 24)); 
        btnMusic.setForeground(new java.awt.Color(0, 0, 0));
        btnMusic.setText("ON");
        btnMusic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMusicActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 53, 0, 6);
        jPanel1.add(btnMusic, gridBagConstraints);

        add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel2.setOpaque(false);
        add(jPanel2, java.awt.BorderLayout.LINE_END);

        jPanel3.setOpaque(false);
        add(jPanel3, java.awt.BorderLayout.LINE_START);

        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel6.setOpaque(false);
        jPanel6.setLayout(null);

        btnJugar.setBackground(new java.awt.Color(255, 255, 255));
        btnJugar.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnJugar.setForeground(new java.awt.Color(0, 0, 0));
        btnJugar.setText("Jugar");
        btnJugar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJugarActionPerformed(evt);
            }
        });
        jPanel6.add(btnJugar);
        btnJugar.setBounds(20, 20, 320, 55);

        btnLogin.setBackground(new java.awt.Color(255, 255, 255));
        btnLogin.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnLogin.setForeground(new java.awt.Color(0, 0, 0));
        btnLogin.setText("Iniciar sesion");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });
        jPanel6.add(btnLogin);
        btnLogin.setBounds(20, 110, 320, 58);

        btnRegistro.setBackground(new java.awt.Color(255, 255, 255));
        btnRegistro.setFont(new java.awt.Font("Comic Sans MS", 1, 35)); 
        btnRegistro.setForeground(new java.awt.Color(0, 0, 0));
        btnRegistro.setText("Registrar usuario");
        btnRegistro.setPreferredSize(new java.awt.Dimension(187, 57));
        btnRegistro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistroActionPerformed(evt);
            }
        });
        jPanel6.add(btnRegistro);
        btnRegistro.setBounds(20, 210, 320, 57);

        btnRecords.setBackground(new java.awt.Color(255, 255, 255));
        btnRecords.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnRecords.setForeground(new java.awt.Color(0, 0, 0));
        btnRecords.setText("Records");
        btnRecords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecordsActionPerformed(evt);
            }
        });
        jPanel6.add(btnRecords);
        btnRecords.setBounds(20, 300, 320, 58);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 364;
        gridBagConstraints.ipady = 389;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(47, 153, 154, 198);
        jPanel4.add(jPanel6, gridBagConstraints);

        add(jPanel4, java.awt.BorderLayout.CENTER);
    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton btnJugar;
    private javax.swing.JButton btnLogin;
    private javax.swing.JButton btnMusic;
    private javax.swing.JButton btnRecords;
    private javax.swing.JButton btnRegistro;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    // End of variables declaration                   
}
