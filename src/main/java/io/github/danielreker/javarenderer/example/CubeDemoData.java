package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.shader.AbstractFragmentShader;
import io.github.danielreker.javarenderer.core.shader.AbstractVertexShader;
import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;


class CubeVertex {
    @Attribute public Vector3f position;
    @Attribute public Vector2f texCoord;

    public CubeVertex(Vector3f position, Vector2f texCoord) {
        this.position = position;
        this.texCoord = texCoord;
    }
}

class CubeVertexShaderIo extends VertexShaderIoBase {
    @Uniform public Matrix4f model;
    @Uniform public Matrix4f view;
    @Uniform public Matrix4f projection;

    @Attribute public Vector3f position;
    @Attribute public Vector2f texCoord;

    @Varying public Vector2f varyingTexCoord = new Vector2f();
}

class CubeVertexShader extends AbstractVertexShader<CubeVertexShaderIo> {
    @Override
    public void main(CubeVertexShaderIo io) {
        Vector4f posInClip = new Vector4f(io.position, 1.0f);
        io.model.transform(posInClip);
        io.view.transform(posInClip);
        io.projection.transform(posInClip);
        io.gl_Position.set(posInClip);

        io.varyingTexCoord.set(io.texCoord.x, io.texCoord.y);
    }
}

class CubeFragmentShaderIo extends FragmentShaderIoBase {
    @Varying public Vector2f varyingTexCoord;
}

class CubeFragmentShader extends AbstractFragmentShader<CubeFragmentShaderIo> {
    @Override
    public void main(CubeFragmentShaderIo io) {
        float scale = 10.0f;
        int checkX = (int) Math.floor(io.varyingTexCoord.x * scale);
        int checkY = (int) Math.floor(io.varyingTexCoord.y * scale);

        if ((checkX + checkY) % 2 == 0) {
            io.gl_FragColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            io.gl_FragColor.set(0.2f, 0.2f, 0.2f, 1.0f);
        }
    }
}