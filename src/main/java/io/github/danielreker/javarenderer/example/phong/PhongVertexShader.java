package io.github.danielreker.javarenderer.example.phong;

import io.github.danielreker.javarenderer.core.shader.AbstractVertexShader;
import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Matrix3f;
import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

public class PhongVertexShader extends AbstractVertexShader<PhongVertexShader.Io> {

    public static class Io extends VertexShaderIoBase {
        @Attribute public Vector3f aPosition;
        @Attribute public Vector2f aTexCoord;
        @Attribute public Vector3f aNormal;

        @Varying public Vector2f varyingTexCoord;
        @Varying public Vector3f varyingFragPos;
        @Varying public Vector3f varyingNormal;

        @Uniform public Matrix4f model;
        @Uniform public Matrix4f view;
        @Uniform public Matrix4f projection;
    }

    @Override
    public void main(Io io) {
        io.varyingFragPos = Matrix4f.multiply(io.model, io.aPosition.withW(1.0f)).xyz();
        io.varyingNormal = Matrix3f.multiply(io.model.invert().orElseThrow().transpose().mat3(), io.aNormal);
        io.gl_Position = Matrix4f.multiply(io.projection, Matrix4f.multiply(io.view, io.varyingFragPos.withW(1.0f)));
        io.varyingTexCoord = io.aTexCoord;
    }

}
