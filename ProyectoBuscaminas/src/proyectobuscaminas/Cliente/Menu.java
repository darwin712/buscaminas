package proyectobuscaminas.Cliente;



import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import javax.swing.*;
import proyectobuscaminas.Comunes.Deco;
import proyectobuscaminas.Comunes.Fondo;
import proyectobuscaminas.Comunes.Mensaje;
import proyectobuscaminas.Comunes.Musica;
import proyectobuscaminas.Comunes.Ventana;


public class Menu extends Fondo { 
    
    public static boolean isLogueado = false;
    public static String usuarioActual = "";
    private static String passActual = ""; 
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private JPanel glassPanel;
    private JLabel lblEstado;
    private Timer timerAnimacion;

    public Menu() {
        super("/proyectobuscaminas/imagenes/abstractbg.jpg");
        initComponents();
        initGlassPane();
        
        Deco deco = new Deco();
        deco.agregarAnimacionHover(btnJugar);
        deco.agregarAnimacionHover(btnLogin);
        deco.agregarAnimacionHover(btnRegistro);
        deco.agregarAnimacionHover(btnRecords);

        // Sincronizar texto inicial
        if (Musica.getInstance().isPlaying()) btnMusic.setText("ON");
        else btnMusic.setText("OFF");
    }

    // --- PANTALLA DE CARGA ---
    private void initGlassPane() {
        glassPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        glassPanel.setLayout(new GridBagLayout());
        glassPanel.setBackground(new Color(0, 0, 0, 220));
        glassPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        
        lblEstado = new JLabel("CARGANDO");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 48));
        lblEstado.setForeground(Color.WHITE);
        glassPanel.add(lblEstado, gbc);
        
        JButton btnCancelar = new JButton("CANCELAR");
        btnCancelar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancelar.setBackground(new Color(231, 76, 60));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFocusable(false);
        btnCancelar.addActionListener(e -> cancelarBusqueda());
        
        gbc.gridy = 1; gbc.insets = new Insets(20, 0, 0, 0);
        glassPanel.add(btnCancelar, gbc);
        
        MouseAdapter blocker = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { e.consume(); }
            @Override public void mouseClicked(MouseEvent e) { e.consume(); }
        };
        glassPanel.addMouseListener(blocker);
        glassPanel.addMouseMotionListener(blocker);
        glassPanel.addKeyListener(new KeyAdapter() {});
        
        timerAnimacion = new Timer(500, e -> {
            String txt = lblEstado.getText();
            lblEstado.setText(txt.endsWith("...") ? txt.replace("...", "") : txt + ".");
        });
    }

    private void mostrarCarga(boolean mostrar, String texto) {
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root != null) {
            if (mostrar) {
                lblEstado.setText(texto);
                root.setGlassPane(glassPanel);
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

    private void cancelarBusqueda() {
        lblEstado.setText("CANCELANDO...");
        new Thread(() -> {
            try { enviar("CANCELAR_BUSQUEDA", null); } 
            catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    mostrarCarga(false, "");
                    btnJugar.setEnabled(true);
                    this.socket = null;
                });
            }
        }).start();
    }

    private boolean asegurarConexion() {
        try {
            if (socket != null && !socket.isClosed()) return true;
            socket = new Socket("localhost", 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            if (isLogueado && !usuarioActual.isEmpty() && !passActual.isEmpty()) {
                out.writeObject(new Mensaje("LOGIN", usuarioActual + ":" + passActual));
                out.flush();
                Mensaje r = (Mensaje) in.readObject();
                if (!r.getTipo().equals("LOGIN_OK")) isLogueado = false;
            }
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "No se puede conectar al servidor.");
            return false;
        }
    }

    // --- BOTÓN MÚSICA ARREGLADO ---
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

    // --- RESTO DE BOTONES ---
    private void btnJugarActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if (!asegurarConexion()) return;
        if (!isLogueado) { JOptionPane.showMessageDialog(this, "Inicia sesión primero."); return; }
        mostrarCarga(true, "BUSCANDO RIVAL");
        new Thread(() -> {
            try {
                enviar("BUSCAR_PARTIDA", "Clasico");
                while (true) {
                    Mensaje msg = (Mensaje) in.readObject();
                    if (msg.getTipo().equals("BUSCANDO")) SwingUtilities.invokeLater(() -> lblEstado.setText("ESPERANDO OPONENTE"));
                    else if (msg.getTipo().equals("RIVAL_ENCONTRADO")) {
                        String rival = (String) msg.getContenido();
                        SwingUtilities.invokeLater(() -> { mostrarCarga(false, ""); irAlJuego(rival); });
                        break;
                    } else if (msg.getTipo().equals("CANCELADO_OK")) {
                        SwingUtilities.invokeLater(() -> { mostrarCarga(false, ""); btnJugar.setEnabled(true); });
                        break;
                    } else if (msg.getTipo().equals("ERROR")) throw new Exception(msg.getContenido().toString());
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    mostrarCarga(false, "");
                    btnJugar.setEnabled(true);
                    if(!lblEstado.getText().startsWith("CANCELANDO")) JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
                    socket = null;
                });
            }
        }).start();
    }                                        

    private void irAlJuego(String rival) {
        Juego pj = (Juego) getParent().getComponent(1);
        pj.iniciarJuego(socket, out, in, rival, usuarioActual);
        this.socket = null; 
        CardLayout cl = (CardLayout) getParent().getLayout();
        cl.show(getParent(), "juego");
    }

    public void solicitarPartidaAutomatica() {
        SwingUtilities.invokeLater(() -> { if (isLogueado) btnJugar.doClick(); });
    }

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {                                         
        if (!asegurarConexion()) return;
        JTextField u = new JTextField(); JPasswordField p = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Usuario:", u, "Pass:", p}, "Login", 2) == 0) {
            try {
                enviar("LOGIN", u.getText() + ":" + new String(p.getPassword()));
                Mensaje r = (Mensaje) in.readObject();
                if (r.getTipo().equals("LOGIN_OK")) { isLogueado = true; usuarioActual = u.getText(); passActual = new String(p.getPassword()); JOptionPane.showMessageDialog(this, "Bienvenido " + usuarioActual); } 
                else JOptionPane.showMessageDialog(this, r.getContenido());
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error de conexión."); socket = null; }
        }
    }                                        

    private void btnRegistroActionPerformed(java.awt.event.ActionEvent evt) {                                            
        if (!asegurarConexion()) return;
        JTextField u = new JTextField(); JPasswordField p = new JPasswordField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Usuario:", u, "Pass:", p}, "Registro", 2) == 0) {
            try { enviar("REGISTRO", u.getText() + ":" + new String(p.getPassword())); JOptionPane.showMessageDialog(this, ((Mensaje)in.readObject()).getContenido()); } 
            catch (Exception e) { JOptionPane.showMessageDialog(this, "Error de conexión."); socket = null; }
        }
    }                                           

    private void btnRecordsActionPerformed(java.awt.event.ActionEvent evt) {                                           
        if (!asegurarConexion()) return;
        if (!isLogueado) { JOptionPane.showMessageDialog(this, "Inicia sesión para ver tus records."); return; }
        mostrarCarga(true, "CARGANDO...");
        new Thread(() -> {
            try {
                enviar("HISTORIAL", usuarioActual);
                Mensaje r = (Mensaje) in.readObject();
                if (r.getTipo().equals("HISTORIAL_LISTA")) {
                    List<String> h = (List<String>) r.getContenido();
                    SwingUtilities.invokeLater(() -> { mostrarCarga(false, ""); mostrarVentanaHistorial(h); });
                } else {
                    String msg = (String) r.getContenido();
                    SwingUtilities.invokeLater(() -> { mostrarCarga(false, ""); JOptionPane.showMessageDialog(this, msg); });
                }
            } catch (Exception e) { SwingUtilities.invokeLater(() -> mostrarCarga(false, "")); }
        }).start();
    }
    
    private void mostrarVentanaHistorial(List<String> historial) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Historial", true);
        dialog.setSize(700, 500);
        dialog.setLayout(new BorderLayout());
        JPanel panelLista = new JPanel();
        panelLista.setLayout(new BoxLayout(panelLista, BoxLayout.Y_AXIS));
        panelLista.setBackground(Color.WHITE);
        if (historial.isEmpty()) panelLista.add(new JLabel("No hay partidas."));
        else {
            for (String linea : historial) {
                String[] partes = linea.split("\\|");
                String infoVisual = partes[0];
                String archivoLog = (partes.length > 1) ? partes[1] : null;
                JPanel fila = new JPanel(new BorderLayout());
                fila.setMaximumSize(new Dimension(700, 45));
                fila.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
                fila.setBackground(Color.WHITE);
                JLabel lblTexto = new JLabel(infoVisual);
                JButton btnVer = new JButton("Ver Partida");
                btnVer.setBackground(new Color(52, 152, 219)); btnVer.setForeground(Color.WHITE); btnVer.setFocusable(false);
                if (archivoLog != null && !archivoLog.isEmpty()) btnVer.addActionListener(e -> verLogPartida(archivoLog, dialog));
                else { btnVer.setEnabled(false); btnVer.setBackground(Color.GRAY); }
                fila.add(lblTexto, BorderLayout.CENTER); fila.add(btnVer, BorderLayout.EAST);
                panelLista.add(fila);
            }
        }
        dialog.add(new JScrollPane(panelLista), BorderLayout.CENTER);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void verLogPartida(String archivoLog, JDialog parent) {
        parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                enviar("VER_LOG", archivoLog);
                Mensaje resp = (Mensaje) in.readObject();
                SwingUtilities.invokeLater(() -> {
                    parent.setCursor(Cursor.getDefaultCursor());
                    JTextArea area = new JTextArea((String)resp.getContenido());
                    area.setEditable(false);
                    JDialog d = new JDialog(parent, "Detalle", true);
                    d.setSize(500, 600); d.add(new JScrollPane(area));
                    d.setLocationRelativeTo(parent); d.setVisible(true);
                });
            } catch(Exception e) {
                SwingUtilities.invokeLater(() -> { parent.setCursor(Cursor.getDefaultCursor()); JOptionPane.showMessageDialog(parent, "Error leyendo log."); });
            }
        }).start();
    }

    private void enviar(String t, Object c) throws IOException {
        out.writeObject(new Mensaje(t, c)); out.flush(); out.reset();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gbc;
        jPanel1 = new javax.swing.JPanel(); jLabel1 = new javax.swing.JLabel(); btnMusic = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel(); jPanel6 = new javax.swing.JPanel();
        btnJugar = new javax.swing.JButton(); btnLogin = new javax.swing.JButton(); btnRegistro = new javax.swing.JButton(); btnRecords = new javax.swing.JButton();
        setBackground(new java.awt.Color(92, 103, 125)); setLayout(new java.awt.BorderLayout());
        jPanel1.setBackground(new java.awt.Color(92, 103, 125)); jPanel1.setOpaque(false); jPanel1.setLayout(new java.awt.GridBagLayout());
        jLabel1.setFont(new java.awt.Font("Segoe UI", 3, 48)); jLabel1.setForeground(Color.WHITE); jLabel1.setText("Buscaminas");
        try { jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/proyectobuscaminas/imagenes/intentodelogo1.png"))); jLabel1.setText(""); } catch(Exception e){}
        jPanel1.add(jLabel1, new java.awt.GridBagConstraints());
        btnMusic.setFont(new java.awt.Font("Comic Sans MS", 1, 24)); btnMusic.setText("ON");
        btnMusic.addActionListener(e -> btnMusicActionPerformed(e)); jPanel1.add(btnMusic, new java.awt.GridBagConstraints());
        add(jPanel1, java.awt.BorderLayout.NORTH);
        jPanel4.setOpaque(false); jPanel4.setLayout(new java.awt.GridBagLayout());
        jPanel6.setOpaque(false); jPanel6.setLayout(null); jPanel6.setPreferredSize(new Dimension(360, 400));
        btnJugar.setBackground(Color.WHITE); btnJugar.setFont(new Font("Comic Sans MS", 1, 36)); btnJugar.setText("Jugar");
        btnJugar.setBounds(20, 20, 320, 55); btnJugar.addActionListener(e -> btnJugarActionPerformed(e)); jPanel6.add(btnJugar);
        btnLogin.setBackground(Color.WHITE); btnLogin.setFont(new Font("Comic Sans MS", 1, 36)); btnLogin.setText("Iniciar Sesion");
        btnLogin.setBounds(20, 110, 320, 58); btnLogin.addActionListener(e -> btnLoginActionPerformed(e)); jPanel6.add(btnLogin);
        btnRegistro.setBackground(Color.WHITE); btnRegistro.setFont(new Font("Comic Sans MS", 1, 35)); btnRegistro.setText("Registrar usuario");
        btnRegistro.setBounds(20, 210, 320, 57); btnRegistro.addActionListener(e -> btnRegistroActionPerformed(e)); jPanel6.add(btnRegistro);
        btnRecords.setBackground(Color.WHITE); btnRecords.setFont(new Font("Comic Sans MS", 1, 36)); btnRecords.setText("Records");
        btnRecords.setBounds(20, 300, 320, 58); btnRecords.addActionListener(e -> btnRecordsActionPerformed(e)); jPanel6.add(btnRecords);
        jPanel4.add(jPanel6, new java.awt.GridBagConstraints());
        add(jPanel4, java.awt.BorderLayout.CENTER);
        JPanel p2 = new javax.swing.JPanel(); p2.setOpaque(false); add(p2, java.awt.BorderLayout.EAST);
        JPanel p3 = new javax.swing.JPanel(); p3.setOpaque(false); add(p3, java.awt.BorderLayout.WEST);
    }
    private javax.swing.JButton btnJugar, btnLogin, btnMusic, btnRecords, btnRegistro;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1, jPanel4, jPanel6;
}