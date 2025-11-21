package proyectobuscaminas;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;

public class Juego extends javax.swing.JPanel {
    
    private JButton[][] botones;
    private Tablero tableroLogico;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean esMiTurno = false;
    
    private JPanel panelTablero;
    private JLabel lblEstado;
    private JLabel lblTurno; // Nuevo indicador visual

    public Juego() {
        setLayout(new BorderLayout());
        setBackground(Color.DARK_GRAY);

        // Panel Superior (Info)
        JPanel top = new JPanel();
        lblTurno = new JLabel("Esperando conexión...");
        lblTurno.setFont(new Font("Arial", Font.BOLD, 18));
        top.add(lblTurno);
        add(top, BorderLayout.NORTH);

        // Panel Central (Tablero y Estado)
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setBackground(Color.DARK_GRAY);
        
        panelTablero = new JPanel();
        panelTablero.setVisible(false);
        centro.add(panelTablero);
        
        lblEstado = new JLabel("Esperando oponente...", SwingConstants.CENTER);
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblEstado.setForeground(Color.WHITE);
        centro.add(lblEstado);

        add(centro, BorderLayout.CENTER);
    }

    public void iniciarJuegoConConexion(Socket s, ObjectOutputStream o, ObjectInputStream i) {
        this.socket = s;
        this.out = o;
        this.in = i;
        
        lblEstado.setVisible(true);
        panelTablero.setVisible(false);
        lblTurno.setText("Buscando partida...");

        // Hilo de escucha del Cliente
        new Thread(() -> {
            try {
                while (true) {
                    Mensaje msg = (Mensaje) in.readObject();
                    procesarMensaje(msg);
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Conexión perdida con el servidor.");
                    regresarAlMenu();
                });
            }
        }).start();
    }

    private void procesarMensaje(Mensaje msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getTipo()) {
                case "INICIO":
                    tableroLogico = (Tablero) msg.getContenido();
                    crearTableroVisual();
                    lblEstado.setVisible(false);
                    panelTablero.setVisible(true);
                    break;
                    
                case "TURNO":
                    esMiTurno = (boolean) msg.getContenido();
                    lblTurno.setText(esMiTurno ? "TU TURNO" : "TURNO DEL OPONENTE");
                    lblTurno.setForeground(esMiTurno ? Color.GREEN : Color.RED);
                    bloquearBotonesSegunTurno();
                    break;
                    
                case "CASILLA_ACTUALIZADA":
                    String[] datos = ((String) msg.getContenido()).split(",");
                    actualizarCasillaRemota(Integer.parseInt(datos[0]), Integer.parseInt(datos[1]), Integer.parseInt(datos[2]));
                    break;
                    
                case "GANASTE":
                case "PERDISTE":
                    String texto = msg.getTipo().equals("GANASTE") ? "¡VICTORIA!" : "DERROTA :(";
                    int op = JOptionPane.showConfirmDialog(this, texto + "\n" + msg.getContenido() + "\n¿Jugar otra vez?", "Fin de Partida", JOptionPane.YES_NO_OPTION);
                    
                    if (op == JOptionPane.YES_OPTION) {
                        enviarMensaje("BUSCAR_PARTIDA", "Clasico");
                        panelTablero.setVisible(false);
                        lblEstado.setText("Buscando nuevo oponente...");
                        lblEstado.setVisible(true);
                    } else {
                        regresarAlMenu();
                    }
                    break;
                    
                case "OPONENTE_DESCONECTADO":
                     JOptionPane.showMessageDialog(this, "El oponente se desconectó.");
                     regresarAlMenu();
                     break;
            }
        });
    }

    private void crearTableroVisual() {
        panelTablero.removeAll();
        int f = tableroLogico.getFilas();
        int c = tableroLogico.getColumnas();
        panelTablero.setLayout(new GridLayout(f, c));
        botones = new JButton[f][c];
        
        // Tamaño fijo para evitar que se vea gigante
        panelTablero.setPreferredSize(new Dimension(c * 35, f * 35));

        for (int i = 0; i < f; i++) {
            for (int j = 0; j < c; j++) {
                JButton btn = new JButton();
                btn.setMargin(new Insets(0,0,0,0));
                btn.setFont(new Font("Arial", Font.BOLD, 12));
                final int r = i;
                final int col = j;
                btn.addActionListener(e -> clickCasilla(r, col));
                botones[i][j] = btn;
                panelTablero.add(btn);
            }
        }
        
        panelTablero.revalidate();
        panelTablero.repaint();
        
        // Forzar empaquetado
        Window w = SwingUtilities.getWindowAncestor(this);
        if(w instanceof JFrame) ((JFrame)w).pack();
    }

    private void clickCasilla(int f, int c) {
        if (!esMiTurno) return;
        
        Casilla casilla = tableroLogico.getCasilla(f, c);
        if (casilla.esRevelado()) return;

        // Acción local inmediata para feedback visual
        casilla.setRevelado(true);
        botones[f][c].setEnabled(false);
        botones[f][c].setBackground(Color.LIGHT_GRAY);

        if (casilla.isMina()) {
            botones[f][c].setBackground(Color.RED);
            botones[f][c].setText("X");
            enviarMensaje("PERDIO", null);
        } else {
            if(casilla.getVecinos() > 0) {
                botones[f][c].setText("" + casilla.getVecinos());
                colorearNumero(botones[f][c], casilla.getVecinos());
            }
            enviarMensaje("CLICK", f + "," + c);
        }
    }
    
    private void actualizarCasillaRemota(int f, int c, int vecinos) {
        if(f >= 0 && f < botones.length && c >= 0 && c < botones[0].length) {
             botones[f][c].setEnabled(false);
             botones[f][c].setBackground(Color.LIGHT_GRAY);
             if(vecinos > 0) {
                 botones[f][c].setText(String.valueOf(vecinos));
                 colorearNumero(botones[f][c], vecinos);
             }
        }
    }
    
    private void colorearNumero(JButton btn, int n) {
        switch(n) {
            case 1: btn.setForeground(Color.BLUE); break;
            case 2: btn.setForeground(new Color(0, 128, 0)); break; // Verde oscuro
            case 3: btn.setForeground(Color.RED); break;
            default: btn.setForeground(new Color(0, 0, 128)); break;
        }
    }

    private void bloquearBotonesSegunTurno() {
        if (botones == null) return;
        // Solo cambiamos la interactividad global del panel, no botón por botón para no afectar a los revelados
        // Pero Swing JButton enabled es visual.
        // La protección real está en clickCasilla: if (!esMiTurno) return;
    }

    private void enviarMensaje(String tipo, Object contenido) {
        try {
            if (out != null) {
                out.writeObject(new Mensaje(tipo, contenido));
                out.flush();
                out.reset(); // CRÍTICO
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void regresarAlMenu() {
        try { socket.close(); } catch(Exception e){}
        CardLayout cl = (CardLayout) getParent().getLayout();
        cl.show(getParent(), "menu");
    }
}