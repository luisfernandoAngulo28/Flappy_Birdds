# Estudio: Flappy Bird con OpenGL en Java

## Arquitectura general — visión general

```
AppFlappyBird  ←→  Escena
     ↓                ↓
  Bird[]           Renderer
  Tuberia[]
  Constantes
```

---

## Clase por clase

---

### `Constantes.java` — El reglamento del juego

Es como el **manual de reglas**. No hace nada por sí sola, solo guarda números que todos los demás usan.

```java
GRAVEDAD = -1.9f        // qué tan rápido cae el pájaro
IMPULSO_SALTO = 0.85f   // qué tan alto sube al saltar
BIRD_X = -0.45f         // posición horizontal fija del pájaro
NIVEL_MAX = 5           // cuántos niveles hay
```

**¿Por qué existe?**
Si quieres cambiar la gravedad, lo cambias en UN solo lugar y afecta a todo el juego. Sin esta clase tendrías el número `-1.9f` escrito en 10 lugares distintos y si lo quieres cambiar tendrías que buscarlo en todos lados.

---

### `Bird.java` — El pájaro (datos del jugador)

Es como una **ficha de jugador**. Guarda TODO lo que describe a un pájaro en un momento dado.

```
Bird tiene:
  y            → donde está verticalmente en pantalla
  velocidadY   → si va subiendo o bajando (y qué tan rápido)
  score        → cuántos puntos tiene
  alive        → ¿está vivo o muerto?
  cr, cg, cb   → su color (rojo, verde, azul)
  jumpKeys     → qué tecla usa para saltar
  tiempoFlash  → cuánto tiempo lleva parpadeando al morir
  anguloAla    → para animar el aleteo
```

**Métodos importantes:**

| Método | Qué hace |
|---|---|
| `Bird(startY, cr, cg, cb, nombre, teclas)` | Constructor: crea el pájaro con su color y teclas |
| `reset(startY)` | Vuelve al estado inicial (para reiniciar la partida) |
| `matar()` | Marca `alive = false` y activa el parpadeo de muerte |
| `toString()` | Muestra el estado del pájaro en texto (para depuración) |

**Ciclo de vida de un Bird:**
```
new Bird()
    ↓
reset() → 🐦 vivo
    ↓
juega (score sube, velocidadY cambia)
    ↓
matar() → 💀 alive=false, tiempoFlash=0.5s
```

---

### `Tuberia.java` — Una tubería (obstáculo)

Es como una **tarjeta de obstáculo**. Cada par de tuberías (arriba + abajo) es un objeto `Tuberia`.

```
Tuberia tiene:
  x                   → posición horizontal (va moviéndose hacia la izquierda)
  centroHuecoY        → dónde está el centro del hueco
  alturaHueco         → qué tan grande es el hueco para pasar
  puntoContabilizado  → ¿ya se cobró el punto por pasar esta tubería?
```

**Diagrama visual:**
```
        TUBERIA SUPERIOR
        ███████████
        ███████████
            ↑
        bordeSuperiorHueco

            HUECO  ← el pájaro debe pasar por aquí
            (alturaHueco)

        bordeInferiorHueco
            ↓
        ███████████
        ███████████
        TUBERIA INFERIOR

  x = 1.2 → aparece lejos a la derecha y se mueve hacia la izquierda
```

---

### `Renderer.java` — El pincel de OpenGL

Es como el **pintor con las herramientas básicas**. No sabe nada del juego — solo sabe dibujar formas geométricas.

**¿Qué puede dibujar?**
```java
dibujarRectangulo(x, y, ancho, alto, rotacion, r, g, b)
dibujarTriangulo(x, y, ancho, alto, rotacion, r, g, b)
rotarOffset(lx, ly, angulo)  // rota una posición local
```

**Internamente hace 3 cosas:**

#### 1. Compila los Shaders (programas que corren en la GPU)
```
Vertex Shader   → le dice a cada vértice DÓNDE ubicarse en pantalla
                  (aplica posición, escala y rotación)

Fragment Shader → le dice a cada pixel QUÉ COLOR tener
                  (simplemente pinta el color uniforme recibido)
```

