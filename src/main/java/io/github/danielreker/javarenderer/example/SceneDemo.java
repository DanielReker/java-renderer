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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class SceneDemo extends Base3dDemo {

    record SceneObject(
            VertexBuffer<PhongVertex> vbo,
            Vector3f color,
            PhongMaterial material
    ) {}

    private final List<SceneObject> sceneObjects;

    private final ShaderProgram<PhongVertexShader.Io, PhongFragmentShader.Io> shaderProgram;

    private final PhongLightSource lightSource = new PhongLightSource(
            Vector3f.of(10.0f, 0.0f, 0.0f),
            new Vector3f(1.0f, 1.0f, 1.0f)
    );

    public SceneDemo() {
        super(800, 600, ".obj Scene Demo",
                Vector3f.of(0.1f, 0.1f, 0.1f));

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

    @Override
    protected void render(FrameBuffer frameBuffer) {

        Matrix4f projection = Matrix4f.perspective(
                camera.getVerticalFovRad(),
                (float) frameWidth / frameHeight,
                0.1f,
                100.0f);
        Matrix4f view = camera.getViewMatrix();

        shaderProgram.setUniform("projection", projection);
        shaderProgram.setUniform("view", view);

        shaderProgram.setUniform("viewPos", camera.getPosition());
        shaderProgram.setUniform("lightSource", lightSource);

        sceneObjects.forEach(sceneObject -> {
            shaderProgram.setUniform("model", Matrix4f.IDENTITY);
            shaderProgram.setUniform("material", sceneObject.material);

            Function<Vector2f, Vector3f> objectColorTexture = texCoord -> {
                float scale = 10.0f;
                final int checkX = (int) Math.floor(texCoord.x() * scale);
                final int checkY = (int) Math.floor(texCoord.y() * scale);

                if ((checkX + checkY) % 2 == 0) {
                    return Vector3f.of(1.0f, 1.0f, 1.0f);
                } else {
                    return sceneObject.color;
                }
            };
            shaderProgram.setUniform("objectColorTexture", objectColorTexture);

            renderer.render(
                    frameBuffer, shaderProgram, sceneObject.vbo, PrimitiveType.TRIANGLES,
                    0, sceneObject.vbo.getVertexCount()
            );
        });
    }

    public static void main(String[] args) throws AWTException {
        new SceneDemo().run();
    }
}
