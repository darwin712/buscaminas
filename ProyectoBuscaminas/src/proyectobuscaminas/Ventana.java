package proyectobuscaminas;

import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Ventana extends javax.swing.JFrame {
    

    private JPanel mainPanel;
    private CardLayout cardLayout;

    public Ventana() {
    
        setTitle("Buscaminas Online");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        
  
        initComponents();
        
    
        if (!Musica.getInstance().isPlaying() && !Musica.getInstance().wasPlayedOnce()) {
            Musica.getInstance().playMusic("recursos/Tobu-Infectious.ogg");
            Musica.getInstance().setWasPlayedOnce(true);
        }
    }

    private void initComponents() {
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Instanciamos las vistas
        Menu menuPanel = new Menu();
        Juego juegoPanel = new Juego();

       
        mainPanel.add(menuPanel, "menu");
        mainPanel.add(juegoPanel, "juego");

        // Agregamos el panel principal a la ventana
        this.getContentPane().add(mainPanel);

        // Mostramos el menú por defecto al arrancar
        cardLayout.show(mainPanel, "menu");
        
        pack(); 
    }

    public static void main(String args[]) {
        
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Ventana.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        // Crear y mostrar la ventana en el hilo de eventos de Swing
        java.awt.EventQueue.invokeLater(() -> {
            new Ventana().setVisible(true);
        });
    }
}