#### 2. Crea la geometría base (VAO / VBO)
```
VAO (Vertex Array Object)  → recuerda cómo leer los vértices
VBO (Vertex Buffer Object) → guarda los vértices en la memoria de la GPU

Rectángulo base: 2 triángulos formando un cuadrado de -0.5 a 0.5
Triángulo base:  un triángulo simple
```

#### 3. Para dibujar cada forma:
```
1. Activa el VAO correcto (quad o triángulo)
2. Envía a la GPU:  uOffset (posición), uScale (tamaño), uRotation, uColor
3. Llama glDrawArrays → la GPU dibuja la forma en pantalla
```

---

### `Escena.java` — El director de arte

Usa al `Renderer` y sabe exactamente **qué dibujar** en cada momento del juego.
Recibe el estado actual del juego desde `AppFlappyBird` antes de cada frame.

**¿Qué dibuja?**

| Método | Qué pinta |
|---|---|
| `dibujarFondo()` | Cielo degradado, sol, montañas, nubes animadas, suelo |
| `dibujarNube()` | Una nube individual con sombra y 4 quads solapados |
| `dibujarPajaro()` | Cuerpo + cola + ala animada + pico + ojo + pupila |
| `dibujarTuberias()` | Los obstáculos verdes con brillo, sombra y capuchón |
| `dibujarHUD()` | Barra superior con puntajes y barra de nivel |
| `dibujarPantallaInicio()` | Pantalla de bienvenida con pájaros flotando |
| `dibujarPantallaGameOver()` | Resultados, barras de puntaje y corona al ganador |
| `dibujarCuentaRegresiva()` | El 3-2-1 con pulso de tamaño antes de empezar |
| `dibujarDigito()` | Un dígito de 7 segmentos (como los marcadores digitales) |
| `dibujarNumero()` | Un número completo usando varios dígitos de 7 segmentos |

**Estado que recibe de AppFlappyBird antes de dibujar:**
```java
escena.started              // ¿ya empezó el juego?
escena.gameOver             // ¿terminó el juego?
escena.enCuentaRegresiva    // ¿está en el 3-2-1?
escena.tiempoCuentaRegresiva// cuánto tiempo queda del 3-2-1
escena.nivelActual          // nivel actual (1-5)
```

**Los objetos `birds`, `tuberias` y `nubes` se comparten por referencia** — Escena siempre ve el estado más reciente sin que AppFlappyBird tenga que enviárselos cada frame.

---

### `AppFlappyBird.java` — El cerebro del juego

Es la clase **más importante**. Controla toda la lógica y coordina a todos los demás.

**Estado que maneja:**
```java
Bird[] birds          // los 3 pájaros (J1, J2, J3)
List<Tuberia> tuberias// lista de tuberías en pantalla
float[][] nubes       // posición y escala de cada nube
boolean started       // ¿el juego arrancó?
boolean gameOver      // ¿todos los pájaros murieron?
int nivelActual       // nivel 1 al 5
float velocidadTuberias  // qué tan rápido se mueven
float tiempoEntreTuberias// cada cuánto aparece una nueva tubería
float alturaHueco     // qué tan grande es el hueco (se achica con el nivel)
```

**Métodos de lógica:**

| Método | Qué hace |
|---|---|
| `procesarInput()` | Lee el teclado: salto, reinicio, ESC |
| `actualizar(dt)` | Actualiza TODO el estado del juego en un frame |
| `actualizarPajaros(dt)` | Aplica gravedad, mueve pájaros, detecta techo/suelo |
| `actualizarTuberias(dt)` | Mueve tuberías, da puntos, detecta colisiones |
| `actualizarNubes(dt)` | Mueve las nubes de derecha a izquierda (siempre) |
| `generarTuberia()` | Crea una nueva tubería en posición aleatoria |
| `colisionaConTuberia()` | AABB: detecta si el pájaro chocó con una tubería |
| `calcularNivel()` | Sube de nivel según el puntaje máximo |
| `resetGame()` | Reinicia todo para una nueva partida |
| `actualizarTitulo()` | Actualiza el texto de la barra de la ventana |
| `ambosEliminados()` | Retorna true si todos los pájaros están muertos |

