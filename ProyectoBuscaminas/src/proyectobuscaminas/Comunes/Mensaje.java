/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectobuscaminas.Comunes;

import java.io.Serializable;

/**
 *
 * @author davek
 */
public class Mensaje implements Serializable {
    private String tipo;
    private Object contenido;

    public Mensaje(String tipo, Object contenido) {
        this.tipo = tipo;
        this.contenido = contenido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Object getContenido() {
        return contenido;
    }

    public void setContenido(Object contenido) {
        this.contenido = contenido;
    }
}
