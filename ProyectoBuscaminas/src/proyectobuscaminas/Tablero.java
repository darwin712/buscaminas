/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas;

import java.io.Serializable;

/**
 *
 * @author davek
 */
public class Tablero implements Serializable{
    private int filas;
    private int columnas;
    private int numMinas;
    private Casilla[][] casillas;

    public Tablero(int filas, int columnas, int numMinas) {
        this.filas = filas;
        this.columnas = columnas;
        this.numMinas = numMinas;
        this.casillas = new Casilla[filas][columnas];
        inicializarCasillas();
        colocarMinas();
        calcularVecinos();
    }

    public Tablero(Tablero t) {
        this(t.filas, t.columnas, t.numMinas);
    }

    private void inicializarCasillas() {
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                casillas[i][j] = new Casilla();
            }
        }
    }

    private void colocarMinas() {
        int minasColocadas = 0;
        while (minasColocadas < numMinas) {
            int f = (int) (Math.random() * filas);
            int c = (int) (Math.random() * columnas);
            if (!casillas[f][c].isMina()) {
                casillas[f][c].setMina(true);
                minasColocadas++;
            }
        }
    }

    public void calcularVecinos() {
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                if (!casillas[i][j].isMina()) {
                    casillas[i][j].setVecinos(contarMinasAlrededor(i, j));
                }
            }
        }
    }

    private int contarMinasAlrededor(int f, int c) {
        int minas = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int nf = f + i;
                int nc = c + j;
                if (nf >= 0 && nf < filas && nc >= 0 && nc < columnas) {
                    if (casillas[nf][nc].isMina()) {
                        minas++;
                    }
                }
            }
        }
        return minas;
    }

    public Casilla getCasilla(int f, int c) {
        return casillas[f][c];
    }

    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }
}
