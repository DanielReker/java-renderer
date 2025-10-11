package io.github.danielreker.javarenderer.core;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.RenderBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class Renderer {

    public <V, V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase>
    void render(
            FrameBuffer targetFrameBuffer,
            ShaderProgram<V_IO, F_IO> program,
            VertexBuffer<V> vbo,
            PrimitiveType mode,
            int first,
            int count
    ) {
        Objects.requireNonNull(targetFrameBuffer, "Target FrameBuffer cannot be null.");
        Objects.requireNonNull(program, "ShaderProgram cannot be null.");
        Objects.requireNonNull(vbo, "VertexBuffer cannot be null.");
        if (vbo.getVertexCount() == 0) return;

        List<V_IO> processedVertices = processVertices(vbo.streamRange(first, count), program).toList();

        if (mode == PrimitiveType.TRIANGLES) {
            assembleAndRasterizeTriangles(processedVertices, program, targetFrameBuffer);
        } else {
            System.err.println("Warning: PrimitiveType " + mode + " not yet supported. Only TRIANGLES.");
        }
    }

    private <V, V_IO extends VertexShaderIoBase> Stream<V_IO> processVertices(
            Stream<V> verticesStream,
            ShaderProgram<V_IO, ?> program
    ) {
        return verticesStream.map(vertexObject -> {
            V_IO vsIo = program.createAndPrepareVertexIO(vertexObject);
            if (vsIo != null) {
                program.executeVertexShader(vsIo);
            } else {
                System.err.println("Warning: Failed to create Vertex I/O Object from Vertex Object "
                        + vertexObject.getClass().getSimpleName());
            }
            return vsIo;
        });
    }

    private <V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase>
    void assembleAndRasterizeTriangles(
            List<V_IO> allProcessedVertices,
            ShaderProgram<V_IO, F_IO> program,
            FrameBuffer targetFrameBuffer
    ) {
        for (int i = 0; i < allProcessedVertices.size() - 2; i += 3) {
            V_IO v0_io = allProcessedVertices.get(i);
            V_IO v1_io = allProcessedVertices.get(i + 1);
            V_IO v2_io = allProcessedVertices.get(i + 2);
            rasterizeTriangle(v0_io, v1_io, v2_io, program, targetFrameBuffer);
        }
    }

    private <V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase>
    void rasterizeTriangle(
            V_IO v0_io, V_IO v1_io, V_IO v2_io,
            ShaderProgram<V_IO, F_IO> program,
            FrameBuffer targetFrameBuffer
    ) {
        Vector4f p0_clip = v0_io.gl_Position;
        Vector4f p1_clip = v1_io.gl_Position;
        Vector4f p2_clip = v2_io.gl_Position;


        final float NEAR_CLIP_PLANE_W = 0.0001f;

        if (p0_clip.w < NEAR_CLIP_PLANE_W && p1_clip.w < NEAR_CLIP_PLANE_W && p2_clip.w < NEAR_CLIP_PLANE_W) {
            return;
        }

        if (p0_clip.w < NEAR_CLIP_PLANE_W || p1_clip.w < NEAR_CLIP_PLANE_W || p2_clip.w < NEAR_CLIP_PLANE_W) {
            // TODO: Implement proper clipping
            return;
        }


        Vector3f p0_ndc = ndcFromClip(v0_io.gl_Position);
        Vector3f p1_ndc = ndcFromClip(v1_io.gl_Position);
        Vector3f p2_ndc = ndcFromClip(v2_io.gl_Position);

        float viewportWidth = targetFrameBuffer.getWidth();
        float viewportHeight = targetFrameBuffer.getHeight();

        Vector2f v0_screen = viewportTransform(p0_ndc, viewportWidth, viewportHeight);
        Vector2f v1_screen = viewportTransform(p1_ndc, viewportWidth, viewportHeight);
        Vector2f v2_screen = viewportTransform(p2_ndc, viewportWidth, viewportHeight);

        float w0_inv = 1.0f / v0_io.gl_Position.w;
        float w1_inv = 1.0f / v1_io.gl_Position.w;
        float w2_inv = 1.0f / v2_io.gl_Position.w;

        int minX = (int) Math.floor(Math.min(v0_screen.x, Math.min(v1_screen.x, v2_screen.x)));
        int maxX = (int) Math.ceil(Math.max(v0_screen.x, Math.max(v1_screen.x, v2_screen.x)));
        int minY = (int) Math.floor(Math.min(v0_screen.y, Math.min(v1_screen.y, v2_screen.y)));
        int maxY = (int) Math.ceil(Math.max(v0_screen.y, Math.max(v1_screen.y, v2_screen.y)));

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min((int) viewportWidth - 1, maxX);
        maxY = Math.min((int) viewportHeight - 1, maxY);

        RenderBuffer<Vector4f> colorBuffer = targetFrameBuffer.getColorAttachment();
        RenderBuffer<Float> depthBuffer = targetFrameBuffer.getDepthAttachment();

        float areaTriangle = edgeFunction(v0_screen, v1_screen, v2_screen);
        if (areaTriangle == 0) return;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Vector2f pixelCenter = new Vector2f(x + 0.5f, y + 0.5f);

                float b0 = edgeFunction(v1_screen, v2_screen, pixelCenter) / areaTriangle;
                float b1 = edgeFunction(v2_screen, v0_screen, pixelCenter) / areaTriangle;
                float b2 = edgeFunction(v0_screen, v1_screen, pixelCenter) / areaTriangle;

                if (b0 >= 0 && b1 >= 0 && b2 >= 0) {
                    float perspectiveCorrection = 1.0f / (b0 * w0_inv + b1 * w1_inv + b2 * w2_inv);

                    float interpolatedDepthNDC = (b0 * p0_ndc.z * w0_inv +
                            b1 * p1_ndc.z * w1_inv +
                            b2 * p2_ndc.z * w2_inv) * perspectiveCorrection;

                    float depthForBuffer = (interpolatedDepthNDC + 1.0f) * 0.5f;

                    if (depthBuffer == null || depthForBuffer < depthBuffer.getValue(x, y)) {
                        Map<String, Object> interpolatedVaryings =
                                interpolateVaryings(v0_io, v1_io, v2_io, b0, b1, b2, w0_inv, w1_inv, w2_inv,
                                        perspectiveCorrection, program);

                        Vector4f fragCoords = new Vector4f(pixelCenter.x, pixelCenter.y, depthForBuffer,
                                1.0f / ( (b0 * w0_inv + b1 * w1_inv + b2 * w2_inv) / perspectiveCorrection)  );

                        F_IO fsIo = program.createAndPrepareFragmentIO(interpolatedVaryings, fragCoords);
                        if (fsIo != null) {
                            program.executeFragmentShader(fsIo);

                            if (!fsIo.discarded) {
                                if (colorBuffer != null) {
                                    colorBuffer.setValue(x, y, fsIo.gl_FragColor);
                                }
                                if (depthBuffer != null) {
                                    float finalDepth = fsIo.gl_FragDepth != null ? fsIo.gl_FragDepth : depthForBuffer;
                                    depthBuffer.setValue(x, y, finalDepth);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Vector3f ndcFromClip(Vector4f clipCoords) {
        if (clipCoords.w == 0) return new Vector3f(clipCoords.x, clipCoords.y, clipCoords.z);
        float invW = 1.0f / clipCoords.w;
        return new Vector3f(clipCoords.x * invW, clipCoords.y * invW, clipCoords.z * invW);
    }

    private Vector2f viewportTransform(Vector3f ndcCoords, float viewportWidth, float viewportHeight) {
        float screenX = (ndcCoords.x + 1.0f) * 0.5f * viewportWidth;
        float screenY = (ndcCoords.y + 1.0f) * 0.5f * viewportHeight;
        return new Vector2f(screenX, screenY);
    }

    private float edgeFunction(Vector2f a, Vector2f b, Vector2f p) {
        return (p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x);
    }

    private <V_IO extends VertexShaderIoBase> Map<String, Object> interpolateVaryings(
            V_IO v0_io, V_IO v1_io, V_IO v2_io,
            float b0, float b1, float b2,
            float w0_inv, float w1_inv, float w2_inv,
            float perspectiveCorrection,
            ShaderProgram<V_IO, ?> program
    ) {
        Map<String, Object> interpolatedVaryings = new HashMap<>();

        program.getVertexShaderVaryingOutputFields().forEach((name, field) -> {
            try {
                Object val0 = field.get(v0_io);
                Object val1 = field.get(v1_io);
                Object val2 = field.get(v2_io);

                switch (val0) {
                    case Vector4f vector4f when val1 instanceof Vector4f && val2 instanceof Vector4f -> {
                        Vector4f v = new Vector4f();
                        v.x = (b0 * vector4f.x * w0_inv + b1 * ((Vector4f) val1).x * w1_inv + b2 * ((Vector4f) val2).x * w2_inv) * perspectiveCorrection;
                        v.y = (b0 * vector4f.y * w0_inv + b1 * ((Vector4f) val1).y * w1_inv + b2 * ((Vector4f) val2).y * w2_inv) * perspectiveCorrection;
                        v.z = (b0 * vector4f.z * w0_inv + b1 * ((Vector4f) val1).z * w1_inv + b2 * ((Vector4f) val2).z * w2_inv) * perspectiveCorrection;
                        v.w = (b0 * vector4f.w * w0_inv + b1 * ((Vector4f) val1).w * w1_inv + b2 * ((Vector4f) val2).w * w2_inv) * perspectiveCorrection;
                        interpolatedVaryings.put(name, v);
                    }
                    case Vector3f vector3f when val1 instanceof Vector3f && val2 instanceof Vector3f -> {
                        Vector3f v = new Vector3f();
                        v.x = (b0 * vector3f.x * w0_inv + b1 * ((Vector3f) val1).x * w1_inv + b2 * ((Vector3f) val2).x * w2_inv) * perspectiveCorrection;
                        v.y = (b0 * vector3f.y * w0_inv + b1 * ((Vector3f) val1).y * w1_inv + b2 * ((Vector3f) val2).y * w2_inv) * perspectiveCorrection;
                        v.z = (b0 * vector3f.z * w0_inv + b1 * ((Vector3f) val1).z * w1_inv + b2 * ((Vector3f) val2).z * w2_inv) * perspectiveCorrection;
                        interpolatedVaryings.put(name, v);
                    }
                    case Vector2f vector2f when val1 instanceof Vector2f && val2 instanceof Vector2f -> {
                        Vector2f v = new Vector2f();
                        v.x = (b0 * vector2f.x * w0_inv + b1 * ((Vector2f) val1).x * w1_inv + b2 * ((Vector2f) val2).x * w2_inv) * perspectiveCorrection;
                        v.y = (b0 * vector2f.y * w0_inv + b1 * ((Vector2f) val1).y * w1_inv + b2 * ((Vector2f) val2).y * w2_inv) * perspectiveCorrection;
                        interpolatedVaryings.put(name, v);
                    }
                    case Float v when val1 instanceof Float && val2 instanceof Float -> {
                        float f = (b0 * v * w0_inv + b1 * (Float) val1 * w1_inv + b2 * (Float) val2 * w2_inv) * perspectiveCorrection;
                        interpolatedVaryings.put(name, f);
                    }
                    default -> {
                        System.err.println("Warning: Varying '" + name + "' of type " + val0.getClass().getSimpleName() + " cannot be interpolated. Using value from first vertex.");
                        interpolatedVaryings.put(name, val0);
                    }
                }
            } catch (IllegalAccessException e) {
                System.err.println("Error interpolating varying " + name + ": " + e.getMessage());
            }
        });
        return interpolatedVaryings;
    }
}