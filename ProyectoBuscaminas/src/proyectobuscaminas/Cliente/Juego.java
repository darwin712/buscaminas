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
    Random random = new Random();
    
    private boolean esMiTurno = false;
    private volatile boolean running = false;
    private int filas = 10;
    private int columnas = 10;

    private Timer timerJuego;
    private int segundos = 0;
    private boolean minasVisibles = false; // Estado del truco
    

    private final Color COLOR_MIO = new Color(46, 204, 113); // Verde
    private final Color COLOR_RIVAL = new Color(231, 76, 60); // Rojo
    
    public Juego() {
        initComponents();
    }

    private void toggleMinasDemo() {
        if (tableroLogico == null || botones == null) return;
        
        minasVisibles = !minasVisibles;
        
        if(minasVisibles) {
            btnMostrarMinas.setText("Ocultar Minas");
            btnMostrarMinas.setBackground(new Color(255, 100, 100));
        } else {
            btnMostrarMinas.setText("Mostrar Minas");
            btnMostrarMinas.setBackground(Color.ORANGE);
        }
        
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                Casilla c = tableroLogico.getCasilla(i, j);
                if (c.isMina() && botones[i][j].isEnabled()) {
                    if(minasVisibles) {
                        botones[i][j].setBackground(Color.MAGENTA);
                        botones[i][j].setText("💣");
                        botones[i][j].setForeground(Color.WHITE);
                    } else {
                        botones[i][j].setBackground(new Color(230, 230, 230));
                        botones[i][j].setText("");
                        botones[i][j].setForeground(Color.BLACK);
                    }
                }
            }
        }
        this.requestFocusInWindow();
    }

    public void iniciarJuego(Socket s, ObjectOutputStream o, ObjectInputStream i, String rival, String yo) {
        if (running) return;
        if(Musica.getInstance().isPlaying() == true && Musica.getInstance().wasPlayedOnce() == true){
            elegirMusica();
        }
        
        this.socket = s;
        this.out = o;
        this.in = i;
        this.running = true;
        this.esMiTurno = false;
        this.minasVisibles = false;
        
        btnMostrarMinas.setText("Mostrar Minas");
        btnMostrarMinas.setBackground(Color.ORANGE);
        
        panelTablero1.removeAll();
        panelTablero1.setVisible(false);
        
        jLabel2.setText("Conectando...");
        jLabel2.setOpaque(true);
        jLabel2.setBackground(Color.GRAY);
        jLabel2.setForeground(Color.WHITE);
        
        jLabel5.setText(yo + " VS " + rival);
        
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
                Object[] opciones = {"Principiante (8x8)", "Normal (10x10)", "Experto (12x12)"};
                JLabel lbl = new JLabel("<html><h3>¡Rival Encontrado!</h3>Selecciona dificultad:</html>");
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                
                int sel = JOptionPane.showOptionDialog(this, lbl, "Votación", 
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opciones, opciones[1]);

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
                Musica.getInstance().playSFX("recursos/Yay.ogg");
            case "PERDISTE":
                timerJuego.stop();
                running = false;
                
                String titulo = msg.getTipo().equals("GANASTE") ? "¡VICTORIA!" : "DERROTA";
                int r = JOptionPane.showConfirmDialog(this, 
                        "<html><h2>" + titulo + "</h2><br>¿Jugar otra vez?</html>", 
                        "Fin de Partida", JOptionPane.YES_NO_OPTION);
                
                try { socket.close(); } catch(IOException e){}
                btnRegresar.doClick();
                
                if (r == JOptionPane.YES_OPTION) {
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
                    if (btn.getText().equals("🚩")) return;
                    if(esMiTurno && !tableroLogico.getCasilla(f, c).esRevelado()) {
                        if(tableroLogico.getCasilla(f, c).isMina()){
                            enviar("PERDIO", null);
                            Musica.getInstance().playSFX("recursos/Bomb.ogg");
                        }else{
                            enviar("CLICK", f + "," + c);
                            Musica.getInstance().playSFX("recursos/Click2.ogg");
                        }
                    }
                });
                
                btn.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        Musica.getInstance().playSFX("recursos/Flag.ogg");
                        if (SwingUtilities.isRightMouseButton(e) && btn.isEnabled()) {
                            // Si las minas están visibles por el truco, no poner banderas
                            if(minasVisibles && btn.getText().equals("💣")) return;

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
        CardLayout cl = (CardLayout) getParent().getLayout();
        if(Musica.getInstance().isPlaying() == true){
            Musica.getInstance().stopMusic();
            Musica.getInstance().playMusic("recursos/MainTheme.ogg");
        }
        cl.show(getParent(), "menu");
    }
    
    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {
        try { socket.close(); } catch(IOException e){}
        salirLimpiamente();
    }
    
    private void elegirMusica(){
        int canciones = 6;

        int opcion = random.nextInt(canciones);

        switch(opcion){
            case 0:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/ChaozFantasy.ogg");
                break;

            case 1:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/Roblox.ogg");
                break;
                
            case 2:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/FireAura.ogg");
                break;
                
            case 3:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/MyHeart.ogg");
                break;
                
            case 4:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/Stardust.ogg");
                break;
                
            case 5:
                Musica.getInstance().stopMusic();
                Musica.getInstance().playMusic("recursos/FireEmblem.ogg");
                break;

            default:
                System.out.println("Error inesperado");
        }
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 629, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(797, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel5)
                .addContainerGap(22, Short.MAX_VALUE))
        );
        add(jPanel1, java.awt.BorderLayout.PAGE_START);

        jPanel3.setBackground(new java.awt.Color(92, 103, 125));
        jLabel1.setBackground(new java.awt.Color(51, 51, 51));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 24)); 
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Estado");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); 
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Esperando...");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addContainerGap(493, Short.MAX_VALUE))
        );
        add(jPanel3, java.awt.BorderLayout.LINE_START);

        jPanel4.setBackground(new java.awt.Color(92, 103, 125));

        btnRegresar.setFont(new java.awt.Font("Segoe UI", 1, 14)); 
        btnRegresar.setText("Salir");
        btnRegresar.setBackground(new Color(231, 76, 60)); 
        btnRegresar.setForeground(Color.WHITE);
        btnRegresar.addActionListener(e -> btnRegresarActionPerformed(e));

        btnMostrarMinas.setFont(new java.awt.Font("Segoe UI", 1, 12));
        btnMostrarMinas.setText("Mostrar Minas");
        btnMostrarMinas.setBackground(Color.ORANGE);
        btnMostrarMinas.setForeground(Color.BLACK);
        btnMostrarMinas.addActionListener(e -> toggleMinasDemo());

        jLabel6.setBackground(new java.awt.Color(51, 51, 51));
        jLabel6.setFont(new java.awt.Font("Segoe UI", 2, 24)); 
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Tools");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(btnRegresar, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                            .addComponent(btnMostrarMinas, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE))
                        .addGap(36, 36, 36))))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnRegresar, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnMostrarMinas, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(388, Short.MAX_VALUE))
        );
        add(jPanel4, java.awt.BorderLayout.LINE_END);

        jPanel2.setBackground(new java.awt.Color(92, 103, 125));
        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); 
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Mi tiempo:");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); 
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("00:00");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addContainerGap(1353, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addContainerGap(32, Short.MAX_VALUE))
        );
        add(jPanel2, java.awt.BorderLayout.PAGE_END);

        jPanel5.setBackground(new java.awt.Color(92, 103, 125));
        jPanel5.setLayout(new java.awt.BorderLayout());
        
        jPanel6.setBackground(new java.awt.Color(92, 103, 125));
        jPanel6.setPreferredSize(new Dimension(40,0)); 
        jPanel5.add(jPanel6, java.awt.BorderLayout.WEST);
        
        jPanel7.setBackground(new java.awt.Color(92, 103, 125));
        jPanel7.setPreferredSize(new Dimension(40,0)); 
        jPanel5.add(jPanel7, java.awt.BorderLayout.EAST);

        panelTablero1.setBackground(new java.awt.Color(255, 255, 255));
        panelTablero1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(204, 204, 204), 6, true));
        panelTablero1.setMaximumSize(new java.awt.Dimension(450, 450));
        panelTablero1.setMinimumSize(new java.awt.Dimension(450, 450));

        javax.swing.GroupLayout panelTablero1Layout = new javax.swing.GroupLayout(panelTablero1);
        panelTablero1.setLayout(panelTablero1Layout);
        panelTablero1Layout.setHorizontalGroup(
            panelTablero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 665, Short.MAX_VALUE)
        );
        panelTablero1Layout.setVerticalGroup(
            panelTablero1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 560, Short.MAX_VALUE)
        );

        jPanel5.add(panelTablero1, java.awt.BorderLayout.CENTER);
        add(jPanel5, java.awt.BorderLayout.CENTER);
    }

    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton btnMostrarMinas;
    private javax.swing.JLabel jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6, jLabel7, jLabel8;
    private javax.swing.JPanel jPanel1, jPanel2, jPanel3, jPanel4, jPanel5, jPanel6, jPanel7, panelTablero1;
}