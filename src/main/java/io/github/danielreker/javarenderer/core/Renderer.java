package io.github.danielreker.javarenderer.core;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.RenderBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Renderer<V, V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase> {

    private final ShaderProgram<V_IO, F_IO> shaderProgram;

    public Renderer(ShaderProgram<V_IO, F_IO> shaderProgram) {
        this.shaderProgram = shaderProgram;
    }


    public void render(
            FrameBuffer targetFrameBuffer,
            VertexBuffer<V> vbo,
            PrimitiveType mode,
            int first,
            int count
    ) {
        List<V_IO> processedVertices = processVertices(vbo.streamRange(first, count)).toList();

        if (mode == PrimitiveType.TRIANGLES) {
            assembleAndRasterizeTriangles(processedVertices, targetFrameBuffer);
        } else {
            System.err.println("Warning: PrimitiveType " + mode + " not yet supported. Only TRIANGLES.");
        }
    }

    private Stream<V_IO> processVertices(
            Stream<V> verticesStream
    ) {
        return verticesStream.map(vertexObject -> {
            V_IO vsIo = shaderProgram.createAndPrepareVertexIO(vertexObject);
            if (vsIo != null) {
                shaderProgram.executeVertexShader(vsIo);
            } else {
                System.err.println("Warning: Failed to create Vertex I/O Object from Vertex Object "
                        + vertexObject.getClass().getSimpleName());
            }
            return vsIo;
        });
    }

    private void assembleAndRasterizeTriangles(
            List<V_IO> allProcessedVertices,
            FrameBuffer targetFrameBuffer
    ) {
        for (int i = 0; i < allProcessedVertices.size() - 2; i += 3) {
            final List<V_IO> clippedVertices =
                    clipPolygon(allProcessedVertices.subList(i, i + 3));

            for (int j = 1; j < clippedVertices.size() - 1; j++) {
                rasterizeTriangle(
                        clippedVertices.getFirst(),
                        clippedVertices.get(j),
                        clippedVertices.get(j + 1),
                        targetFrameBuffer
                );
            }
        }
    }

    private static final List<Vector4f> CLIPPING_PLANES = List.of(
            Vector4f.of(1, 0, 0, 1),
            Vector4f.of(-1, 0, 0, 1),
            Vector4f.of(0, 1, 0, 1),
            Vector4f.of(0, -1, 0, 1),
            Vector4f.of(0, 0, 1, 1),
            Vector4f.of(0, 0, -1, 1)
    );

    private List<V_IO> clipPolygon(
            List<V_IO> polygonVertices
    ) {
        for (final Vector4f clippingPlane : CLIPPING_PLANES) {
            polygonVertices = clipPolygonWithPlane(polygonVertices, clippingPlane);
        }
        return polygonVertices;
    }

    private List<V_IO> clipPolygonWithPlane(
            List<V_IO> polygonVertices,
            Vector4f plane
    ) {
        if (polygonVertices.isEmpty()) {
            return List.of();
        }

        final List<V_IO> result = new ArrayList<>();

        V_IO start = polygonVertices.getLast();
        boolean startInside = isInside(start, plane);
        for (final V_IO end : polygonVertices) {
            final boolean endInside = isInside(end, plane);

            if (endInside) {
                if (!startInside) {
                    result.add(calculateIntersection(start, end, plane));
                }
                result.add(end);
            } else if (startInside) {
                result.add(calculateIntersection(start, end, plane));
            }

            start = end;
            startInside = endInside;
        }

        return result;
    }

    private boolean isInside(
            V_IO vertex,
            Vector4f plane
    ) {
        return vertex.gl_Position.dot(plane) >= 0;
    }

    private V_IO calculateIntersection(
            V_IO startVertex,
            V_IO endVertex,
            Vector4f plane
    ) {
        final float dotStart = startVertex.gl_Position.dot(plane);
        final float dotEnd = endVertex.gl_Position.dot(plane);

        final float t = dotStart / (dotStart - dotEnd);

        V_IO intersection = shaderProgram.createAndPrepareVertexIO();

        for (final Map.Entry<String, Field> entry : shaderProgram.getVertexShaderVaryingOutputFields().entrySet()) {
            final String name = entry.getKey();
            final Field field = entry.getValue();
            try {
                Object startValue = field.get(startVertex);
                Object endValue = field.get(endVertex);

                MathOperations<?> mathOperations = MathOperations
                        .forClass(startValue.getClass());

                startValue = mathOperations.multiply(startValue, 1 - t);
                endValue = mathOperations.multiply(endValue, t);

                Object intersectionValue = mathOperations.add(startValue, endValue);

                field.set(intersection, intersectionValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error interpolating varying " + name, e);
            }
        }

        intersection.gl_Position = startVertex.gl_Position.multiply(1 - t)
                .add(endVertex.gl_Position.multiply(t));

        return intersection;
    }


    private void rasterizeTriangle(
            V_IO v0_io, V_IO v1_io, V_IO v2_io,
            FrameBuffer targetFrameBuffer
    ) {
        Vector3f p0_ndc = ndcFromClip(v0_io.gl_Position);
        Vector3f p1_ndc = ndcFromClip(v1_io.gl_Position);
        Vector3f p2_ndc = ndcFromClip(v2_io.gl_Position);

        float viewportWidth = targetFrameBuffer.getWidth();
        float viewportHeight = targetFrameBuffer.getHeight();

        Vector2f v0_screen = viewportTransform(p0_ndc, viewportWidth, viewportHeight);
        Vector2f v1_screen = viewportTransform(p1_ndc, viewportWidth, viewportHeight);
        Vector2f v2_screen = viewportTransform(p2_ndc, viewportWidth, viewportHeight);

        float w0_inv = 1.0f / v0_io.gl_Position.w();
        float w1_inv = 1.0f / v1_io.gl_Position.w();
        float w2_inv = 1.0f / v2_io.gl_Position.w();

        int minX = (int) Math.floor(Math.min(v0_screen.x(), Math.min(v1_screen.x(), v2_screen.x())));
        int maxX = (int) Math.ceil(Math.max(v0_screen.x(), Math.max(v1_screen.x(), v2_screen.x())));
        int minY = (int) Math.floor(Math.min(v0_screen.y(), Math.min(v1_screen.y(), v2_screen.y())));
        int maxY = (int) Math.ceil(Math.max(v0_screen.y(), Math.max(v1_screen.y(), v2_screen.y())));

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

                    float interpolatedDepthNDC = (b0 * p0_ndc.z() * w0_inv +
                            b1 * p1_ndc.z() * w1_inv +
                            b2 * p2_ndc.z() * w2_inv) * perspectiveCorrection;

                    float depthForBuffer = (interpolatedDepthNDC + 1.0f) * 0.5f;

                    if (depthBuffer == null || depthForBuffer < depthBuffer.getValue(x, y)) {
                        Map<String, Object> interpolatedVaryings =
                                interpolateVaryings(v0_io, v1_io, v2_io, b0, b1, b2, w0_inv, w1_inv, w2_inv,
                                        perspectiveCorrection);

                        Vector4f fragCoords = new Vector4f(pixelCenter.x(), pixelCenter.y(), depthForBuffer,
                                1.0f / ( (b0 * w0_inv + b1 * w1_inv + b2 * w2_inv) / perspectiveCorrection)  );

                        F_IO fsIo = shaderProgram.createAndPrepareFragmentIO(interpolatedVaryings, fragCoords);
                        if (fsIo != null) {
                            shaderProgram.executeFragmentShader(fsIo);

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
        if (clipCoords.w() == 0) return new Vector3f(clipCoords.x(), clipCoords.y(), clipCoords.z());
        float invW = 1.0f / clipCoords.w();
        return new Vector3f(clipCoords.x() * invW, clipCoords.y() * invW, clipCoords.z() * invW);
    }

    private Vector2f viewportTransform(Vector3f ndcCoords, float viewportWidth, float viewportHeight) {
        float screenX = (ndcCoords.x() + 1.0f) * 0.5f * viewportWidth;
        float screenY = (ndcCoords.y() + 1.0f) * 0.5f * viewportHeight;
        return new Vector2f(screenX, screenY);
    }

    private float edgeFunction(Vector2f a, Vector2f b, Vector2f p) {
        return (p.x() - a.x()) * (b.y() - a.y()) - (p.y() - a.y()) * (b.x() - a.x());
    }

    private Map<String, Object> interpolateVaryings(
            V_IO v0_io, V_IO v1_io, V_IO v2_io,
            float b0, float b1, float b2,
            float w0_inv, float w1_inv, float w2_inv,
            float perspectiveCorrection
    ) {
        final Map<String, Object> interpolatedVaryings = new HashMap<>();

        for (final Map.Entry<String, Field> entry : shaderProgram
                .getVertexShaderVaryingOutputFields().entrySet()) {
            final String name = entry.getKey();
            final Field field = entry.getValue();
            try {
                Object val0 = field.get(v0_io);
                Object val1 = field.get(v1_io);
                Object val2 = field.get(v2_io);

                MathOperations<?> mathOperations = MathOperations
                        .forClass(val0.getClass());

                val0 = mathOperations.multiply(val0, b0 * w0_inv);
                val1 = mathOperations.multiply(val1, b1 * w1_inv);
                val2 = mathOperations.multiply(val2, b2 * w2_inv);

                Object interpolatedValue = mathOperations.add(val0, mathOperations.add(val1, val2));
                interpolatedValue = mathOperations.multiply(interpolatedValue, perspectiveCorrection);

                interpolatedVaryings.put(name, interpolatedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error interpolating varying " + name, e);
            }
        }
        return interpolatedVaryings;
    }
}