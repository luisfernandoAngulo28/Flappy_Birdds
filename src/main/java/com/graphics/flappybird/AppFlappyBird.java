package com.graphics.flappybird;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static com.graphics.flappybird.Constantes.*;

/**
 * Clase principal del juego.
 * Gestiona la ventana GLFW, la logica del juego (fisica, colisiones, niveles)
 * y coordina el Renderer y la Escena para el dibujado.
 */
public class AppFlappyBird {

    private long window;
    private Renderer renderer;
    private Escena   escena;

    // -------------------------------------------------------------------------
    // Estado del juego
    // -------------------------------------------------------------------------
    private final Bird[] birds = {
            new Bird( 0.12f, 0.97f, 0.82f, 0.15f, "J1", GLFW.GLFW_KEY_SPACE),
            new Bird(-0.12f, 0.20f, 0.80f, 0.97f, "J2", GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP),
            new Bird( 0.00f, 0.95f, 0.40f, 0.80f, "J3", GLFW.GLFW_KEY_L)
    };

    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random        random   = new Random();
    private final float[][]     nubes    = {
            {-0.50f, 0.65f, 1.0f},
            { 0.15f, 0.74f, 0.7f},
            { 0.70f, 0.60f, 1.2f},
            {-0.85f, 0.70f, 0.8f},
            { 1.10f, 0.67f, 0.9f},
    };

    private float   timerGeneracion;
    private boolean started, gameOver, teclaReinicioPulsada;
    private boolean enCuentaRegresiva;
    private float   tiempoCuentaRegresiva;
    private float   alturaHueco        = GAP_ALTO_BASE;
    private int     nivelActual        = 1;
    private float   velocidadTuberias  = VEL_BASE;
    private float   tiempoEntreTuberias = SPAWN_BASE;

