package com.graphics.flappybird;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Capa OpenGL de bajo nivel.
 * Se encarga de compilar shaders, crear geometria base (VAO/VBO)
 * y exponer metodos para dibujar rectangulos y triangulos.
 */
class Renderer {

    int programaShader;
    private int vaoQuad, vboQuad, vaoTri, vboTri;
    int uOffsetLoc, uScaleLoc, uColorLoc, uRotLoc;

    Renderer() {
        crearShaders();
        crearGeometriaRectangulo();
        crearGeometriaTriangulo();
    }

    // -------------------------------------------------------------------------
    // Shaders
    // -------------------------------------------------------------------------
    private void crearShaders() {
        // Vertex shader: ubica cada vertice segun posicion, tamano y rotacion.
        String vertexShader = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                uniform vec2  uOffset;
                uniform vec2  uScale;
                uniform float uRotation;
                void main() {
                    vec2 s = aPos.xy * uScale;
                    float c = cos(uRotation), ss = sin(uRotation);
                    vec2 r = vec2(s.x*c - s.y*ss, s.x*ss + s.y*c);
                    gl_Position = vec4(r + uOffset, aPos.z, 1.0);
                }
                """;
        // Fragment shader: pinta cada pixel con el color uniforme recibido.
        String fragmentShader = """
                #version 330 core
                uniform vec3 uColor;
                out vec4 fragColor;
                void main() { fragColor = vec4(uColor, 1.0); }
                """;

        int vs = compilarShader(vertexShader,   GL20.GL_VERTEX_SHADER,   "VS");
        int fs = compilarShader(fragmentShader, GL20.GL_FRAGMENT_SHADER, "FS");

        programaShader = GL20.glCreateProgram();
        GL20.glAttachShader(programaShader, vs);
        GL20.glAttachShader(programaShader, fs);
        GL20.glLinkProgram(programaShader);
        if (GL20.glGetProgrami(programaShader, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Link: " + GL20.glGetProgramInfoLog(programaShader));

        uOffsetLoc = GL20.glGetUniformLocation(programaShader, "uOffset");
        uScaleLoc  = GL20.glGetUniformLocation(programaShader, "uScale");
        uColorLoc  = GL20.glGetUniformLocation(programaShader, "uColor");
        uRotLoc    = GL20.glGetUniformLocation(programaShader, "uRotation");

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
    }

    private int compilarShader(String codigo, int tipo, String nombre) {
        int id = GL20.glCreateShader(tipo);
        GL20.glShaderSource(id, codigo);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(nombre + ": " + GL20.glGetShaderInfoLog(id));
        return id;
    }

    // -------------------------------------------------------------------------
    // Geometria base (VAO / VBO)
    // -------------------------------------------------------------------------
    private int[] crearVAO(float[] vertices) {
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    private void crearGeometriaRectangulo() {
        int[] r = crearVAO(new float[]{
                -0.5f, -0.5f, 0f,  0.5f, -0.5f, 0f,  0.5f,  0.5f, 0f,
                -0.5f, -0.5f, 0f,  0.5f,  0.5f, 0f, -0.5f,  0.5f, 0f});
        vaoQuad = r[0];
        vboQuad = r[1];
    }

    private void crearGeometriaTriangulo() {
        int[] r = crearVAO(new float[]{-0.5f, -0.5f, 0f,  0.5f, 0f, 0f, -0.5f, 0.5f, 0f});
        vaoTri = r[0];
        vboTri = r[1];
    }

    // -------------------------------------------------------------------------
    // Primitivas de dibujo
    // -------------------------------------------------------------------------
    void dibujarRectangulo(float x, float y, float ancho, float alto, float rot,
                            float r, float g, float b) {
        GL30.glBindVertexArray(vaoQuad);
        GL20.glUniform2f(uOffsetLoc, x, y);
        GL20.glUniform2f(uScaleLoc, ancho, alto);
        GL20.glUniform1f(uRotLoc, rot);
        GL20.glUniform3f(uColorLoc, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    void dibujarRectangulo(float x, float y, float ancho, float alto,
                            float r, float g, float b) {
        dibujarRectangulo(x, y, ancho, alto, 0f, r, g, b);
    }

    void dibujarTriangulo(float x, float y, float ancho, float alto, float rot,
                           float r, float g, float b) {
        GL30.glBindVertexArray(vaoTri);
        GL20.glUniform2f(uOffsetLoc, x, y);
        GL20.glUniform2f(uScaleLoc, ancho, alto);
        GL20.glUniform1f(uRotLoc, rot);
        GL20.glUniform3f(uColorLoc, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    /** Rota el offset local (lx, ly) segun el angulo dado. */
    float[] rotarOffset(float lx, float ly, float angulo) {
        float c = (float) Math.cos(angulo), s = (float) Math.sin(angulo);
        return new float[]{lx * c - ly * s, lx * s + ly * c};
    }

    // -------------------------------------------------------------------------
    // Limpieza de recursos GPU
    // -------------------------------------------------------------------------
    void liberarRecursos() {
        GL30.glDeleteVertexArrays(vaoQuad);
        GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTri);
        GL15.glDeleteBuffers(vboTri);
        GL20.glDeleteProgram(programaShader);
    }
}
