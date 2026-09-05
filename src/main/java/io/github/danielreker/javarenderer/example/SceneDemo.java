package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.example.phong.*;
import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.obj.ObjFile;
import io.github.danielreker.javarenderer.obj.ObjObject;
import io.github.danielreker.javarenderer.obj.ObjParser;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.danielreker.javarenderer.math.Constants.DOUBLE_PI_F;
import static io.github.danielreker.javarenderer.math.Constants.PI_F;

public class SceneDemo extends Base3dDemo {

    record SceneObject(
            VertexBuffer<PhongVertex> vbo,
            Vector3f color,
            PhongMaterial material
    ) {}

    private final List<SceneObject> sceneObjects;

    private final ShaderProgram<PhongVertexShader.Io, PhongFragmentShader.Io> shaderProgram;


    private static final VertexBuffer<PhongVertex> LIGHT_SOURCE_VBO =
            generateSphereVbo(0.3f, 18, 18);

    public static final Vector3f LIGHT_SOURCE_INITIAL_POSITION = Vector3f.of(4.0f, 3.0f, 0.0f);

    private static final Vector3f LIGHT_SOURCE_COLOR = Vector3f.of(1.0f, 1.0f, 1.0f);

    private static final PhongMaterial LIGHT_SOURCE_MATERIAL = new PhongMaterial(
            1.0f, 0.0f, 0.0f, 0
    );

    private static final float LIGHT_SOURCE_ROTATION_SPEED_RAD_PER_SEC =
            (float) Math.toRadians(90.0f);

    private final AtomicReference<Boolean> lightSourceRotating =
            new AtomicReference<>(false);

    private final AtomicReference<Float> lightSourceAngleRad =
            new AtomicReference<>(0.0f);


    private final AtomicReference<Boolean> isPerspective =
            new AtomicReference<>(true);


    public SceneDemo() {
        super(800, 600, ".obj Scene Demo",
                Vector3f.of(0.1f, 0.1f, 0.1f), 100.0f);

        ObjFile sceneObj = ObjParser.parse(SceneDemo.class.getClassLoader()
                .getResourceAsStream("demo/scene.obj"));


        sceneObjects = List.of(
                new SceneObject(
                        buildVboFromObjObject(sceneObj.getObject("Solid").orElseThrow()),
                        new Vector3f(1.0f, 0.0f, 0.0f),
                        new PhongMaterial(0.20f, 0.40f, 0.85f, 128)
                ),
                new SceneObject(
                        buildVboFromObjObject(sceneObj.getObject("Конус").orElseThrow()),
                        new Vector3f(0.0f, 1.0f, 0.0f),
                        new PhongMaterial(0.20f, 0.40f, 0.85f, 128)
                ),
                new SceneObject(
                        buildVboFromObjObject(sceneObj.getObject("Cube").orElseThrow()),
                        new Vector3f(0.0f, 0.0f, 1.0f),
                        new PhongMaterial(0.20f, 0.40f, 0.85f, 128)
                )
        );

        shaderProgram = ShaderProgram.create(
                new PhongVertexShader(),
                new PhongFragmentShader()
        );
    }

    private static VertexBuffer<PhongVertex> buildVboFromObjObject(
            ObjObject objObject
    ) {
        List<PhongVertex> vertices = objObject
                .faces()
                .stream()
                .flatMap(face -> Stream
                        .of(face.v0(), face.v1(), face.v2()))
                .map(objVertex -> new PhongVertex(
                        objVertex.position(),
                        Optional.ofNullable(objVertex.textureCoordinates()).orElse(Vector2f.ZERO),
                        objVertex.normal()
                ))
                .toList();

        return VertexBuffer.create(vertices);
    }

    @SuppressWarnings("SameParameterValue")
    private static VertexBuffer<PhongVertex> generateSphereVbo(
            float radius,
            int uSegments,
            int vSegments
    ) {
        List<PhongVertex> vertices = new ArrayList<>();
        for (int uSegment = 0; uSegment <= uSegments; uSegment++) {
            float u = (float) uSegment / uSegments;
            float lon = u * DOUBLE_PI_F;
            for (int vSegment = 0; vSegment <= vSegments; vSegment++) {
                float v = (float) vSegment / vSegments;
                float lat = (v - 0.5f) * PI_F;

                float sinLon = (float) Math.sin(lon);
                float cosLon = (float) Math.cos(lon);
                float sinLat = (float) Math.sin(lat);
                float cosLat = (float) Math.cos(lat);

                Vector3f position = Vector3f.of(
                        cosLon * cosLat,
                        sinLat,
                        sinLon * cosLat
                ).multiply(radius);

                vertices.add(new PhongVertex(
                        position,
                        Vector2f.of(u, v),
                        position.normalize()
                ));
            }
        }

        List<PhongVertex> vboVertices = new ArrayList<>();
        for (int uSegment = 0; uSegment < uSegments; uSegment++) {
            for (int vSegment = 0; vSegment < vSegments; vSegment++) {
                vboVertices.add(vertices.get((vSegments + 1) * uSegment + vSegment));
                vboVertices.add(vertices.get((vSegments + 1) * (uSegment + 1) + vSegment + 1));
                vboVertices.add(vertices.get((vSegments + 1) * (uSegment + 1) + vSegment));

                vboVertices.add(vertices.get((vSegments + 1) * uSegment + vSegment));
                vboVertices.add(vertices.get((vSegments + 1) * (uSegment + 1) + vSegment + 1));
                vboVertices.add(vertices.get((vSegments + 1) * uSegment + vSegment + 1));
            }
        }

        return VertexBuffer.create(vboVertices);
    }