    // -------------------------------------------------------------------------
    // Inicializacion
    // -------------------------------------------------------------------------
    private void inicializar() {
        if (!GLFW.glfwInit())
            throw new IllegalStateException("No se pudo iniciar GLFW");
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,               GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE,             GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,        GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "", 0, 0);
        if (window == 0)
            throw new RuntimeException("No se pudo crear la ventana");
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        renderer = new Renderer();
        escena   = new Escena(renderer, birds, tuberias, nubes);
    }

    // -------------------------------------------------------------------------
    // Logica: reset
    // -------------------------------------------------------------------------
    private void resetGame() {
        birds[0].reset( 0.12f);
        birds[1].reset(-0.12f);
        birds[2].reset( 0.00f);
        timerGeneracion      = 0f;
        started              = false;
        gameOver             = false;
        enCuentaRegresiva    = false;
        tiempoCuentaRegresiva = 0f;
        nivelActual          = 1;
        velocidadTuberias    = VEL_BASE;
        tiempoEntreTuberias  = SPAWN_BASE;
        alturaHueco          = GAP_ALTO_BASE;
        tuberias.clear();
        actualizarTitulo();
    }

    // -------------------------------------------------------------------------
    // Logica: entrada de teclado
    // -------------------------------------------------------------------------
    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS)
            GLFW.glfwSetWindowShouldClose(window, true);

        for (Bird pajaro : birds) {
            boolean jumpPresionado = false;
            for (int tecla : pajaro.jumpKeys)
                if (GLFW.glfwGetKey(window, tecla) == GLFW.GLFW_PRESS) {
                    jumpPresionado = true;
                    break;
                }

            if (jumpPresionado && !pajaro.saltoPrevio) {
                if (gameOver) { resetGame(); break; }
                if (!started && !enCuentaRegresiva) {
                    enCuentaRegresiva     = true;
                    tiempoCuentaRegresiva = 3.0f;
                } else if (started && pajaro.alive) {
                    pajaro.velocidadY = IMPULSO_SALTO;
                }
            }
            pajaro.saltoPrevio = jumpPresionado;
        }

        boolean teclaR = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R)     == GLFW.GLFW_PRESS
                      || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        if (teclaR && !teclaReinicioPulsada && gameOver)
            resetGame();
        teclaReinicioPulsada = teclaR;
    }

    // -------------------------------------------------------------------------
    // Logica: actualizacion por frame
    // -------------------------------------------------------------------------
    private void actualizar(float dt) {
        actualizarNubes(dt);

        if (enCuentaRegresiva) {
            tiempoCuentaRegresiva -= dt;
            if (tiempoCuentaRegresiva <= 0f) {
                enCuentaRegresiva = false;
                started = true;
            }
            return;
        }

        if (!started || gameOver) return;

        actualizarPajaros(dt);
        if (ambosEliminados()) { gameOver = true; actualizarTitulo(); return; }

        actualizarTuberias(dt);
        if (ambosEliminados()) { gameOver = true; actualizarTitulo(); }
    }

    private void actualizarNubes(float dt) {
        for (float[] nube : nubes) {
            nube[0] -= CLOUD_SPEED * dt;
            if (nube[0] < -1.5f) nube[0] = 1.5f;
        }
    }

    private void actualizarPajaros(float dt) {
        for (Bird p : birds) {//para cada pajaro, actualizar su estado segun si esta vivo o muerto, aplicar gravedad, verificar colisiones con el suelo y techo, etc.
            if (p.tiempoFlash > 0f) //El parpadeo de muerte
                p.tiempoFlash = Math.max(0f, p.tiempoFlash - dt);

            if (!p.alive) {// el suelo: muere, pero sigue cayendo sin limite de velocidad, y su ala sigue batiendo cada vez mas rapido hasta desaparecer en el horizonte.
                // Pajaro muerto: sigue cayendo sin limite de velocidad.
                p.velocidadY += GRAVEDAD * dt;// sigue acelerando hacia abajo
                p.y          += p.velocidadY * dt; //sigue cayendo
                continue;   // salta todo lo demás y pasa al siguiente pájaro
            }

            p.anguloAla += dt * (Math.abs(p.velocidadY) * 2.5f + 5f);// Animación del ala

            // Impulso especial al superar ciertos puntajes.
            if (p.score != p.puntajeAnterior) { //Impulso especial al llegar a 5 puntos
                if (p.score > 0 && p.score % PUNTAJE_SUBIDA_ESPECIAL == 0) //5 == 0
                    p.velocidadY = 1.5f;
                p.puntajeAnterior = p.score;
            }

            p.velocidadY += GRAVEDAD * dt;
            if (p.velocidadY < VELOCIDAD_MAX_CAIDA)
                p.velocidadY = VELOCIDAD_MAX_CAIDA;
            p.y += p.velocidadY * dt;

            if (p.y - BIRD_ALTO * 0.5f <= SUELO_Y + SUELO_ALTO * 0.5f) p.matar();//si toca el suelo, muere.
            if (p.y + BIRD_ALTO * 0.5f >= 1f)                           p.matar();//si toca el techo, también muere.
        }
    }

    private void actualizarTuberias(float dt) {
        timerGeneracion += dt;
        if (timerGeneracion >= tiempoEntreTuberias) {
            timerGeneracion = 0f;
            generarTuberia();
        }

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= velocidadTuberias * dt;

            // Punto cuando el borde derecho de la tuberia supera la posicion X del pajaro.
            if (t.x + TUBERIA_ANCHO * 0.5f < BIRD_X && !t.puntoContabilizado) {
                t.puntoContabilizado = true;
                boolean alguienPuntuo = false;
                for (Bird p : birds)
                    if (p.alive) { p.score++; alguienPuntuo = true; }
                if (alguienPuntuo) { 
                    calcularNivel();// Recalcula el nivel y sus parametros cada vez que se anota un punto.
                    actualizarTitulo(); 
                }
            }
            //Choca con una TUBERÍA 
            for (Bird pajarito : birds)
                if (pajarito.alive && colisionaConTuberia(t, pajarito.y))
                    pajarito.matar();

            if (t.x + TUBERIA_ANCHO * 0.5f < -1.3f)
                it.remove();
        }
    }

    private void generarTuberia() {
        float centro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, centro, alturaHueco));
    }

    // AABB: superposicion horizontal primero, luego verificar que el pajaro este fuera del hueco.
    private boolean colisionaConTuberia(Tuberia t, float posY) {
        float pIzq = BIRD_X - BIRD_ANCHO * 0.5f, pDer = BIRD_X + BIRD_ANCHO * 0.5f;
        float pInf = posY   - BIRD_ALTO  * 0.5f, pSup = posY   + BIRD_ALTO  * 0.5f;
        float tIzq = t.x   - TUBERIA_ANCHO * 0.5f, tDer = t.x + TUBERIA_ANCHO * 0.5f;
        if (!(pDer > tIzq && pIzq < tDer)) return false;
        return pSup > t.centroHuecoY + t.alturaHueco * 0.5f
            || pInf < t.centroHuecoY - t.alturaHueco * 0.5f;
    }

    private void calcularNivel() {
        int pMax = 0;
        for (Bird p : birds) pMax = Math.max(pMax, p.score);
        int nivelNuevo = Math.min(pMax / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
                            //  pMax=0→4 : nivel 1     (0/5 + 1 = 1)
        if (nivelNuevo != nivelActual) {
            nivelActual         = nivelNuevo;
            velocidadTuberias   = VEL_BASE   + (nivelActual - 1) * VEL_PASO;
            tiempoEntreTuberias = SPAWN_BASE - (nivelActual - 1) * SPAWN_PASO;
            alturaHueco         = GAP_ALTO_BASE
                    - (nivelActual - 1) * (GAP_ALTO_BASE - GAP_ALTO_MIN) / (float) (NIVEL_MAX - 1);
        }
    }

    private void actualizarTitulo() {
        String e1 = "P1: " + birds[0].score + (birds[0].alive ? "" : " [X]");
        String e2 = "P2: " + birds[1].score + (birds[1].alive ? "" : " [X]");
        String e3 = "P3: " + birds[2].score + (birds[2].alive ? "" : " [X]");
        String vel = String.format("%.2f", velocidadTuberias);
        String tit = "Flappy Bird OpenGL | " + e1 + "  |  " + e2 + "  |  " + e3
                   + "  ||  Nivel " + nivelActual + "/" + NIVEL_MAX + " | Val: " + vel;
        if      (!started) GLFW.glfwSetWindowTitle(window, tit + "  |  SPACE / W para iniciar");
        else if (gameOver)  GLFW.glfwSetWindowTitle(window, tit + "  |  GAME OVER - SPACE/ENTER para reiniciar");
        else                GLFW.glfwSetWindowTitle(window, tit);
    }

    private boolean ambosEliminados() {
        for (Bird b : birds) if (b.alive) return false;
        return true;
    }

    // -------------------------------------------------------------------------
    // Bucle principal
    // -------------------------------------------------------------------------
    private void bucleDeJuego() {
        float ultimoFrame = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt    = Math.min(ahora - ultimoFrame, 0.033f);
            ultimoFrame = ahora;

            procesarInput();
            actualizar(dt);

            // Sincronizar estado escalar del juego con la Escena antes de dibujar.
            escena.started              = started;
            escena.gameOver             = gameOver;
            escena.enCuentaRegresiva    = enCuentaRegresiva;
            escena.tiempoCuentaRegresiva = tiempoCuentaRegresiva;
            escena.nivelActual          = nivelActual;
            escena.dibujarEscena(ahora);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void liberarRecursos() {
        renderer.liberarRecursos();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // -------------------------------------------------------------------------
    // Entrada
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        AppFlappyBird app = new AppFlappyBird();
        app.inicializar();
        app.resetGame();
        app.bucleDeJuego();
        app.liberarRecursos();
    }
}
