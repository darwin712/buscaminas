package proyectobuscaminas.Cliente;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import javax.swing.*;
import proyectobuscaminas.Comunes.Mensaje;
import proyectobuscaminas.Comunes.Musica;
import proyectobuscaminas.Tablero.Casilla;
import proyectobuscaminas.Tablero.Tablero;

public class Juego extends javax.swing.JPanel {
    
    private JButton[][] botones;
    private Tablero tableroLogico;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    private boolean esMiTurno = false;
    private volatile boolean running = false;
    private int filas = 10;
    private int columnas = 10;
    
    private Timer timerJuego;
    private int segundos = 0;
    
    private final Color COLOR_FONDO = new Color(92, 103, 125);
    private final Color COLOR_MIO = new Color(46, 204, 113); 
    private final Color COLOR_RIVAL = new Color(231, 76, 60); 
    
    public Juego() {
        initComponents();
    }

    private void ponerMusicaAleatoria() {
        Random random = new Random();
        int canciones = 6;
        int opcion = random.nextInt(canciones);

        Musica.getInstance().stopMusic();

        switch(opcion){
            case 0: Musica.getInstance().playMusic("recursos/ChaozFantasy.ogg"); break;
            case 1: Musica.getInstance().playMusic("recursos/Roblox.ogg"); break;
            case 2: Musica.getInstance().playMusic("recursos/FireAura.ogg"); break;
            case 3: Musica.getInstance().playMusic("recursos/MyHeart.ogg"); break;
            case 4: Musica.getInstance().playMusic("recursos/Stardust.ogg"); break;
            case 5: Musica.getInstance().playMusic("recursos/FireEmblem.ogg"); break;
            default: System.out.println("Error inesperado en música");
        }
    }

