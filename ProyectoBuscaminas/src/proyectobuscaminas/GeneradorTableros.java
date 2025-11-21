package proyectobuscaminas;

import java.util.*;

public class GeneradorTableros {
    private List<Tablero> plantillas;
    private Random random;

    public GeneradorTableros() {
        plantillas = new ArrayList<>();
        random = new Random();
        crearPlantillasPorDefecto();
    }

    private void crearPlantillasPorDefecto() {
        try {
            plantillas.add(new Tablero(8, 8, 10));
            plantillas.add(new Tablero(10, 10, 15));
            plantillas.add(new Tablero(12, 12, 20));
            plantillas.add(new Tablero(8, 8, 12));
            plantillas.add(new Tablero(10, 10, 18));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Tablero[] generarTableros() {
        if (plantillas.size() < 2) {
            return new Tablero[] {
                    new Tablero(8, 8, 10),
                    new Tablero(8, 8, 10)
            };
        }

        int idx1 = random.nextInt(plantillas.size());
        int idx2;
        do {
            idx2 = random.nextInt(plantillas.size());
        } while (idx2 == idx1 && plantillas.size() > 1);

        return new Tablero[] {
                new Tablero(plantillas.get(idx1)),
                new Tablero(plantillas.get(idx2))
        };
    }
}