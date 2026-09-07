package io.github.danielreker.javarenderer.core.rendering;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;

import java.util.List;

public class Renderer<V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase> {

    private final ShaderProgram<V_IO, F_IO> shaderProgram;

    private final PolygonClipper<V_IO> polygonClipper;

    private final TriangleRasterizer<V_IO, F_IO> triangleRasterizer;


    public Renderer(ShaderProgram<V_IO, F_IO> shaderProgram) {
        this.shaderProgram = shaderProgram;
        this.polygonClipper = new PolygonClipper<>(shaderProgram);
        triangleRasterizer = new TriangleRasterizer<>(shaderProgram);
    }


    public void render(
            FrameBuffer targetFrameBuffer,
            List<?> vbo,
            PrimitiveType mode
    ) {
        List<V_IO> processedVertices = vbo
                .stream()
                .map(vertexObject -> {
                    V_IO vsIo = shaderProgram.createAndPrepareVertexIO(vertexObject);
                    shaderProgram.executeVertexShader(vsIo);
                    return vsIo;
                })
                .toList();

        if (mode == PrimitiveType.TRIANGLES) {
            assembleAndRasterizeTriangles(processedVertices, targetFrameBuffer);
        } else {
            throw new IllegalArgumentException("PrimitiveType " + mode + " is not supported");
        }
    }

    private void assembleAndRasterizeTriangles(
            List<V_IO> allProcessedVertices,
            FrameBuffer targetFrameBuffer
    ) {
        for (int i = 0; i < allProcessedVertices.size() - 2; i += 3) {
            final List<V_IO> clippedVertices = polygonClipper
                    .clip(allProcessedVertices.subList(i, i + 3));

            for (int j = 1; j < clippedVertices.size() - 1; j++) {
                triangleRasterizer.rasterize(
                        clippedVertices.getFirst(),
                        clippedVertices.get(j),
                        clippedVertices.get(j + 1),
                        targetFrameBuffer
                );
            }
        }
    }

}