---

## El flujo completo de un frame (60 veces por segundo)

```
┌──────────────────────────────────────────────────────┐
│                   AppFlappyBird                      │
│                                                      │
│  1. procesarInput()                                  │
│     → Lee teclado                                    │
│     → Si tecla de salto → bird.velocidadY = IMPULSO  │
│     → Si ESC → cerrar ventana                        │
│                                                      │
│  2. actualizar(dt)                                   │
│     → actualizarNubes(dt)    ← siempre se mueven     │
│     → actualizarPajaros(dt)  ← gravedad + física     │
│     → actualizarTuberias(dt) ← mover + colisiones    │
│     → calcularNivel()        ← ¿subió de nivel?      │
│                                                      │
│  3. Sincronizar estado → Escena                      │
│     escena.started = started                         │
│     escena.gameOver = gameOver                       │
│     escena.nivelActual = nivelActual                 │
│                                                      │
│  4. escena.dibujarEscena(tiempo)                     │
│       → dibujarFondo()                               │
│       → dibujarTuberias()                            │
│       → dibujarPajaro() x3                           │
│       → dibujarHUD()                                 │
│       → pantalla según estado                        │
│           ↓                                          │
│     Renderer.dibujarRectangulo/Triangulo(...)        │
│           ↓                                          │
│        GPU pinta en pantalla                         │
│                                                      │
│  5. glfwSwapBuffers() ← muestra el frame terminado   │
│  6. glfwPollEvents() ← procesa eventos del SO        │
└──────────────────────────────────────────────────────┘
```

---

## La física del salto explicada

```
Sin saltar (cayendo):
  velocidadY += GRAVEDAD * dt   // velocidadY se vuelve más negativa
  y += velocidadY * dt          // y baja cada frame

Al saltar (presionar tecla):
  velocidadY = IMPULSO_SALTO    // se setea a positivo de golpe (sube)
  
  Luego la gravedad lo frena y eventualmente lo hace caer de nuevo:
  frame 1: velocidadY = 0.85
  frame 2: velocidadY = 0.85 - 1.9*dt  (más chico)
  frame 3: velocidadY = ...             (sigue bajando)
  frame N: velocidadY < 0               (ya está cayendo)
```

---

## Detección de colisiones (AABB)

AABB = Axis-Aligned Bounding Box (caja delimitadora alineada a los ejes)

```
Primero: ¿hay superposición en X?
  pájaro_derecha > tubería_izquierda  AND  pájaro_izquierda < tubería_derecha

Si hay superposición en X, entonces:
  ¿el pájaro está FUERA del hueco en Y?
    pájaro_arriba > borde_superior_hueco   (chocó arriba)
    OR
    pájaro_abajo  < borde_inferior_hueco   (chocó abajo)

Si ambas condiciones → colisión → bird.matar()
```

---

## Dificultad progresiva (niveles 1 al 5)

```
Cada 5 puntos sube un nivel (PUNTOS_POR_NIVEL = 5)

Al subir de nivel:
  velocidadTuberias   = VEL_BASE   + (nivel-1) * VEL_PASO    // tuberías más rápidas
  tiempoEntreTuberias = SPAWN_BASE - (nivel-1) * SPAWN_PASO  // aparecen más seguido
  alturaHueco         = GAP_ALTO_BASE - ...                  // hueco más chico

Nivel 1 → verde  (más fácil, hueco grande, tuberías lentas)
Nivel 5 → púrpura (más difícil, hueco pequeño, tuberías rápidas)
```

---

## Los Shaders — explicación completa

### ¿Qué es un Shader?

Un **shader** es un programa pequeño que **corre directamente en la GPU** (tarjeta gráfica), no en la CPU.
Se escribe en un lenguaje llamado **GLSL** (parecido a C).

En este juego hay **dos shaders**, ambos viven en `Renderer.java` líneas 33-52.

---

### Shader 1 — Vertex Shader (`Renderer.java` líneas 33-45)

