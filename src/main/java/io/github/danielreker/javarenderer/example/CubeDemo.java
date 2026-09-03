package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.example.phong.*;
import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Function;

public class CubeDemo extends Base3dDemo {

    record CubeObject(
            Vector3f position,
            Vector3f chessColor
    ) {}

    private final List<CubeObject> cubes = List.of(
            new CubeObject(new Vector3f( 0.0f,  0.0f,  0.0f), new Vector3f( 1.0f,  1.0f, 1.0f)),
            new CubeObject(new Vector3f( 2.0f,  5.0f, -15.0f), new Vector3f( 0.0f,  0.0f,  0.0f)),
            new CubeObject(new Vector3f(-1.5f, -2.2f, -2.5f), new Vector3f( 1.0f,  0.0f, 0.0f)),
            new CubeObject(new Vector3f(-3.8f, -2.0f, -12.3f), new Vector3f( 0.0f,  1.0f, 0.0f)),
            new CubeObject(new Vector3f( 2.4f, -0.4f, -3.5f), new Vector3f( 0.0f,  0.0f, 1.0f)),
            new CubeObject(new Vector3f(-1.7f,  3.0f, -7.5f), new Vector3f( 1.0f,  1.0f, 0.0f)),
            new CubeObject(new Vector3f( 1.3f, -2.0f, -2.5f), new Vector3f( 1.0f,  0.0f, 1.0f)),
            new CubeObject(new Vector3f( 1.5f,  2.0f, -2.5f), new Vector3f( 0.0f,  1.0f, 1.0f)),
            new CubeObject(new Vector3f( 1.5f,  0.2f, -1.5f), new Vector3f( 0.5f,  0.5f, 0.5f)),
            new CubeObject(new Vector3f(0.0f,  0.0f, 2.0f), new Vector3f( 0.5f,  0.5f, 0.0f))
    );

    private final VertexBuffer<PhongVertex> cubeVbo;
    private final ShaderProgram<PhongVertexShader.Io, PhongFragmentShader.Io> cubeProgram;

    private final PhongMaterial cubeMaterial = new PhongMaterial(
            0.20f, 0.40f, 0.85f, 128
    );

    private final PhongLightSource lightSource = new PhongLightSource(
            Vector3f.ZERO,
            new Vector3f(1.0f, 1.0f, 1.0f)
    );

    private boolean rotating = false;

    public CubeDemo() {
        super(800, 600, "Cube Demo",
                Vector3f.of(0.1f, 0.1f, 0.1f));

        final List<PhongVertex> cubeVertexData = List.of(
                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f)),

                new PhongVertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, 0.0f, 1.0f)),

                new PhongVertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f)),

                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f)),

                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, -1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, -1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, -1.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f)),

                new PhongVertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector2f(1.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new PhongVertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector2f(1.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector2f(0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new PhongVertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector2f(0.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f))
        );
        cubeVbo = VertexBuffer.create(cubeVertexData);
        cubeProgram = ShaderProgram.create(
                new PhongVertexShader(),
                new PhongFragmentShader()
        );
    }

    @Override
    protected void render(FrameBuffer frameBuffer) {

        Matrix4f projection = Matrix4f.perspective(
                camera.getVerticalFovRad(),
                (float) frameWidth / frameHeight,
                0.1f,
                100.0f);
        Matrix4f view = camera.getViewMatrix();

        cubeProgram.setUniform("projection", projection);
        cubeProgram.setUniform("view", view);

        cubeProgram.setUniform("viewPos", camera.getPosition());
        cubeProgram.setUniform("lightSource", lightSource);
        cubeProgram.setUniform("material", cubeMaterial);

        cubes.forEach(cube -> {
            Matrix4f model = Matrix4f.translation(cube.position);
            float angle = rotating ? (System.nanoTime() / 1_000_000_000.0f) * 0.5f : 0.0f;
            if (cube.position.lengthSquared() > 0.1f && rotating) {
                angle += cube.position.x() + cube.position.y();
            }
            model = Matrix4f.multiply(model, Matrix4f.multiply(
                    Matrix4f.rotationAroundX(angle),
                    Matrix4f.rotationAroundY(angle / 2)
            ));
            cubeProgram.setUniform("model", model);

            Function<Vector2f, Vector3f> objectColorTexture = texCoord -> {
                float scale = 10.0f;
                final int checkX = (int) Math.floor(texCoord.x() * scale);
                final int checkY = (int) Math.floor(texCoord.y() * scale);

                if ((checkX + checkY) % 2 == 0) {
                    return Vector3f.of(1.0f, 1.0f, 1.0f);
                } else {
                    return cube.chessColor;
                }
            };
            cubeProgram.setUniform("objectColorTexture", objectColorTexture);

            renderer.render(frameBuffer, cubeProgram, cubeVbo, PrimitiveType.TRIANGLES, 0, cubeVbo.getVertexCount());
        });
    }

    @Override
    protected void onKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_R) {
            rotating = !rotating;
        }
    }

    public static void main(String[] args) throws AWTException {
        new CubeDemo().run();
    }
}
