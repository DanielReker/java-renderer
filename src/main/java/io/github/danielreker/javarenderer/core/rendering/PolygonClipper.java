package io.github.danielreker.javarenderer.core.rendering;

import io.github.danielreker.javarenderer.core.MathOperations;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Vector4f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolygonClipper<V_IO extends VertexShaderIoBase> {

    private static final List<Vector4f> CLIPPING_PLANES = List.of(
            Vector4f.of(1, 0, 0, 1),
            Vector4f.of(-1, 0, 0, 1),
            Vector4f.of(0, 1, 0, 1),
            Vector4f.of(0, -1, 0, 1),
            Vector4f.of(0, 0, 1, 1),
            Vector4f.of(0, 0, -1, 1)
    );

    private final ShaderProgram<V_IO, ?> shaderProgram;


    public PolygonClipper(ShaderProgram<V_IO, ?> shaderProgram) {
        this.shaderProgram = shaderProgram;
    }


    public List<V_IO> clip(
            List<V_IO> polygonVertices
    ) {
        for (final Vector4f clippingPlane : CLIPPING_PLANES) {
            polygonVertices = clipWithPlane(polygonVertices, clippingPlane);
        }
        return polygonVertices;
    }

    private List<V_IO> clipWithPlane(
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
}