```glsl
#version 330 core
layout (location = 0) in vec3 aPos;   // recibe la posición de cada vértice
uniform vec2  uOffset;                 // posición en pantalla (x, y)   ← viene de Java
uniform vec2  uScale;                  // tamaño (ancho, alto)           ← viene de Java
uniform float uRotation;               // ángulo de rotación             ← viene de Java

void main() {
    vec2 s = aPos.xy * uScale;         // 1. aplica el tamaño
    float c = cos(uRotation), ss = sin(uRotation);
    vec2 r = vec2(s.x*c - s.y*ss,     // 2. aplica la rotación (matriz 2D)
                  s.x*ss + s.y*c);
    gl_Position = vec4(r + uOffset,    // 3. aplica la posición final
                       aPos.z, 1.0);
}
```

**¿Qué hace?**
Se ejecuta **una vez por cada vértice**. Un rectángulo tiene 6 vértices (2 triángulos), así que corre 6 veces por rectángulo.

```
Vértice base (del VAO)  →  Vertex Shader  →  Posición final en pantalla
(-0.5, -0.5) esquina       * escala           (x - ancho/2, y - alto/2)
( 0.5, -0.5) esquina       + rotación         (x + ancho/2, y - alto/2)
( 0.5,  0.5) esquina       + offset           (x + ancho/2, y + alto/2)
    ...                                             ...
```

---

### Shader 2 — Fragment Shader (`Renderer.java` líneas 47-52)

```glsl
#version 330 core
uniform vec3 uColor;           // color recibido desde Java (r, g, b)  ← viene de Java
out vec4 fragColor;            // color que sale al pixel de pantalla

void main() {
    fragColor = vec4(uColor, 1.0);   // pinta el pixel con ese color (alpha=1 = opaco)
}
```

**¿Qué hace?**
Se ejecuta **una vez por cada pixel** que cubre la forma. Si dibujas un rectángulo de 100x100 píxeles, este shader corre 10,000 veces.

```
Pixel en pantalla → Fragment Shader → Color final del pixel
   (200, 150)        uColor=(1,0,0)      ROJO
   (201, 150)        uColor=(1,0,0)      ROJO
   (202, 150)        uColor=(1,0,0)      ROJO
       ...                ...              ...
```

---

### Las variables `uniform` — cómo Java habla con los shaders

Las `uniform` son variables que **Java le envía a la GPU** antes de dibujar cada forma.

**Primero se guardan las "direcciones"** (`Renderer.java` líneas 64-67):
```java
uOffsetLoc = GL20.glGetUniformLocation(programaShader, "uOffset");
uScaleLoc  = GL20.glGetUniformLocation(programaShader, "uScale");
uColorLoc  = GL20.glGetUniformLocation(programaShader, "uColor");
uRotLoc    = GL20.glGetUniformLocation(programaShader, "uRotation");
```

**Luego, cada vez que se dibuja una forma** (`Renderer.java` líneas 120-123):
```java
GL20.glUniform2f(uOffsetLoc, x, y);        // posición
GL20.glUniform2f(uScaleLoc, ancho, alto);  // tamaño
GL20.glUniform1f(uRotLoc, rot);            // rotación
GL20.glUniform3f(uColorLoc, r, g, b);     // color
```

---

### El proceso completo de compilación de shaders

```
Paso 1 — COMPILAR cada shader por separado (líneas 54-55)
   compilarShader(vertexShader,   GL_VERTEX_SHADER)   → id del VS en GPU
   compilarShader(fragmentShader, GL_FRAGMENT_SHADER) → id del FS en GPU

Paso 2 — CREAR un programa y ADJUNTAR los shaders (líneas 57-60)
   glCreateProgram()          → crea el contenedor
   glAttachShader(prog, vs)   → adjunta Vertex Shader
   glAttachShader(prog, fs)   → adjunta Fragment Shader
   glLinkProgram(prog)        → los une en un solo programa ejecutable

Paso 3 — USAR el programa al dibujar (Escena.java)
   glUseProgram(programaShader) → activa el programa en la GPU

Paso 4 — ENVIAR datos con glUniform y DIBUJAR
   glUniform...()              → envía posición, tamaño, color
   glDrawArrays()              → la GPU ejecuta los shaders y pinta en pantalla
```

