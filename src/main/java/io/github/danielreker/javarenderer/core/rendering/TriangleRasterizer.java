package io.github.danielreker.javarenderer.core.rendering;

import io.github.danielreker.javarenderer.core.MathOperations;
import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TriangleRasterizer<VS_IO extends VertexShaderIoBase, FS_IO extends FragmentShaderIoBase> {

    private final ShaderProgram<VS_IO, FS_IO> shaderProgram;


    public TriangleRasterizer(ShaderProgram<VS_IO, FS_IO> shaderProgram) {
        this.shaderProgram = shaderProgram;
    }


    public void rasterize(
            VS_IO v0Io, VS_IO v1Io, VS_IO v2Io,
            FrameBuffer targetFrameBuffer
    ) {
        Vector3f v0Ndc = clipToNdc(v0Io.glPosition);
        Vector3f v1Ndc = clipToNdc(v1Io.glPosition);
        Vector3f v2Ndc = clipToNdc(v2Io.glPosition);

        Vector2f viewportSize = Vector2f.of(
                targetFrameBuffer.getWidth(),
                targetFrameBuffer.getHeight()
        );

        Vector2f v0Screen = ndcToScreen(v0Ndc, viewportSize);
        Vector2f v1Screen = ndcToScreen(v1Ndc, viewportSize);
        Vector2f v2Screen = ndcToScreen(v2Ndc, viewportSize);

        Vector3f wClipInv = Vector3f.of(
                1.0f / v0Io.glPosition.w(),
                1.0f / v1Io.glPosition.w(),
                1.0f / v2Io.glPosition.w()
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

                FS_IO fsIo = shaderProgram.createAndPrepareFragmentIO(interpolatedVaryings);

                float zFragNdc = Vector3f.of(
                        v0Ndc.z(), v1Ndc.z(), v2Ndc.z()
                ).dot(pixelBarycentric);

                float depth = (zFragNdc + 1.0f) * 0.5f;

                fsIo.glFragCoord = Vector4f.of(
                        pixelCenter.x(),
                        pixelCenter.y(),
                        depth,
                        wFragInv
                );

                shaderProgram.executeFragmentShader(fsIo);

                if (fsIo.discarded) continue;

                float finalDepth = fsIo.glFragDepth == null ? depth : fsIo.glFragDepth;

                if (finalDepth < targetFrameBuffer.getDepthAttachment().getValue(x, y)) {
                    targetFrameBuffer.getColorAttachment().setValue(x, y, fsIo.glFragColor);
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
            VS_IO v0_io, VS_IO v1_io, VS_IO v2_io,
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
