package proyectobuscaminas;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import java.awt.CardLayout; // IMPORTANTE: Importación corregida

public class Menu extends javax.swing.JPanel {

    private Socket socketTemp;
    private ObjectOutputStream outTemp;
    private ObjectInputStream inTemp;
    
    private JButton btnMusic;

    public Menu() {
        initComponentsPersonalizado();
        if (Musica.getInstance().isPlaying()) {
            btnMusic.setText("ON");
        } else {
            btnMusic.setText("OFF");
        }
    }

    private boolean conectarServidor() {
        try {
            if (socketTemp == null || socketTemp.isClosed()) {
                socketTemp = new Socket("localhost", 12345);
                outTemp = new ObjectOutputStream(socketTemp.getOutputStream());
                inTemp = new ObjectInputStream(socketTemp.getInputStream());
            }
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se puede conectar al servidor.");
            return false;
        }
    }

    private void initComponentsPersonalizado() {
        setLayout(new BorderLayout());
        setBackground(new Color(80, 90, 100)); // Color de fondo gris azulado

        // --- PANEL SUPERIOR (Botón Sonido a la derecha) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        
        btnMusic = new JButton("ON");
        btnMusic.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnMusic.setBackground(Color.WHITE);
        btnMusic.setForeground(Color.BLACK);
        btnMusic.setFocusPainted(false);
        btnMusic.addActionListener(e -> btnMusicActionPerformed());
        
        topPanel.add(btnMusic);
        add(topPanel, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Título y Botones) ---
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título
        JLabel titleLabel = new JLabel("Buscaminas");
        titleLabel.setFont(new Font("Segoe UI", Font.ITALIC | Font.BOLD, 52));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 0;
        centerPanel.add(titleLabel, gbc);

        // Botones
        gbc.gridy = 1;
        centerPanel.add(crearBoton("Jugar"), gbc);
        
        gbc.gridy = 2;
        centerPanel.add(crearBoton("Iniciar sesion"), gbc);
        
        gbc.gridy = 3;
        centerPanel.add(crearBoton("Registrar usuario"), gbc);
        
        gbc.gridy = 4;
        centerPanel.add(crearBoton("Records"), gbc);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JButton crearBoton(String texto) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(350, 60));
        
        // Asignar acciones según el texto
        if (texto.equals("Jugar")) btn.addActionListener(e -> btnJugarActionPerformed());
        else if (texto.equals("Iniciar sesion")) btn.addActionListener(e -> btnLoginActionPerformed());
        else if (texto.equals("Registrar usuario")) btn.addActionListener(e -> btnRegistroActionPerformed());
        else if (texto.equals("Records")) btn.addActionListener(e -> btnRecordsActionPerformed());
        
        return btn;
    }

    private void btnLoginActionPerformed() {
        if (!conectarServidor()) return;

        JTextField nombreField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] inputs = {"Usuario:", nombreField, "Contraseña:", passField};

        int op = JOptionPane.showConfirmDialog(this, inputs, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (op == JOptionPane.OK_OPTION) {
            try {
                String creds = nombreField.getText() + ":" + new String(passField.getPassword());
                outTemp.writeObject(new Mensaje("LOGIN", creds));
                
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                if (respuesta.getTipo().equals("LOGIN_OK")) {
                    JOptionPane.showMessageDialog(this, "Bienvenido " + nombreField.getText());
                } else {
                    JOptionPane.showMessageDialog(this, "Error: " + respuesta.getContenido(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void btnRegistroActionPerformed() {
        if (!conectarServidor()) return;

        JTextField nombreField = new JTextField();
        JPasswordField passField = new JPasswordField();
        Object[] inputs = {"Nuevo Usuario:", nombreField, "Nueva Contraseña:", passField};

        int op = JOptionPane.showConfirmDialog(this, inputs, "Registro", JOptionPane.OK_CANCEL_OPTION);
        if (op == JOptionPane.OK_OPTION) {
            try {
                String creds = nombreField.getText() + ":" + new String(passField.getPassword());
                outTemp.writeObject(new Mensaje("REGISTRO", creds));
                
                Mensaje respuesta = (Mensaje) inTemp.readObject();
                JOptionPane.showMessageDialog(this, respuesta.getContenido());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void btnJugarActionPerformed() {
        if (socketTemp == null || socketTemp.isClosed()) {
            JOptionPane.showMessageDialog(this, "Debes Iniciar Sesion primero.");
            return;
        }

        try {
            // Enviar solicitud de partida (Modo Único: Clasico)
            outTemp.writeObject(new Mensaje("BUSCAR_PARTIDA", "Clasico"));
            
            // Ir a la pantalla de juego
            irAlJuego(socketTemp, outTemp, inTemp);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void btnRecordsActionPerformed() {
        if (socketTemp == null || socketTemp.isClosed()) {
            if (!conectarServidor()) {
                JOptionPane.showMessageDialog(this, "Conectate primero.");
                return;
            }
        }
        
        try {
            outTemp.writeObject(new Mensaje("RECORDS", null));
            Mensaje respuesta = (Mensaje) inTemp.readObject();
            
            if (respuesta.getTipo().equals("RECORDS_DATA")) {
                JTextArea textArea = new JTextArea((String) respuesta.getContenido());
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(this, scrollPane, "Historial de Partidas", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al obtener records.");
        }
    }
    
    private void irAlJuego(Socket s, ObjectOutputStream o, ObjectInputStream i) {
        // El componente en el índice 1 del panel padre (Ventana) es Juego.java
        Juego panelJuego = (Juego) getParent().getComponent(1);
        panelJuego.iniciarJuegoConConexion(s, o, i);
        
        // Obtenemos el CardLayout del panel padre (que es mainPanel en Ventana.java)
        java.awt.CardLayout cl = (java.awt.CardLayout) getParent().getLayout();
        cl.show(getParent(), "juego");
    }

    private void btnMusicActionPerformed() {
        if(Musica.getInstance().isPlaying()){
            Musica.getInstance().stopMusic();
            btnMusic.setText("OFF");
        }else{
            Musica.getInstance().playMusic("recursos/Tobu-Infectious.ogg");
            btnMusic.setText("ON");
        }
    }
}