---

### Diferencia clave entre los dos shaders

| | Vertex Shader | Fragment Shader |
|---|---|---|
| **¿Cuándo corre?** | Una vez por vértice | Una vez por pixel |
| **¿Qué decide?** | DÓNDE va cada punto | DE QUÉ COLOR es cada pixel |
| **Input** | Posición del vértice (aPos) | — |
| **Output** | `gl_Position` (posición final) | `fragColor` (color del pixel) |
| **En este juego** | Aplica escala + rotación + offset | Pinta con el color uniforme |

---

### Frase para el examen

> El **Vertex Shader** decide *dónde* va cada vértice en pantalla (aplica posición, tamaño y rotación usando matrices). El **Fragment Shader** decide *de qué color* es cada pixel de la forma. Ambos están en `Renderer.java` líneas 33-52, escritos en GLSL y compilados en la GPU al iniciar el juego.

---

## Velocidad progresiva — cómo y cuándo sube

### ¿Cuándo se dispara el aumento?

Cada vez que un pájaro pasa una tubería y suma un punto, se llama `calcularNivel()`.
Esto ocurre en `AppFlappyBird.java` línea 212:

```java
if (alguienPuntuo) { calcularNivel(); }
//                    ↑ se llama aquí, justo después de sumar el punto
```

---

### ¿Qué hace `calcularNivel()`? (líneas 239-249)

```java
private void calcularNivel() {
    int pMax = 0;
    for (Bird p : birds) pMax = Math.max(pMax, p.score); // toma el puntaje más alto

    int nivelNuevo = Math.min(pMax / PUNTOS_POR_NIVEL + 1, NIVEL_MAX);
    //  pMax = 0→4  → nivel 1   (0/5 + 1 = 1)
    //  pMax = 5→9  → nivel 2   (5/5 + 1 = 2)
    //  pMax = 10→14→ nivel 3   (10/5 + 1 = 3)
    //  pMax = 15→19→ nivel 4   (15/5 + 1 = 4)
    //  pMax = 20+  → nivel 5   (tope = NIVEL_MAX)

    if (nivelNuevo != nivelActual) {          // ¿realmente subió de nivel?
        nivelActual         = nivelNuevo;
        velocidadTuberias   = VEL_BASE + (nivelActual - 1) * VEL_PASO;    // más rápido
        tiempoEntreTuberias = SPAWN_BASE - (nivelActual - 1) * SPAWN_PASO; // más seguidas
        alturaHueco         = GAP_ALTO_BASE - ...;                          // hueco más chico
    }
}
```

**El nivel se basa en el puntaje más alto** entre los 3 jugadores, no en el individual.

---

### Los números exactos nivel por nivel

Valores en `Constantes.java`:
- `VEL_BASE = 0.62` — velocidad inicial de las tuberías
- `VEL_PASO = 0.15` — cuánto aumenta la velocidad por nivel
- `SPAWN_BASE = 1.50s` — tiempo inicial entre tuberías
- `SPAWN_PASO = 0.13s` — cuánto se reduce ese tiempo por nivel
- `PUNTOS_POR_NIVEL = 5` — puntos necesarios para subir un nivel

| Nivel | Puntaje necesario | Velocidad tuberías | Tiempo entre tuberías | Color HUD |
|---|---|---|---|---|
| 1 | 0 pts  | 0.62 | 1.50s | Verde    |
| 2 | 5 pts  | 0.77 | 1.37s | Amarillo |
| 3 | 10 pts | 0.92 | 1.24s | Naranja  |
| 4 | 15 pts | 1.07 | 1.11s | Rojo     |
| 5 | 20 pts | 1.22 | 0.98s | Púrpura  |

---

### ¿Dónde se aplica la velocidad?

En `AppFlappyBird.java` línea 204, cada frame:
```java
t.x -= velocidadTuberias * dt;
// cada tubería se mueve este valor hacia la izquierda por cada frame
```

