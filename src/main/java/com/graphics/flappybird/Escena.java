package com.graphics.flappybird;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import static com.graphics.flappybird.Constantes.*;

/**
 * Capa de dibujo de la escena del juego.
 * Usa Renderer para las primitivas OpenGL y traduce el estado del juego
 * a llamadas de dibujo concretas (fondo, pajaros, tuberias, HUD, pantallas).
 */
class Escena {

    // Angulo PI/2: hace que el triangulo base apunte hacia arriba.
    private static final float UP = (float) (Math.PI / 2.0);

    // Colores por nivel: verde → amarillo → naranja → rojo → purpura.
    private static final float[][] NIVEL_COLORES = {
            {0.20f, 0.85f, 0.35f}, // nivel 1 - verde
            {0.75f, 0.90f, 0.15f}, // nivel 2 - amarillo
            {1.00f, 0.65f, 0.10f}, // nivel 3 - naranja
            {1.00f, 0.25f, 0.15f}, // nivel 4 - rojo
            {0.80f, 0.20f, 0.90f}, // nivel 5 - purpura
    };

    private final Renderer r;
    private final Bird[]         birds;
    private final List<Tuberia>  tuberias;
    private final float[][]      nubes;

    // Estado de juego; AppFlappyBird los actualiza antes de cada llamada a dibujarEscena().
    boolean started, gameOver, enCuentaRegresiva;
    float   tiempoCuentaRegresiva;
    int     nivelActual;

    Escena(Renderer renderer, Bird[] birds, List<Tuberia> tuberias, float[][] nubes) {
        this.r        = renderer;
        this.birds    = birds;
        this.tuberias = tuberias;
        this.nubes    = nubes;
    }

