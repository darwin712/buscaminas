package proyectobuscaminas;

import java.util.Random;

public class GeneradorTableros {

    public Tablero[] generarTableros(String dificultad) {
        int filas = 10;
        int columnas = 10;
        int minas = 10;

       
        try {
            int tam = Integer.parseInt(dificultad);
            filas = tam;
            columnas = tam;
            
            if (tam == 8) minas = 10;
            else if (tam == 10) minas = 15;
            else if (tam == 12) minas = 25;
            
        } catch (NumberFormatException e) {
            System.out.println("Error en dificultad, usando por defecto 10x10");
        }

       
        Tablero t1 = new Tablero(filas, columnas, 0); 
        Tablero t2 = new Tablero(filas, columnas, 0);
        
        Random random = new Random();

    
        int minasPuestas1 = 0;
        while (minasPuestas1 < minas) {
            int f = random.nextInt(filas);
            int c = random.nextInt(columnas);
            
            
            if (!t1.getCasilla(f, c).isMina()) {
                t1.getCasilla(f, c).setMina(true);
                minasPuestas1++;
            }
        }

       
        int minasPuestas2 = 0;
        while (minasPuestas2 < minas) {
            int f = random.nextInt(filas);
            int c = random.nextInt(columnas);
            
            if (!t2.getCasilla(f, c).isMina()) {
                t2.getCasilla(f, c).setMina(true);
                minasPuestas2++;
            }
        }
        
        // 4. Calcular pistas
        t1.calcularVecinos();
        t2.calcularVecinos();

        return new Tablero[]{t1, t2};
    }
}