---

### Flujo completo en una línea

```
Pájaro pasa tubería → score++ → calcularNivel() → ¿pMax/5 subió de nivel?
    → SÍ → velocidadTuberias += 0.15  |  hueco se achica  |  tuberías más seguidas
    → NO → nada cambia, sigue igual hasta el próximo punto
```

---

## `actualizarPajaros(dt)` — explicado línea por línea

`dt` = **delta time** = cuántos segundos pasaron desde el último frame (normalmente ~0.016s a 60fps)

```java
for (Bird p : birds) {
```
Recorre los 3 pájaros uno por uno. Todo lo que está adentro se repite para J1, J2 y J3.

---

### BLOQUE 1 — El parpadeo de muerte

```java
if (p.tiempoFlash > 0f)
    p.tiempoFlash = Math.max(0f, p.tiempoFlash - dt);
```
Cuando un pájaro muere, tiene `tiempoFlash = 0.5f` segundos de parpadeo.
Cada frame le resta el tiempo transcurrido hasta llegar a 0.
```
frame 1: tiempoFlash = 0.50
frame 2: tiempoFlash = 0.484
frame 3: tiempoFlash = 0.468
...
frame N: tiempoFlash = 0.0  → deja de parpadear
```

---

### BLOQUE 2 — Si el pájaro está MUERTO

```java
if (!p.alive) {
    p.velocidadY += GRAVEDAD * dt;    // sigue acelerando hacia abajo
    p.y          += p.velocidadY * dt; // sigue cayendo
    continue;                          // salta todo lo demás y pasa al siguiente pájaro
}
```
El pájaro muerto **sigue cayendo** sin límite de velocidad hasta salir de pantalla.
El `continue` es clave: ignora todo el código de abajo y pasa al siguiente pájaro del `for`.

---

### BLOQUE 3 — Animación del ala (solo pájaros vivos)

```java
p.anguloAla += dt * (Math.abs(p.velocidadY) * 2.5f + 5f);
```
El ala bate más rápido cuando el pájaro va más rápido.
```
Si velocidadY = 0.0 → anguloAla sube  5.0 por segundo (aleteo suave)
Si velocidadY = 1.0 → anguloAla sube  7.5 por segundo (aleteo medio)
Si velocidadY = 1.8 → anguloAla sube  9.5 por segundo (aleteo rápido)
```
`Escena` usa este ángulo para dibujar el ala en posiciones distintas cada frame → animación.

---

### BLOQUE 4 — Impulso especial al llegar a 5 puntos

```java
if (p.score != p.puntajeAnterior) {        // ¿cambió el puntaje este frame?
    if (p.score > 0 && p.score % 5 == 0)   // ¿es múltiplo de 5? (5, 10, 15...)
        p.velocidadY = 1.5f;               // ← sube disparado hacia arriba
    p.puntajeAnterior = p.score;           // guarda para no repetirlo
}
```
```
score=4  → sin cambio
score=5  → ¡cambió! 5%5=0  → velocidadY=1.5 (sube solo)
score=5  → ya registrado, no se repite
score=6  → cambió, pero 6%5=1 → nada
score=10 → ¡cambió! 10%5=0 → velocidadY=1.5 (sube solo otra vez)
```

---

### BLOQUE 5 — Física de gravedad

```java
p.velocidadY += GRAVEDAD * dt;           // GRAVEDAD=-1.9 → cada frame baja la velocidad
if (p.velocidadY < VELOCIDAD_MAX_CAIDA)  // si cae muy rápido, lo limita
    p.velocidadY = VELOCIDAD_MAX_CAIDA;  // VELOCIDAD_MAX_CAIDA=-1.8 (tope de caída)
p.y += p.velocidadY * dt;               // mueve el pájaro según su velocidad
```
Ejemplo con un salto:
```
Salto:   velocidadY =  0.85  → sube rápido
frame 2: velocidadY =  0.82  → sube (más lento)
frame 3: velocidadY =  0.79  → sube (más lento aún)
...
frame N: velocidadY =  0.00  → en el pico, no sube ni baja
frame M: velocidadY = -0.10  → empieza a caer
...
frame Z: velocidadY = -1.80  → tope máximo de caída (ya no acelera más)
```

