package com.graphics.flappybird;

/** Todas las constantes del juego en un solo lugar. */
final class Constantes {

    private Constantes() {}

    // Ventana
    static final int ANCHO = 1100;
    static final int ALTO  = 720;

    // Pajaro
    static final float BIRD_X     = -0.45f;
    static final float BIRD_ANCHO =  0.10f;
    static final float BIRD_ALTO  =  0.09f;

    // Fisica
    static final float GRAVEDAD            = -1.9f;
    static final float IMPULSO_SALTO       =  0.85f;
    static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Tuberias
    static final float TUBERIA_ANCHO  =  0.18f;
    static final float GAP_ALTO_BASE  =  0.48f;
    static final float GAP_ALTO_MIN   =  0.28f;
    static final float GAP_MIN_CENTRO = -0.38f;
    static final float GAP_MAX_CENTRO =  0.38f;

    // Dificultad progresiva
    static final float VEL_BASE              = 0.62f;
    static final float SPAWN_BASE            = 1.50f;
    static final float VEL_PASO              = 0.15f;
    static final float SPAWN_PASO            = 0.13f;
    static final int   NIVEL_MAX             = 5;
    static final int   PUNTOS_POR_NIVEL      = 5;
    static final int   PUNTAJE_SUBIDA_ESPECIAL = 5;

    // Suelo
    static final float SUELO_Y    = -0.88f;
    static final float SUELO_ALTO =  0.14f;

    // Nubes
    static final float CLOUD_SPEED = 0.07f;
}
