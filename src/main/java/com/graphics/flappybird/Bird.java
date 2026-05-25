package com.graphics.flappybird;

/**
 * Estado completo de un jugador: posicion, velocidad, puntaje,
 * color, teclas de salto y temporizadores de animacion.
 */
public class Bird {

    public static final float FLASH_DURACION = 0.5f; // segundos que dura el parpadeo al morir

    public float y, velocidadY;
    public int score, puntajeAnterior;
    public boolean alive, saltoPrevio;
    public float anguloAla, tiempoFlash;

    public final int[] jumpKeys;
    public final float cr, cg, cb;
    public final String nombre;

    public Bird(float startY, float cr, float cg, float cb, String nombre, int... jumpKeys) {
        this.cr = cr;
        this.cg = cg;
        this.cb = cb;
        this.nombre = nombre;
        this.jumpKeys = jumpKeys;
        reset(startY); //deja el pájaro listo para jugar
    }

    public void reset(float startY) {
        y = startY;
        velocidadY = 0f;
        score = 0;
        puntajeAnterior = 0;
        alive = true;
        saltoPrevio = false;
        anguloAla = 0f;
        tiempoFlash = 0f;
    }
    /*
              🐦
               ↑
        Posición: 0.12
        Velocidad: quieto
        Puntos: 0
        Estado: vivo
        Flash: apagado
     */

    // Marca al pajaro como eliminado y activa el flash de muerte.
    public void matar() {
        alive = false;
        tiempoFlash = FLASH_DURACION;
    }

    @Override
    public String toString() {
        return "Bird{" +
                "nombre='" + nombre + '\'' +
                ", y=" + y +
                ", velocidadY=" + velocidadY +
                ", score=" + score +
                ", alive=" + alive +
                ", tiempoFlash=" + tiempoFlash +
                '}';
    }

    public static void main(String[] args) {

        Bird bird = new Bird(
                0.12f,
                0.97f, 0.82f, 0.15f,
                "J1",
                32);

        System.out.println("Estado inicial:");
        System.out.println(bird);
        /*
                    🐦
                    ↑

            Posición: 0.12
            Velocidad: quieto
            Puntos: 0
            Estado: vivo
            Flash: apagado
         */

        bird.score = 5;
        bird.velocidadY = 0.8f; //velocidadY = 0.8f → el pájaro se mueve hacia arriba
        /*          🐦
                    ↑↑↑
                    subiendo

        Puntaje: 5
        Estado: vivo */

        System.out.println("\nDespues de jugar:");
        System.out.println(bird);

        bird.matar();
        /*
                  💀🐦

            Estado: muerto
            Flash: ON
            Duración: 0.5 segundos
        */
        System.out.println("\nDespues de morir:");
        System.out.println(bird);
        //--------------------------------------
        /*
        CREAR BIRD
                ↓
            reset()
                ↓
            🐦 vivo
                ↓
            juega (score + velocidad)
                ↓
            🐦 ↑↑↑
                ↓
            matar()
                ↓
            💀🐦 flash = 0.5s
         */
    }
}
