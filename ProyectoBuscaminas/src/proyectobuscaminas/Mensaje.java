package proyectobuscaminas;

import java.io.Serializable;

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