    // -------------------------------------------------------------------------
    // Punto de entrada principal: dibuja un frame completo.
    // -------------------------------------------------------------------------
    void dibujarEscena(float tiempo) {
        GL11.glClearColor(0.52f, 0.80f, 0.95f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(r.programaShader);

        // 1. Fondo: cielo, sol, montanas, suelo, nubes.
        dibujarFondo();

        // 2. Tuberias con brillo, sombra y capuchon.
        dibujarTuberias();

        // 3. Pajaros (se omiten si estan muy fuera de pantalla).
        for (Bird b : birds) {
            if (b.y < -1.4f || b.y > 1.3f) continue;
            dibujarPajaro(BIRD_X, b.y, b.velocidadY, b);
        }

        // 4. Franja lateral de jugador muerto.
        for (int i = 0; i < birds.length; i++) {
            if (!birds[i].alive) {
                float px = (i == 0) ? -0.97f : 0.97f;
                r.dibujarRectangulo(px, 0f, 0.04f, 2f,
                        birds[i].cr * 0.4f, birds[i].cg * 0.4f, birds[i].cb * 0.4f);
            }
        }

        // 5. HUD: puntajes y barra de nivel.
        dibujarHUD();

        // 6. Pantalla segun el estado del juego.
        if (!started && !enCuentaRegresiva)
            dibujarPantallaInicio(tiempo);
        else if (enCuentaRegresiva)
            dibujarCuentaRegresiva();
        else if (gameOver)
            dibujarPantallaGameOver();
    }

    // -------------------------------------------------------------------------
    // Fondo: cielo degradado, sol, montanas, suelo, nubes.
    // -------------------------------------------------------------------------
    private void dibujarFondo() {
        // Cielo: cuatro franjas de degradado azul.
        r.dibujarRectangulo(0f, -1.0f, 2f, 1.0f, 0.38f, 0.65f, 0.82f);
        r.dibujarRectangulo(0f, -0.2f, 2f, 1.0f, 0.46f, 0.74f, 0.90f);
        r.dibujarRectangulo(0f,  0.5f, 2f, 1.0f, 0.54f, 0.82f, 0.97f);
        r.dibujarRectangulo(0f,  0.9f, 2f, 0.4f, 0.62f, 0.89f, 1.00f);

        // Sol: circulo aproximado con quads solapados.
        r.dibujarRectangulo(0.72f, 0.72f, 0.14f, 0.14f, 1.00f, 0.96f, 0.50f);
        r.dibujarRectangulo(0.72f, 0.72f, 0.10f, 0.18f, 1.00f, 0.96f, 0.50f);
        r.dibujarRectangulo(0.72f, 0.72f, 0.18f, 0.10f, 1.00f, 0.96f, 0.50f);
        // Rayos del sol.
        r.dibujarTriangulo(0.72f, 0.82f, 0.04f, 0.06f, UP,                    1.00f, 0.96f, 0.50f);
        r.dibujarTriangulo(0.72f, 0.62f, 0.04f, 0.06f, UP + (float) Math.PI,  1.00f, 0.96f, 0.50f);
        r.dibujarTriangulo(0.82f, 0.72f, 0.06f, 0.04f, UP * 2,                1.00f, 0.96f, 0.50f);
        r.dibujarTriangulo(0.62f, 0.72f, 0.06f, 0.04f, 0f,                    1.00f, 0.96f, 0.50f);

        // Montanas lejanas: capa trasera (mas claras, mas anchas).
        r.dibujarTriangulo(-0.80f, -0.74f, 0.90f, 0.62f, UP, 0.58f, 0.66f, 0.63f);
        r.dibujarTriangulo(-0.05f, -0.70f, 0.70f, 0.56f, UP, 0.54f, 0.62f, 0.60f);
        r.dibujarTriangulo( 0.65f, -0.72f, 0.80f, 0.58f, UP, 0.56f, 0.64f, 0.61f);
        r.dibujarTriangulo(-1.30f, -0.76f, 0.75f, 0.55f, UP, 0.60f, 0.67f, 0.64f);

        // Montanas delanteras: capa frontal (mas oscuras, mas altas).
        r.dibujarTriangulo(-0.62f, -0.76f, 0.65f, 0.72f, UP, 0.42f, 0.52f, 0.50f);
        r.dibujarTriangulo( 0.10f, -0.73f, 0.55f, 0.65f, UP, 0.38f, 0.48f, 0.46f);
        r.dibujarTriangulo( 0.60f, -0.75f, 0.60f, 0.68f, UP, 0.40f, 0.50f, 0.48f);
        r.dibujarTriangulo(-1.20f, -0.78f, 0.58f, 0.60f, UP, 0.44f, 0.54f, 0.52f);

        // Nevado en los picos.
        r.dibujarTriangulo(-0.62f, -0.40f, 0.18f, 0.20f, UP, 0.92f, 0.95f, 0.97f);
        r.dibujarTriangulo( 0.10f, -0.43f, 0.15f, 0.17f, UP, 0.92f, 0.95f, 0.97f);
        r.dibujarTriangulo( 0.60f, -0.41f, 0.16f, 0.18f, UP, 0.92f, 0.95f, 0.97f);

        // Suelo: franja verde con linea de horizonte y detalle de hierba.
        r.dibujarRectangulo(0f, SUELO_Y, 2f, SUELO_ALTO, 0.24f, 0.62f, 0.18f);
        r.dibujarRectangulo(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.005f, 2f, 0.025f, 0.18f, 0.50f, 0.14f);
        r.dibujarRectangulo(0f, SUELO_Y + SUELO_ALTO * 0.5f - 0.018f, 2f, 0.012f, 0.32f, 0.72f, 0.24f);

        // Nubes animadas.
        for (float[] n : nubes)
            dibujarNube(n[0], n[1], n[2]);
    }

    private void dibujarNube(float x, float y, float escala) {
        // Sombra tenue debajo.
        r.dibujarRectangulo(x + 0.02f * escala, y - 0.05f * escala,
                0.22f * escala, 0.07f * escala, 0.75f, 0.82f, 0.90f);
        // Cuerpo de la nube (4 quads blancos solapados).
        r.dibujarRectangulo(x,                   y,         0.22f * escala, 0.11f * escala, 0.96f, 0.98f, 1.00f);
        r.dibujarRectangulo(x - 0.09f * escala,  y - 0.02f, 0.15f * escala, 0.10f * escala, 0.96f, 0.98f, 1.00f);
        r.dibujarRectangulo(x + 0.09f * escala,  y - 0.02f, 0.15f * escala, 0.10f * escala, 0.96f, 0.98f, 1.00f);
        r.dibujarRectangulo(x - 0.04f * escala,  y + 0.04f, 0.12f * escala, 0.09f * escala, 0.99f, 1.00f, 1.00f);
    }

    // -------------------------------------------------------------------------
    // Pajaro compuesto: cuerpo, cola, ala, pico, ojo y pupila.
    // -------------------------------------------------------------------------
    private void dibujarPajaro(float x, float y, float velocidadY, Bird bird) {
        float brillo = bird.alive ? 1.0f : 0.35f;
        boolean parpadeo = bird.tiempoFlash > 0f && ((int) (bird.tiempoFlash * 12f) % 2) == 0;

        float cr = parpadeo ? 1f : bird.cr * brillo;
        float cg = parpadeo ? 1f : bird.cg * brillo;
        float cb = parpadeo ? 1f : bird.cb * brillo;
        float pr = parpadeo ? 1f : 1f  * brillo;  // pico rojo
        float pg = parpadeo ? 1f : 0.50f * brillo;
        float pb = parpadeo ? 1f : 0.08f * brillo;
        float ojo = parpadeo ? 1f : brillo;

        float inclinacion = bird.alive ? clamp(velocidadY * 0.28f, -0.55f, 0.45f) : -0.60f;
        float anguloAla   = bird.anguloAla;
        float[] pos;

        // Cuerpo.
        r.dibujarRectangulo(x, y, BIRD_ANCHO, BIRD_ALTO, inclinacion, cr, cg, cb);

        // Cola.
        pos = r.rotarOffset(-0.057f, 0f, inclinacion);
        r.dibujarTriangulo(x + pos[0], y + pos[1], 0.045f, 0.030f,
                inclinacion + (float) Math.PI, cr * .85f, cg * .85f, cb * .85f);

        // Ala con movimiento de aleteo.
        float dyAla  = bird.alive ? (float) Math.sin(anguloAla) * 0.018f : 0f;
        float rotAla = bird.alive ? inclinacion + (float) Math.sin(anguloAla) * 0.35f : inclinacion;
        pos = r.rotarOffset(-0.005f, -0.005f + dyAla, inclinacion);
        r.dibujarRectangulo(x + pos[0], y + pos[1], 0.065f, 0.028f, rotAla,
                cr * .78f, cg * .78f, cb * .78f);

        // Pico.
        pos = r.rotarOffset(0.058f, -0.004f, inclinacion);
        r.dibujarTriangulo(x + pos[0], y + pos[1], 0.038f, 0.024f, inclinacion, pr, pg, pb);

        // Ojo.
        pos = r.rotarOffset(0.022f, 0.018f, inclinacion);
        r.dibujarRectangulo(x + pos[0], y + pos[1], 0.028f, 0.028f, inclinacion, ojo, ojo, ojo);

        // Pupila.
        pos = r.rotarOffset(0.028f, 0.016f, inclinacion);
        r.dibujarRectangulo(x + pos[0], y + pos[1], 0.013f, 0.013f, inclinacion, 0.08f, 0.08f, 0.08f);
    }

    // -------------------------------------------------------------------------
    // Tuberias: cuerpo + brillo + sombra + capuchon por par.
    // -------------------------------------------------------------------------
    private void dibujarTuberias() {
        for (Tuberia tuberia : tuberias) {
            float bordeSup = tuberia.centroHuecoY + tuberia.alturaHueco * 0.5f;
            float bordeInf = tuberia.centroHuecoY - tuberia.alturaHueco * 0.5f;
            float altoArriba = 1f - bordeSup;
            float altoAbajo  = bordeInf + 1f;

            if (altoArriba > 0f) {
                float cy = bordeSup + altoArriba * 0.5f;
                r.dibujarRectangulo(tuberia.x + TUBERIA_ANCHO * 0.5f - 0.012f, cy, 0.024f, altoArriba, 0.08f, 0.40f, 0.10f);
                r.dibujarRectangulo(tuberia.x, cy, TUBERIA_ANCHO, altoArriba, 0.15f, 0.60f, 0.18f);
                r.dibujarRectangulo(tuberia.x - TUBERIA_ANCHO * 0.5f + 0.010f, cy, 0.018f, altoArriba, 0.22f, 0.72f, 0.26f);
                r.dibujarRectangulo(tuberia.x, bordeSup - 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                r.dibujarRectangulo(tuberia.x, bordeSup - 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }

            if (altoAbajo > 0f) {
                float cy = -1f + altoAbajo * 0.5f;
                r.dibujarRectangulo(tuberia.x + TUBERIA_ANCHO * 0.5f - 0.012f, cy, 0.024f, altoAbajo, 0.08f, 0.40f, 0.10f);
                r.dibujarRectangulo(tuberia.x, cy, TUBERIA_ANCHO, altoAbajo, 0.15f, 0.60f, 0.18f);
                r.dibujarRectangulo(tuberia.x - TUBERIA_ANCHO * 0.5f + 0.010f, cy, 0.018f, altoAbajo, 0.22f, 0.72f, 0.26f);
                r.dibujarRectangulo(tuberia.x, bordeInf + 0.018f, TUBERIA_ANCHO + 0.035f, 0.036f, 0.10f, 0.45f, 0.13f);
                r.dibujarRectangulo(tuberia.x, bordeInf + 0.018f, TUBERIA_ANCHO + 0.020f, 0.026f, 0.18f, 0.58f, 0.20f);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Display de 7 segmentos para mostrar numeros.
    // -------------------------------------------------------------------------
    private void dibujarDigito(int digito, float x, float y, float tam,
                                float rojo, float verde, float azul) {
        if (digito < 0 || digito > 9) return;
        float h = tam, w = tam * 0.55f, g = tam * 0.13f;
        float mh = h / 2f, mw = w / 2f;
        float lv = mh - g, lh = w - g;
        // Segmentos: [arriba, derArriba, derAbajo, abajo, izqAbajo, izqArriba, medio]
        boolean[][] tabla = {
                {true,  true,  true,  true,  true,  true,  false}, // 0
                {false, true,  true,  false, false, false, false}, // 1
                {true,  true,  false, true,  true,  false, true }, // 2
                {true,  true,  true,  true,  false, false, true }, // 3
                {false, true,  true,  false, false, true,  true }, // 4
                {true,  false, true,  true,  false, true,  true }, // 5
                {true,  false, true,  true,  true,  true,  true }, // 6
                {true,  true,  true,  false, false, false, false}, // 7
                {true,  true,  true,  true,  true,  true,  true }, // 8
                {true,  true,  true,  true,  false, true,  true }, // 9
        };
        boolean[] s = tabla[digito];
        if (s[0]) r.dibujarRectangulo(x,          y + mh - g / 2f,   lh, g, rojo, verde, azul);
        if (s[1]) r.dibujarRectangulo(x + mw - g / 2f, y + h / 4f,  g, lv, rojo, verde, azul);
        if (s[2]) r.dibujarRectangulo(x + mw - g / 2f, y - h / 4f,  g, lv, rojo, verde, azul);
        if (s[3]) r.dibujarRectangulo(x,          y - mh + g / 2f,   lh, g, rojo, verde, azul);
        if (s[4]) r.dibujarRectangulo(x - mw + g / 2f, y - h / 4f,  g, lv, rojo, verde, azul);
        if (s[5]) r.dibujarRectangulo(x - mw + g / 2f, y + h / 4f,  g, lv, rojo, verde, azul);
        if (s[6]) r.dibujarRectangulo(x,          y,                  lh, g, rojo, verde, azul);
    }

    private void dibujarNumero(int numero, float cx, float cy,
                                float tam, float rojo, float verde, float azul) {
        String txt = String.valueOf(Math.max(0, numero));
        float espacio = tam * 0.72f;
        float anchoTotal = (txt.length() - 1) * espacio;
        for (int i = 0; i < txt.length(); i++)
            dibujarDigito(txt.charAt(i) - '0',
                    cx - anchoTotal / 2f + i * espacio, cy, tam, rojo, verde, azul);
    }

    // -------------------------------------------------------------------------
    // Cuenta regresiva 3-2-1 con pulso de tamano.
    // -------------------------------------------------------------------------
    private void dibujarCuentaRegresiva() {
        int num = Math.max(1, (int) Math.ceil(tiempoCuentaRegresiva));
        float fraccion = tiempoCuentaRegresiva - (num - 1); // 0..1 dentro del segundo
        float tam = 0.15f + fraccion * 0.38f;
        float rojo, verde, azul;
        if      (num == 3) { rojo = 0.20f; verde = 0.88f; azul = 0.35f; } // verde
        else if (num == 2) { rojo = 1.00f; verde = 0.82f; azul = 0.10f; } // amarillo
        else               { rojo = 1.00f; verde = 0.22f; azul = 0.15f; } // rojo
        r.dibujarRectangulo(0.010f, -0.010f, tam * 0.65f, tam * 1.05f, 0.04f, 0.04f, 0.05f);
        dibujarNumero(num, 0f, 0f, tam, rojo, verde, azul);
    }

    // -------------------------------------------------------------------------
    // HUD: barra superior con puntajes y barra de nivel.
    // -------------------------------------------------------------------------
    private void dibujarHUD() {
        if (!started) return;

        r.dibujarRectangulo(0f, 0.935f, 2f, 0.130f, 0.08f, 0.10f, 0.13f);
        r.dibujarRectangulo(0f, 0.865f, 2f, 0.010f, 0.18f, 0.22f, 0.28f);

        // J1 izquierda, J3 centro, J2 derecha.
        dibujarPajaro(-0.88f, 0.938f, 0f, birds[0]);
        dibujarNumero(birds[0].score, -0.72f, 0.935f, 0.078f, birds[0].cr, birds[0].cg, birds[0].cb);

        dibujarPajaro(0.00f, 0.938f, 0f, birds[2]);
        dibujarNumero(birds[2].score,  0.14f, 0.935f, 0.078f, birds[2].cr, birds[2].cg, birds[2].cb);

        dibujarPajaro(0.88f, 0.938f, 0f, birds[1]);
        dibujarNumero(birds[1].score,  0.72f, 0.935f, 0.078f, birds[1].cr, birds[1].cg, birds[1].cb);

        // Indicador de nivel.
        float[] cNivel = nivelColor(nivelActual);
        dibujarNumero(nivelActual, 0f, 0.940f, 0.058f, cNivel[0], cNivel[1], cNivel[2]);

        // Barra de progreso hacia el siguiente nivel.
        float anchoB = 0.50f, posYB = 0.878f, altoB = 0.016f;
        r.dibujarRectangulo(0f, posYB, anchoB + 0.01f, altoB + 0.008f, 0.18f, 0.20f, 0.24f);
        r.dibujarRectangulo(0f, posYB, anchoB,         altoB,           0.22f, 0.24f, 0.28f);

        int ptsEnNivel = Math.max(Math.max(birds[0].score, birds[1].score), birds[2].score) % PUNTOS_POR_NIVEL;
        float progreso = (nivelActual >= NIVEL_MAX) ? 1f : ptsEnNivel / (float) PUNTOS_POR_NIVEL;
        if (progreso > 0f)
            r.dibujarRectangulo(-anchoB * 0.5f + anchoB * progreso * 0.5f, posYB,
                    anchoB * progreso, altoB, cNivel[0], cNivel[1], cNivel[2]);

        r.dibujarTriangulo(anchoB * 0.5f + 0.022f, posYB, 0.018f, 0.028f, 0f,
                cNivel[0], cNivel[1], cNivel[2]);

        for (int i = 1; i < PUNTOS_POR_NIVEL; i++) {
            float px = -anchoB * 0.5f + anchoB * (i / (float) PUNTOS_POR_NIVEL);
            r.dibujarRectangulo(px, posYB, 0.004f, altoB, 0.10f, 0.11f, 0.14f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantalla de inicio con pajaros animados flotando.
    // -------------------------------------------------------------------------
    private void dibujarPantallaInicio(float tiempo) {
        r.dibujarRectangulo(0.015f, -0.015f, 1.38f, 0.68f, 0.03f, 0.04f, 0.05f);
        r.dibujarRectangulo(0f,     0.0f,    1.38f, 0.68f, 0.10f, 0.13f, 0.17f);
        r.dibujarRectangulo(0f,     0.30f,   1.38f, 0.08f, 0.14f, 0.18f, 0.24f);

        // Borde celeste (4 lados + esquinas).
        r.dibujarRectangulo(0f,      0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        r.dibujarRectangulo(0f,     -0.340f, 1.38f, 0.012f, 0.40f, 0.70f, 0.95f);
        r.dibujarRectangulo(-0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        r.dibujarRectangulo( 0.690f, 0.0f,   0.012f, 0.68f, 0.40f, 0.70f, 0.95f);
        r.dibujarRectangulo(-0.690f,  0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        r.dibujarRectangulo( 0.690f,  0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        r.dibujarRectangulo(-0.690f, -0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);
        r.dibujarRectangulo( 0.690f, -0.340f, 0.016f, 0.016f, 0.70f, 0.90f, 1.00f);

        // Titulo: bloques de color alternados.
        float[] cTit = {0.40f, 0.70f, 0.95f};
        for (int i = 0; i < 9; i++) {
            float[] c = (i % 2 == 0) ? cTit : new float[]{1f, 1f, 1f};
            r.dibujarRectangulo(-0.20f + i * 0.048f, 0.295f, 0.040f, 0.035f, c[0], c[1], c[2]);
        }

        // Pajaros flotando.
        float f1 = (float) Math.sin(tiempo * 2.2f)         * 0.025f;
        float f2 = (float) Math.sin(tiempo * 2.2f + 1.2f)  * 0.025f;
        float f3 = (float) Math.sin(tiempo * 2.2f + 2.4f)  * 0.025f;
        dibujarPajaro(-0.55f, 0.06f + f1, f1 * 12f,  birds[0]);
        dibujarPajaro( 0.00f, 0.06f + f3, f3 * 12f,  birds[2]);
        dibujarPajaro( 0.55f, 0.06f + f2, -f2 * 12f, birds[1]);

        // Separadores verticales.
        r.dibujarRectangulo(-0.275f, 0.08f, 0.006f, 0.28f, 0.25f, 0.30f, 0.38f);
        r.dibujarRectangulo( 0.275f, 0.08f, 0.006f, 0.28f, 0.25f, 0.30f, 0.38f);

        // Barras de tecla por jugador.
        r.dibujarRectangulo(-0.55f, -0.14f, 0.22f, 0.040f, birds[0].cr * 0.7f, birds[0].cg * 0.7f, birds[0].cb * 0.7f);
        r.dibujarRectangulo(-0.55f, -0.14f, 0.18f, 0.030f, birds[0].cr,        birds[0].cg,        birds[0].cb);
        r.dibujarRectangulo( 0.00f, -0.14f, 0.22f, 0.040f, birds[2].cr * 0.7f, birds[2].cg * 0.7f, birds[2].cb * 0.7f);
        r.dibujarRectangulo( 0.00f, -0.14f, 0.18f, 0.030f, birds[2].cr,        birds[2].cg,        birds[2].cb);
        r.dibujarRectangulo( 0.55f, -0.14f, 0.22f, 0.040f, birds[1].cr * 0.7f, birds[1].cg * 0.7f, birds[1].cb * 0.7f);
        r.dibujarRectangulo( 0.55f, -0.14f, 0.18f, 0.030f, birds[1].cr,        birds[1].cg,        birds[1].cb);

        // Boton parpadeante "PLAY".
        if (((int) (tiempo * 1.6f) % 2) == 0) {
            float pulso = 0.85f + (float) Math.sin(tiempo * 6f) * 0.15f;
            r.dibujarTriangulo(0f,    -0.255f, 0.07f * pulso, 0.07f * pulso, 0f, 0.95f, 0.95f, 0.60f);
            r.dibujarRectangulo(-0.10f, -0.255f, 0.055f, 0.018f,                    0.95f, 0.95f, 0.60f);
            r.dibujarRectangulo( 0.10f, -0.255f, 0.055f, 0.018f,                    0.95f, 0.95f, 0.60f);
        }
    }

    // -------------------------------------------------------------------------
    // Pantalla de game over con resultados y ganador.
    // -------------------------------------------------------------------------
    private void dibujarPantallaGameOver() {
        float t = (float) GLFW.glfwGetTime();

        r.dibujarRectangulo(0.015f, -0.015f, 1.50f, 0.82f, 0.02f, 0.03f, 0.04f);
        r.dibujarRectangulo(0f,     0.02f,   1.50f, 0.82f, 0.08f, 0.09f, 0.12f);
        r.dibujarRectangulo(0f,     0.37f,   1.50f, 0.08f, 0.55f, 0.12f, 0.12f);

        // Borde rojo.
        r.dibujarRectangulo(0f,      0.410f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        r.dibujarRectangulo(0f,     -0.390f, 1.50f, 0.014f, 0.85f, 0.18f, 0.18f);
        r.dibujarRectangulo(-0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);
        r.dibujarRectangulo( 0.750f, 0.010f, 0.014f, 0.82f, 0.85f, 0.18f, 0.18f);

        // Decoracion "GAME OVER".
        for (int i = 0; i < 9; i++) {
            float[] c = (i % 2 == 0) ? new float[]{0.90f, 0.18f, 0.18f} : new float[]{1f, 1f, 1f};
            r.dibujarRectangulo(-0.20f + i * 0.050f, 0.368f, 0.042f, 0.036f, c[0], c[1], c[2]);
        }

        // Barras de puntaje por jugador.
        int p1 = birds[0].score, p2 = birds[1].score, p3 = birds[2].score;
        int pMax = Math.max(Math.max(p1, p2), Math.max(p3, 1));
        float anchoB = 0.52f, altoB = 0.048f;

        float posYJ1 = 0.230f;
        dibujarBarraPuntaje(-0.60f, posYJ1, p1, pMax, anchoB, altoB, birds[0]);

        float posYJ3 = 0.100f;
        dibujarBarraPuntaje(-0.60f, posYJ3, p3, pMax, anchoB, altoB, birds[2]);

        float posYJ2 = -0.030f;
        dibujarBarraPuntaje(-0.60f, posYJ2, p2, pMax, anchoB, altoB, birds[1]);

        // Numeros de puntaje a la derecha.
        dibujarNumero(p1, 0.46f, posYJ1, 0.10f, birds[0].cr, birds[0].cg, birds[0].cb);
        dibujarNumero(p3, 0.46f, posYJ3, 0.10f, birds[2].cr, birds[2].cg, birds[2].cb);
        dibujarNumero(p2, 0.46f, posYJ2, 0.10f, birds[1].cr, birds[1].cg, birds[1].cb);

        // Corona al ganador.
        int pGan = Math.max(Math.max(p1, p2), p3);
        if (p1 == pGan) dibujarCorona(-0.57f, posYJ1 + 0.060f);
        if (p3 == pGan) dibujarCorona(-0.57f, posYJ3 + 0.060f);
        if (p2 == pGan) dibujarCorona(-0.57f, posYJ2 + 0.060f);

        // Barra de nivel alcanzado.
        float[] cNiv = nivelColor(nivelActual);
        r.dibujarRectangulo(0.05f, -0.100f, 0.80f, 0.040f, 0.15f, 0.17f, 0.20f);
        r.dibujarRectangulo(0.05f - 0.40f + 0.40f * (nivelActual / (float) NIVEL_MAX),
                -0.100f, 0.80f * (nivelActual / (float) NIVEL_MAX), 0.030f,
                cNiv[0], cNiv[1], cNiv[2]);
        for (int i = 1; i <= NIVEL_MAX; i++) {
            float px = 0.05f - 0.40f + 0.80f * (i / (float) NIVEL_MAX);
            r.dibujarRectangulo(px, -0.100f, 0.006f, 0.040f, 0.08f, 0.09f, 0.12f);
        }

        // Boton de reinicio parpadeante.
        if (((int) (t * 1.4f) % 2) == 0) {
            r.dibujarRectangulo(0.05f, -0.270f, 0.75f, 0.050f, 0.16f, 0.18f, 0.22f);
            r.dibujarRectangulo(0.05f, -0.270f, 0.70f, 0.036f, 0.28f, 0.32f, 0.40f);
            r.dibujarTriangulo( 0.05f, -0.270f, 0.030f, 0.030f, 0f,   0.80f, 0.85f, 0.95f);
        }
    }

    private void dibujarBarraPuntaje(float x, float y, int pts, int pMax,
                                      float anchoB, float altoB, Bird bird) {
        float prop = pts / (float) pMax;
        dibujarPajaro(x, y, 0f, bird);
        r.dibujarRectangulo(-anchoB * 0.5f * prop - 0.05f, y,
                anchoB * prop, altoB, bird.cr * 0.55f, bird.cg * 0.55f, bird.cb * 0.55f);
        r.dibujarRectangulo(-anchoB * 0.5f * prop - 0.05f, y + 0.003f,
                anchoB * prop, altoB * 0.60f, bird.cr, bird.cg, bird.cb);
    }

    private void dibujarCorona(float x, float y) {
        r.dibujarTriangulo(x,        y, 0.048f, 0.048f, UP, 1.00f, 0.90f, 0.20f);
        r.dibujarTriangulo(x + 0.06f, y, 0.030f, 0.035f, UP, 1.00f, 0.90f, 0.20f);
        r.dibujarTriangulo(x - 0.06f, y, 0.030f, 0.035f, UP, 1.00f, 0.90f, 0.20f);
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------
    private float[] nivelColor(int nivel) {
        return NIVEL_COLORES[Math.min(nivel, NIVEL_MAX) - 1];
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
   
}