    private void mostrarMinasDemo() {
        if (tableroLogico == null || botones == null) return;
        
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                Casilla c = tableroLogico.getCasilla(i, j);
                if (c.isMina() && botones[i][j].isEnabled()) {
                    botones[i][j].setBackground(Color.MAGENTA);
                    botones[i][j].setText("💣");
                    botones[i][j].setForeground(Color.WHITE);
                }
            }
        }
        this.requestFocusInWindow();
    }

    public void iniciarJuego(Socket s, ObjectOutputStream o, ObjectInputStream i, String rival, String yo) {
        if (running) return;
        
        this.socket = s;
        this.out = o;
        this.in = i;
        this.running = true;
        this.esMiTurno = false;
        
        ponerMusicaAleatoria();
        
        panelTablero1.removeAll();
        panelTablero1.setVisible(false);
        
        jLabel2.setText("Conectando...");
        jLabel2.setOpaque(true);
        jLabel2.setBackground(Color.GRAY);
        jLabel2.setForeground(Color.WHITE);
        
        jLabel5.setText(yo + " VS " + rival);
        jLabel5.setFont(new Font("Segoe UI", Font.BOLD, 32));
        
        if (timerJuego != null) timerJuego.stop();
        segundos = 0;
        jLabel4.setText("00:00");
        timerJuego = new Timer(1000, e -> actualizarReloj());
        
        new Thread(this::bucleEscucha).start();
    }
    
    private void actualizarReloj() {
        segundos++;
        int min = segundos / 60;
        int sec = segundos % 60;
        jLabel4.setText(String.format("%02d:%02d", min, sec));
    }
    
    private void bucleEscucha() {
        try {
            while (running && !socket.isClosed()) {
                Mensaje msg = (Mensaje) in.readObject();
                SwingUtilities.invokeLater(() -> procesarMensaje(msg));
            }
        } catch (Exception e) {
            if (running) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Conexión perdida.");
                    salirLimpiamente();
                });
            }
        } finally {
            running = false;
        }
    }

    public void procesarMensaje(Mensaje msg) {
        if (!running) return;

        switch (msg.getTipo()) {
            case "VOTACION":
                Object[] opciones = {"🟢 Fácil (8x8)", "🟡 Intermedio (10x10)", "🔴 Difícil (12x12)"};
                JLabel lblMensaje = new JLabel("<html><h2>¡Rival Encontrado!</h2><p>Selecciona la dificultad del mapa:</p></html>");
                lblMensaje.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                int sel = JOptionPane.showOptionDialog(this, 
                        lblMensaje, 
                        "Votación de Mapa", 
                        JOptionPane.DEFAULT_OPTION, 
                        JOptionPane.QUESTION_MESSAGE, 
                        null, 
                        opciones, 
                        opciones[1]);

                String voto = "10";
                if (sel == 0) voto = "8";
                if (sel == 2) voto = "12";

                enviar("ENVIAR_VOTO", voto);
                jLabel2.setText("Esperando al rival...");
                jLabel2.setBackground(Color.DARK_GRAY);
                break;

            case "TAMANO":
                int tam = Integer.parseInt((String)msg.getContenido());
                this.filas = tam;
                this.columnas = tam;
                break;

            case "INICIO":
                this.tableroLogico = (Tablero) msg.getContenido();
                crearTableroVisual();
                panelTablero1.setVisible(true);
                timerJuego.start();
                break;
                
            case "TURNO":
                esMiTurno = (boolean) msg.getContenido();
                if (esMiTurno) {
                    jLabel2.setText(" TU TURNO ");
                    jLabel2.setBackground(COLOR_MIO);
                    jLabel2.setForeground(Color.BLACK);
                } else {
                    jLabel2.setText(" TURNO RIVAL ");
                    jLabel2.setBackground(COLOR_RIVAL);
                    jLabel2.setForeground(Color.WHITE);
                }
                break;
                
            case "CASILLA_ACTUALIZADA":
                String[] d = ((String)msg.getContenido()).split(",");
                actualizarVisual(Integer.parseInt(d[0]), Integer.parseInt(d[1]), Integer.parseInt(d[2]));
                break;
                
            case "MINAS":
                String[] minas = ((String)msg.getContenido()).split(";");
                for(String m : minas) {
                    if(!m.isEmpty()) {
                        String[] c = m.split(",");
                        int f = Integer.parseInt(c[0]);
                        int col = Integer.parseInt(c[1]);
                        if(botones != null) {
                             botones[f][col].setBackground(new Color(255, 80, 80));
                             botones[f][col].setText("💣");
                             botones[f][col].setEnabled(false);
                             botones[f][col].setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                             botones[f][col].setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        }
                    }
                }
                break;

            case "GANASTE":
            case "PERDISTE":
                timerJuego.stop();
                running = false;
                
                String tituloFin = msg.getTipo().equals("GANASTE") ? "🏆 ¡VICTORIA!" : "💀 DERROTA";
                String msjFin = (String) msg.getContenido();
                
                int res = JOptionPane.showConfirmDialog(this, 
                        "<html><h3>" + msjFin + "</h3><br>¿Quieres jugar otra vez?</html>", 
                        tituloFin, 
                        JOptionPane.YES_NO_OPTION);
                
                try { socket.close(); } catch(IOException e){}
                btnRegresar.doClick();
                
                if (res == JOptionPane.YES_OPTION) {
                    Container parent = getParent();
                    for(Component c : parent.getComponents()) {
                        if(c instanceof Menu) ((Menu)c).solicitarPartidaAutomatica();
                    }
                }
                break;
                
            case "OPONENTE_DESCONECTADO":
                timerJuego.stop();
                JOptionPane.showMessageDialog(this, "El rival se ha desconectado. ¡Ganaste!");
                try { socket.close(); } catch(IOException e){}
                salirLimpiamente();
                break;
        }
    }

    private void crearTableroVisual() {
        panelTablero1.removeAll();
        panelTablero1.setLayout(new GridLayout(filas, columnas));
        botones = new JButton[filas][columnas];
        
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                JButton btn = new JButton();
                btn.setFocusable(false);
                btn.setMargin(new Insets(0,0,0,0));
                btn.setBackground(new Color(230, 230, 230));
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                btn.setFont(new Font("Arial", Font.BOLD, 14));
                
                final int f = i;
                final int c = j;
                
                btn.addActionListener(e -> {
                    // SONIDO AL CLICK IZQUIERDO
                    Musica.getInstance().playSFX("recursos/Click2.ogg");
                    
                    if (btn.getText().equals("🚩")) return;
                    
                    if(esMiTurno && !tableroLogico.getCasilla(f, c).esRevelado()) {
                        if(tableroLogico.getCasilla(f, c).isMina()) enviar("PERDIO", null);
                        else enviar("CLICK", f + "," + c);
                    }
                });
                
                btn.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e) && btn.isEnabled()) {
                            // SONIDO AL CLICK DERECHO
                            Musica.getInstance().playSFX("recursos/Click2.ogg");
                            
                            if (btn.getText().equals("🚩")) {
                                btn.setText("");
                                btn.setForeground(Color.BLACK);
                            } else {
                                btn.setText("🚩");
                                btn.setForeground(Color.ORANGE);
                                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                            }
                        }
                    }
                });
                
                botones[i][j] = btn;
                panelTablero1.add(btn);
            }
        }
        panelTablero1.revalidate();
        panelTablero1.repaint();
    }
    
    private void actualizarVisual(int f, int c, int v) {
        if(botones == null) return;
        JButton btn = botones[f][c];
        
        for(ActionListener al : btn.getActionListeners()) btn.removeActionListener(al);
        for(MouseListener ml : btn.getMouseListeners()) btn.removeMouseListener(ml);
        
        btn.setBackground(new Color(220, 220, 220));
        btn.setText(""); 
        
        if(v > 0) {
            btn.setText(String.valueOf(v));
            btn.setFont(new Font("Segoe UI Black", Font.BOLD, 16));
            switch (v) {
                case 1: btn.setForeground(Color.BLUE); break;
                case 2: btn.setForeground(new Color(0, 128, 0)); break;
                case 3: btn.setForeground(Color.RED); break;
                case 4: btn.setForeground(new Color(0, 0, 128)); break;
                case 5: btn.setForeground(new Color(128, 0, 0)); break;
                case 6: btn.setForeground(new Color(0, 128, 128)); break;
                case 7: btn.setForeground(Color.BLACK); break;
                case 8: btn.setForeground(Color.GRAY); break;
            }
        }
        tableroLogico.getCasilla(f, c).setRevelado(true);
    }

    private void enviar(String t, Object c) {
        try {
            out.writeObject(new Mensaje(t, c));
            out.flush();
            out.reset();
        } catch(IOException e) { e.printStackTrace(); }
    }
    
    private void salirLimpiamente() {
        running = false;
        Musica.getInstance().stopMusic();
        Musica.getInstance().playMusic("recursos/MainTheme.ogg");
        CardLayout cl = (CardLayout) getParent().getLayout();
        cl.show(getParent(), "menu");
    }
    
    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {
        try { socket.close(); } catch(IOException e){}
        salirLimpiamente();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        btnRegresar = new javax.swing.JButton();
        btnMostrarMinas = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        panelTablero1 = new javax.swing.JPanel();

        setBackground(new java.awt.Color(92, 103, 125));
        setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(92, 103, 125));
        jLabel5.setFont(new java.awt.Font("Segoe UI", 3, 48)); 
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel5.setText("Buscaminas");
        jPanel1.add(jLabel5);
        add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBackground(new java.awt.Color(92, 103, 125));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 24)); 
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Estado");
        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 16)); 
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Esperando...");
        jPanel3.add(jLabel1); jPanel3.add(jLabel2);
        add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setBackground(new java.awt.Color(92, 103, 125));
        
        jLabel6.setFont(new java.awt.Font("Segoe UI", 2, 24)); 
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Tools");
        
        btnRegresar.setFont(new java.awt.Font("Segoe UI", 1, 14)); 
        btnRegresar.setText("Salir");
        btnRegresar.setBackground(new Color(231, 76, 60)); 
        btnRegresar.setForeground(Color.WHITE);
        btnRegresar.setFocusable(false);
        btnRegresar.setMaximumSize(new Dimension(150, 40));
        btnRegresar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRegresar.addActionListener(e -> btnRegresarActionPerformed(e));
        
        btnMostrarMinas.setFont(new java.awt.Font("Segoe UI", 1, 12));
        btnMostrarMinas.setText("Mostrar Minas");
        btnMostrarMinas.setBackground(Color.ORANGE);
        btnMostrarMinas.setForeground(Color.BLACK);
        btnMostrarMinas.setFocusable(false);
        btnMostrarMinas.setMaximumSize(new Dimension(150, 40));
        btnMostrarMinas.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMostrarMinas.addActionListener(e -> mostrarMinasDemo());
        
        jPanel4.add(jLabel6);
        jPanel4.add(Box.createVerticalStrut(20));
        jPanel4.add(btnRegresar);
        jPanel4.add(Box.createVerticalStrut(10));
        jPanel4.add(btnMostrarMinas);
        
        add(jPanel4, java.awt.BorderLayout.LINE_END);

        jPanel2.setBackground(new java.awt.Color(92, 103, 125));
        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); 
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Tiempo:");
        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); 
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("00:00");
        jPanel2.add(jLabel3); jPanel2.add(jLabel4);
        add(jPanel2, java.awt.BorderLayout.PAGE_END);

        jPanel5.setBackground(new java.awt.Color(92, 103, 125));
        jPanel5.setLayout(new java.awt.BorderLayout());
        jPanel6.setBackground(new java.awt.Color(92, 103, 125)); jPanel6.setPreferredSize(new Dimension(40,0)); 
        jPanel5.add(jPanel6, java.awt.BorderLayout.WEST);
        jPanel7.setBackground(new java.awt.Color(92, 103, 125)); jPanel7.setPreferredSize(new Dimension(40,0)); 
        jPanel5.add(jPanel7, java.awt.BorderLayout.EAST);
        panelTablero1.setBackground(Color.WHITE);
        panelTablero1.setBorder(BorderFactory.createLineBorder(new Color(204,204,204), 6));
        jPanel5.add(panelTablero1, java.awt.BorderLayout.CENTER);
        add(jPanel5, java.awt.BorderLayout.CENTER);
    }
    
    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton btnMostrarMinas;
    private javax.swing.JLabel jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, jLabel7, jLabel8;
    private javax.swing.JPanel jPanel1, jPanel2, jPanel3, jPanel4, jPanel5, jPanel6, jPanel7, panelTablero1;
}