    @Override
    protected void render(FrameBuffer frameBuffer) {
        Matrix4f projection = isPerspective.get() ? Matrix4f.perspective(
                camera.getVerticalFovRad(),
                (float) frameWidth / frameHeight,
                0.1f,
                100.0f
        ) : Matrix4f.ortho(
                -5, 5, -5, 5, 0, 100
        );
        Matrix4f view = camera.getViewMatrix();

        shaderProgram.setUniform("projection", projection);
        shaderProgram.setUniform("view", view);

        shaderProgram.setUniform("viewPos", camera.getPosition());

        Matrix4f lightSourceModelMatrix = getLightSourceModelMatrix();
        shaderProgram.setUniform("lightSource", new PhongLightSource(
                Matrix4f.multiply(lightSourceModelMatrix, Vector3f.ZERO.withW(1.0f)).xyz(),
                LIGHT_SOURCE_COLOR
        ));

        sceneObjects.forEach(sceneObject -> {
            shaderProgram.setUniform("model", Matrix4f.IDENTITY);
            shaderProgram.setUniform("material", sceneObject.material);

            Function<Vector2f, Vector3f> objectColorTexture =
                    buildChessTexture(10.0f, sceneObject.color, Vector3f.of(1.0f, 1.0f, 1.0f));
            shaderProgram.setUniform("objectColorTexture", objectColorTexture);

            renderer.render(
                    frameBuffer, shaderProgram, sceneObject.vbo, PrimitiveType.TRIANGLES,
                    0, sceneObject.vbo.getVertexCount()
            );
        });

        shaderProgram.setUniform("model", lightSourceModelMatrix);
        shaderProgram.setUniform("material", LIGHT_SOURCE_MATERIAL);
        Function<Vector2f, Vector3f> lightSourceTexture =
                buildChessTexture(18.0f, LIGHT_SOURCE_COLOR, LIGHT_SOURCE_COLOR.multiply(0.8f));
        shaderProgram.setUniform("objectColorTexture", lightSourceTexture);

        renderer.render(
                frameBuffer, shaderProgram, LIGHT_SOURCE_VBO, PrimitiveType.TRIANGLES,
                0, LIGHT_SOURCE_VBO.getVertexCount()
        );
    }

    private static Function<Vector2f, Vector3f> buildChessTexture(
            float scale,
            Vector3f evenColor,
            Vector3f oddColor

    ) {
        return texCoord -> {
            final int checkX = (int) Math.floor(texCoord.x() * scale);
            final int checkY = (int) Math.floor(texCoord.y() * scale);

            if ((checkX + checkY) % 2 == 0) {
                return evenColor;
            } else {
                return oddColor;
            }
        };
    }

    private Matrix4f getLightSourceModelMatrix() {
        float angleRad = lightSourceAngleRad.get();
        return Matrix4f.multiply(
                Matrix4f.rotationAroundY(angleRad),
                Matrix4f.translation(LIGHT_SOURCE_INITIAL_POSITION)
        );
    }


    @Override
    protected void onKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_R) {
            lightSourceRotating.updateAndGet(current -> !current);
        } else if (keyCode == KeyEvent.VK_P) {
            isPerspective.set(true);
        } else if (keyCode == KeyEvent.VK_O) {
            isPerspective.set(false);
        }
    }

    @Override
    protected void processLogic(float deltaTimeSec) {
        if (lightSourceRotating.get()) {
            float deltaAngleRad = deltaTimeSec * LIGHT_SOURCE_ROTATION_SPEED_RAD_PER_SEC;

            lightSourceAngleRad.updateAndGet(current ->
                    ((current + deltaAngleRad) % DOUBLE_PI_F + DOUBLE_PI_F)
                            % DOUBLE_PI_F);
        }
    }

    public static void main(String[] args) throws AWTException {
        new SceneDemo().run();
    }
}
