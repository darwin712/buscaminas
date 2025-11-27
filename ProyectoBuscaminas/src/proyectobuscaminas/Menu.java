package proyectobuscaminas;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class Menu extends Fondo { 
    
    // --- VARIABLES DE SESIÓN (Estáticas) ---
    public static boolean isLogueado = false;
    public static String usuarioActual = "";
    private static String passActual = ""; 
    
    // Conexión
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean buscandoPartida = false;

    // Componentes de Carga
    private JPanel glassPanel;
    private JLabel lblEstado;
    private Timer timerAnimacion;
    private JButton btnCancelar;

    public Menu() {
        super("/proyectobuscaminas/imagenes/abstractbg.jpg");
        initComponents();
        initGlassPane(); // Inicializar capa de carga
        
        // Decoración de botones
        Deco deco = new Deco();
        deco.agregarAnimacionHover(btnJugar);
        deco.agregarAnimacionHover(btnLogin);
        deco.agregarAnimacionHover(btnRegistro);
        deco.agregarAnimacionHover(btnRecords);

        if (Musica.getInstance().isPlaying()) btnMusic.setText("ON");
        else btnMusic.setText("OFF");
        
    }

    // --- 1. PANTALLA DE CARGA (GLASS PANE) ---
    private void initGlassPane() {
        // Usamos un panel personalizado que fuerza el pintado del fondo semitransparente
        glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        
        glassPanel.setLayout(new GridBagLayout());
        glassPanel.setBackground(new Color(0, 0, 0, 220)); // Negro transparente
        glassPanel.setOpaque(false); 
        
        lblEstado = new JLabel("CARGANDO");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblEstado.setForeground(Color.WHITE);
        glassPanel.add(lblEstado);
        
        // Bloquear interacción del ratón y teclado mientras carga
        MouseAdapter blocker = new MouseAdapter() {};
        glassPanel.addMouseListener(blocker);
        glassPanel.addMouseMotionListener(blocker);
        glassPanel.addKeyListener(new KeyAdapter() {});
        
        // Animación de los puntos "..."
        timerAnimacion = new Timer(500, e -> {
            String txt = lblEstado.getText();
            if (txt.endsWith("...")) lblEstado.setText(txt.replace("...", ""));
            else lblEstado.setText(txt + ".");
        });
    }


    private void mostrarCarga(boolean mostrar, String texto) {
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root != null) {
            if (mostrar) {
                lblEstado.setText(texto);
                root.setGlassPane(glassPanel); // Poner el panel en la capa superior (GlassPane)
                glassPanel.setVisible(true);
                timerAnimacion.start();
                root.getGlassPane().setVisible(true);
            } else {
                timerAnimacion.stop();
                glassPanel.setVisible(false);
                root.getGlassPane().setVisible(false);
            }
        }
    }

    // --- 2. GESTIÓN DE CONEXIÓN ---
    private boolean asegurarConexion() {
        try {
            // Si ya existe y está bien, usarla
            if (socket != null && !socket.isClosed()) return true;
            
            // Si no, conectar
            socket = new Socket("localhost", 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Auto-Login si tenemos credenciales guardadas
            if (isLogueado && !usuarioActual.isEmpty() && !passActual.isEmpty()) {
                out.writeObject(new Mensaje("LOGIN", usuarioActual + ":" + passActual));
                out.flush();
                Mensaje r = (Mensaje) in.readObject();
                if (!r.getTipo().equals("LOGIN_OK")) {
                    isLogueado = false; // Si falla, pedir login manual
                }
            }
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se puede conectar al servidor.");
            return false;
        }
    }

    // --- 3. BOTÓN JUGAR ---
    private void btnJugarActionPerformed(java.awt.event.ActionEvent evt) {                                         
        Musica.getInstance().playSFX("recursos/Click2.ogg");
        buscandoPartida = true;
        if (!asegurarConexion()) return;
        
        if (!isLogueado) {
            JOptionPane.showMessageDialog(this, "Debes iniciar sesión primero.");
            return;
        }

        mostrarCarga(true, "BUSCANDO RIVAL");
        
        new Thread(() -> {
            try {
                enviar("BUSCAR_PARTIDA", "Clasico");
                
                while (true) {
                    Mensaje msg = (Mensaje) in.readObject();
                    
                    if (msg.getTipo().equals("BUSCANDO")) {
                        SwingUtilities.invokeLater(() -> lblEstado.setText("ESPERANDO OPONENTE"));
                    } 
                    else if (msg.getTipo().equals("RIVAL_ENCONTRADO")) {
                        String nombreRival = (String) msg.getContenido();
                        SwingUtilities.invokeLater(() -> {
                            mostrarCarga(false, "");
                            irAlJuego(nombreRival);
                        });
                        break;
                    }
                    else if (msg.getTipo().equals("ERROR")) {
                         throw new Exception(msg.getContenido().toString());
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    mostrarCarga(false, "");
                    JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                });
            }
        }).start();
    }                                        

    private void irAlJuego(String rival) {
        Juego panelJuego = (Juego) getParent().getComponent(1);
        // Transferir conexión al juego
        panelJuego.iniciarJuego(socket, out, in, rival, usuarioActual);
        this.socket = null; // Soltar referencia en el menú
        
        CardLayout cl = (CardLayout) getParent().getLayout();
        cl.show(getParent(), "juego");
    }

    public void solicitarPartidaAutomatica() {
        SwingUtilities.invokeLater(() -> {
            if (isLogueado) btnJugar.doClick();
        });
    }

    // --- OTROS BOTONES ---
    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {  
        Musica.getInstance().playSFX("recursos/Click2.ogg");
        if (!asegurarConexion()) return;

        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        Object[] msg = {"Usuario:", u, "Contraseña:", p};

        if (JOptionPane.showConfirmDialog(this, msg, "Login", JOptionPane.OK_CANCEL_OPTION) == 0) {
            try {
                enviar("LOGIN", u.getText() + ":" + new String(p.getPassword()));
                Mensaje r = (Mensaje) in.readObject();
                
                if (r.getTipo().equals("LOGIN_OK")) {
                    isLogueado = true;
                    usuarioActual = u.getText();
                    passActual = new String(p.getPassword());
                    JOptionPane.showMessageDialog(this, "Bienvenido " + usuarioActual);
                } else {
                    JOptionPane.showMessageDialog(this, r.getContenido());
                }
            } catch (Exception e) {}
        }
    }                                        

    private void btnRegistroActionPerformed(java.awt.event.ActionEvent evt) { 
        Musica.getInstance().playSFX("recursos/Click2.ogg");
        if (!asegurarConexion()) return;
        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        Object[] msg = {"Nuevo Usuario:", u, "Nueva Contraseña:", p};
        
        if (JOptionPane.showConfirmDialog(this, msg, "Registro", JOptionPane.OK_CANCEL_OPTION) == 0) {
            try {
                enviar("REGISTRO", u.getText() + ":" + new String(p.getPassword()));
                JOptionPane.showMessageDialog(this, ((Mensaje)in.readObject()).getContenido());
            } catch (Exception e) {}
        }
    }                                           

    private void btnRecordsActionPerformed(java.awt.event.ActionEvent evt) {  
        Musica.getInstance().playSFX("recursos/Click2.ogg");
        if (!asegurarConexion()) return;
        if (!isLogueado) { JOptionPane.showMessageDialog(this, "Inicia sesión para ver tus records."); return; }
        
        new Thread(() -> {
            try {
                enviar("HISTORIAL", usuarioActual); // Enviar usuario para filtrar
                Mensaje r = (Mensaje) in.readObject();
                
                SwingUtilities.invokeLater(() -> {
                    JTextArea ta = new JTextArea((String)r.getContenido());
                    ta.setEditable(false);
                    ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
                    JOptionPane.showMessageDialog(Menu.this, new JScrollPane(ta));
                });
            } catch (Exception e) {}
        }).start();
    }                                          

    private void btnMusicActionPerformed(java.awt.event.ActionEvent evt) { 
        Musica.getInstance().playSFX("recursos/Click2.ogg");
        if(Musica.getInstance().isPlaying()){
            Musica.getInstance().stopMusic();
            btnMusic.setText("OFF");
        } else {
            Musica.getInstance().playMusic("recursos/MainTheme.ogg");
            btnMusic.setText("ON");
        }
    }                                        

    private void enviar(String tipo, Object contenido) throws IOException {
        out.writeObject(new Mensaje(tipo, contenido));
        out.flush();
        out.reset();
    }

    // --- COMPONENTES VISUALES (DISEÑO ORIGINAL) ---
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

        // Header
        jPanel1.setBackground(new java.awt.Color(92, 103, 125));
        jPanel1.setOpaque(false);
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Segoe UI", 3, 48)); 
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        try {
            jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/proyectobuscaminas/imagenes/intentodelogo1.png"))); 
            jLabel1.setText("");
        } catch(Exception e) {
            jLabel1.setText("BUSCAMINAS");
        }
        
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

        // Laterales
        jPanel2.setOpaque(false);
        add(jPanel2, java.awt.BorderLayout.LINE_END);

        jPanel3.setOpaque(false);
        add(jPanel3, java.awt.BorderLayout.LINE_START);

        // Centro
        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jPanel6.setOpaque(false);
        jPanel6.setLayout(null);
        jPanel6.setPreferredSize(new Dimension(360, 400));

        btnJugar.setBackground(new java.awt.Color(255, 255, 255));
        btnJugar.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnJugar.setForeground(new java.awt.Color(0, 0, 0));
        btnJugar.setText("Jugar");
        btnJugar.setBounds(20, 20, 320, 55);
        btnJugar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJugarActionPerformed(evt);
            }
        });
        jPanel6.add(btnJugar);

        btnLogin.setBackground(new java.awt.Color(255, 255, 255));
        btnLogin.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnLogin.setForeground(new java.awt.Color(0, 0, 0));
        btnLogin.setText("Iniciar sesion");
        btnLogin.setBounds(20, 110, 320, 58);
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });
        jPanel6.add(btnLogin);

        btnRegistro.setBackground(new java.awt.Color(255, 255, 255));
        btnRegistro.setFont(new java.awt.Font("Comic Sans MS", 1, 35)); 
        btnRegistro.setForeground(new java.awt.Color(0, 0, 0));
        btnRegistro.setText("Registrar usuario");
        btnRegistro.setBounds(20, 210, 320, 57);
        btnRegistro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistroActionPerformed(evt);
            }
        });
        jPanel6.add(btnRegistro);

        btnRecords.setBackground(new java.awt.Color(255, 255, 255));
        btnRecords.setFont(new java.awt.Font("Comic Sans MS", 1, 36)); 
        btnRecords.setForeground(new java.awt.Color(0, 0, 0));
        btnRecords.setText("Records");
        btnRecords.setBounds(20, 300, 320, 58);
        btnRecords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRecordsActionPerformed(evt);
            }
        });
        jPanel6.add(btnRecords);

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
    
    // Variables declaration
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
}