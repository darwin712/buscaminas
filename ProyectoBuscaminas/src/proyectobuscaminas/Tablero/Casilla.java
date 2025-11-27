/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas.Tablero;

import java.io.Serializable;

/**
 *
 * @author davek
 */
public class Casilla implements Serializable {
    private boolean mina;
    private boolean esRevelado;
    private boolean bandera;
    private int vecinos;

    public Casilla() {
        this.mina = false;
        this.esRevelado = false;
        this.bandera = false;
        this.vecinos = 0;
    }

    public boolean isMina() { return mina; }
    public void setMina(boolean mina) { this.mina = mina; }

    public boolean esRevelado() { return esRevelado; }
    public void setRevelado(boolean esRevelado) { this.esRevelado = esRevelado; }

    public boolean tieneBandera() { return bandera; }
    public void setBandera(boolean bandera) { this.bandera = bandera; }

    public int getVecinos() { return vecinos; }
    public void setVecinos(int vecinos) { this.vecinos = vecinos; }
}
