package io.github.danielreker.javarenderer.core;

import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

import java.lang.reflect.Field;
import java.util.*;
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
            shaderProgram.executeVertexShader(vsIo);
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

        return intersection;
    }


    private void rasterizeTriangle(
            V_IO v0Io, V_IO v1Io, V_IO v2Io,
            FrameBuffer targetFrameBuffer
    ) {
        Vector3f v0Ndc = clipToNdc(v0Io.gl_Position);
        Vector3f v1Ndc = clipToNdc(v1Io.gl_Position);
        Vector3f v2Ndc = clipToNdc(v2Io.gl_Position);

        Vector2f viewportSize = Vector2f.of(
                targetFrameBuffer.getWidth(),
                targetFrameBuffer.getHeight()
        );

        Vector2f v0Screen = ndcToScreen(v0Ndc, viewportSize);
        Vector2f v1Screen = ndcToScreen(v1Ndc, viewportSize);
        Vector2f v2Screen = ndcToScreen(v2Ndc, viewportSize);

        Vector3f wClipInv = Vector3f.of(
                1.0f / v0Io.gl_Position.w(),
                1.0f / v1Io.gl_Position.w(),
                1.0f / v2Io.gl_Position.w()
        );

        int minX = (int) Math.floor(Math.min(v0Screen.x(), Math.min(v1Screen.x(), v2Screen.x())));
        int maxX = (int) Math.ceil(Math.max(v0Screen.x(), Math.max(v1Screen.x(), v2Screen.x())));
        int minY = (int) Math.floor(Math.min(v0Screen.y(), Math.min(v1Screen.y(), v2Screen.y())));
        int maxY = (int) Math.ceil(Math.max(v0Screen.y(), Math.max(v1Screen.y(), v2Screen.y())));

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min((int) viewportSize.x() - 1, maxX);
        maxY = Math.min((int) viewportSize.y() - 1, maxY);

        float areaTriangle = edgeFunction(v0Screen, v1Screen, v2Screen);
        if (areaTriangle == 0) return;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                Vector2f pixelCenter = Vector2f.of(x + 0.5f, y + 0.5f);

                Vector3f pixelBarycentric = Vector3f.of(
                        edgeFunction(v1Screen, v2Screen, pixelCenter),
                        edgeFunction(v2Screen, v0Screen, pixelCenter),
                        edgeFunction(v0Screen, v1Screen, pixelCenter)
                ).multiply(1.0f / areaTriangle);

                if (pixelBarycentric.v0() < 0 || pixelBarycentric.v1() < 0 || pixelBarycentric.v2() < 0)
                    continue;

                float wFragInv = pixelBarycentric.dot(wClipInv);

                Vector3f perspectiveCorrectBarycentric = pixelBarycentric
                        .multiply(wClipInv)
                        .multiply(1.0f / wFragInv);

                Map<String, Object> interpolatedVaryings = interpolateVaryings(
                        v0Io, v1Io, v2Io, perspectiveCorrectBarycentric
                );

                F_IO fsIo = shaderProgram.createAndPrepareFragmentIO(interpolatedVaryings);

                float zFragNdc = Vector3f.of(
                        v0Ndc.z(), v1Ndc.z(), v2Ndc.z()
                ).dot(pixelBarycentric);

                float depth = (zFragNdc + 1.0f) * 0.5f;

                fsIo.gl_FragCoord = Vector4f.of(
                        pixelCenter.x(),
                        pixelCenter.y(),
                        depth,
                        wFragInv
                );

                shaderProgram.executeFragmentShader(fsIo);

                if (fsIo.discarded) continue;

                float finalDepth = Optional
                        .ofNullable(fsIo.gl_FragDepth)
                        .orElse(depth);

                if (finalDepth < targetFrameBuffer.getDepthAttachment().getValue(x, y)) {
                    targetFrameBuffer.getColorAttachment().setValue(x, y, fsIo.gl_FragColor);
                    targetFrameBuffer.getDepthAttachment().setValue(x, y, finalDepth);
                }
            }
        }
    }

    private Vector3f clipToNdc(Vector4f clipCoords) {
        return clipCoords
                .xyz()
                .multiply(1.0f / clipCoords.w());
    }

    private Vector2f ndcToScreen(Vector3f ndcCoords, Vector2f viewportSize) {
        return ndcCoords
                .xy()
                .add(Vector2f.of(1.0f, 1.0f))
                .multiply(0.5f)
                .multiply(viewportSize);
    }

    private float edgeFunction(Vector2f a, Vector2f b, Vector2f p) {
        return (p.x() - a.x()) * (b.y() - a.y())
                - (p.y() - a.y()) * (b.x() - a.x());
    }

    private Map<String, Object> interpolateVaryings(
            V_IO v0_io, V_IO v1_io, V_IO v2_io,
            Vector3f perspectiveCorrectBarycentric
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

                val0 = mathOperations.multiply(val0, perspectiveCorrectBarycentric.v0());
                val1 = mathOperations.multiply(val1, perspectiveCorrectBarycentric.v1());
                val2 = mathOperations.multiply(val2, perspectiveCorrectBarycentric.v2());

                Object interpolatedValue = mathOperations.add(val0, mathOperations.add(val1, val2));

                interpolatedVaryings.put(name, interpolatedValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error interpolating varying " + name, e);
            }
        }
        return interpolatedVaryings;
    }
}