---

### BLOQUE 6 — Colisión con suelo y techo

```java
if (p.y - BIRD_ALTO * 0.5f <= SUELO_Y + SUELO_ALTO * 0.5f)  p.matar(); // toca suelo
if (p.y + BIRD_ALTO * 0.5f >= 1f)                             p.matar(); // toca techo
```
```
p.y es el CENTRO del pájaro.

Borde inferior = p.y - (BIRD_ALTO / 2)  → si llega al suelo → matar()
Borde superior = p.y + (BIRD_ALTO / 2)  → si llega al techo (y=1.0) → matar()
```

---

### Diagrama completo del método

```
Para cada pájaro en birds[]:
        ↓
¿tiempoFlash > 0? → reducirlo (cuenta el parpadeo)
        ↓
¿está muerto (alive=false)?
   → SÍ → cae libremente con gravedad → continue (salta al siguiente)
   → NO → continúa abajo
        ↓
Animar ala (anguloAla sube según velocidad)
        ↓
¿Cambió el puntaje y es múltiplo de 5?
   → SÍ → velocidadY = 1.5 (impulso hacia arriba automático)
        ↓
Aplicar gravedad  → velocidadY baja cada frame
Limitar caída     → velocidadY no puede bajar de -1.8
Mover pájaro      → y += velocidadY * dt
        ↓
¿Tocó el suelo? → matar()
¿Tocó el techo? → matar()
```

---

## ¿A cuántas tuberías termina el juego?

### Respuesta corta: el juego NO termina por tuberías

El juego termina cuando los **3 pájaros están muertos** al mismo tiempo. No existe un contador de tuberías. Teóricamente puede durar infinitas tuberías si los jugadores no mueren.

---

### ¿Cómo muere un pájaro? — 3 formas (`AppFlappyBird.java`)

```java
// 1. Toca el SUELO (línea 189)
if (p.y - BIRD_ALTO * 0.5f <= SUELO_Y + SUELO_ALTO * 0.5f)
    p.matar();

// 2. Toca el TECHO (línea 190)
if (p.y + BIRD_ALTO * 0.5f >= 1f)
    p.matar();

// 3. Choca con una TUBERÍA (línea 220)
if (p.alive && colisionaConTuberia(t, p.y))
    p.matar();
```

---

### ¿Cuándo se activa el Game Over? — líneas 150 y 153

```java
actualizarPajaros(dt);
if (ambosEliminados()) { gameOver = true; }  // ← chequea tras mover pájaros

actualizarTuberias(dt);
if (ambosEliminados()) { gameOver = true; }  // ← chequea tras colisiones con tuberías
```

Se chequea **dos veces por frame** para no perder ningún caso.

---

### El método que decide si terminó — líneas 268-270

```java
private boolean ambosEliminados() {
    for (Bird b : birds) if (b.alive) return false; // si alguno vive → sigue el juego
    return true;                                     // todos muertos  → Game Over
}
```

---

### Flujo del Game Over

```
Algún pájaro choca/cae
    ↓
bird.matar() → alive = false
    ↓
ambosEliminados() → ¿todos tienen alive=false?
    → NO → el juego sigue (al menos uno vive)
    → SÍ → gameOver = true → se muestra pantalla Game Over
    ↓
Jugador presiona SPACE / ENTER / R
    ↓
resetGame() → todos los pájaros vuelven a alive=true → nueva partida
```

---

## Relación entre clases (resumen)

```
Constantes      → define los números del juego (nadie la instancia, solo se lee)
Bird            → dato puro del jugador (AppFlappyBird la modifica, Escena la lee)
Tuberia         → dato puro del obstáculo (AppFlappyBird la modifica, Escena la lee)
Renderer        → sabe hablar con OpenGL/GPU (solo Escena lo usa)
Escena          → sabe qué dibujar (usa Renderer + lee Bird[] y Tuberia[])
AppFlappyBird   → cerebro: crea todo, corre el bucle, maneja la lógica
```
