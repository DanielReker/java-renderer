package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.shader.AbstractFragmentShader;
import io.github.danielreker.javarenderer.core.shader.AbstractVertexShader;
import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import io.github.danielreker.javarenderer.math.Matrix3f;
import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;


class CubeVertex {
    @Attribute public Vector3f position;
    @Attribute public Vector2f texCoord;
    @Attribute public Vector3f normal;

    public CubeVertex(Vector3f position, Vector2f texCoord, Vector3f normal) {
        this.position = position;
        this.texCoord = texCoord;
        this.normal = normal;
    }
}

class CubeVertexShaderIo extends VertexShaderIoBase {
    @Attribute public Vector3f position;
    @Attribute public Vector2f texCoord;
    @Attribute public Vector3f normal;

    @Varying public Vector2f varyingTexCoord;
    @Varying public Vector3f varyingFragPos;
    @Varying public Vector3f varyingNormal;

    @Uniform public Matrix4f model;
    @Uniform public Matrix4f view;
    @Uniform public Matrix4f projection;
}

class CubeVertexShader extends AbstractVertexShader<CubeVertexShaderIo> {
    @Override
    public void main(CubeVertexShaderIo io) {
        io.varyingFragPos = Matrix4f.multiply(io.model, io.position.withW(1.0f)).xyz();

        io.varyingNormal = Matrix3f.multiply(io.model.invert().orElseThrow().transpose().mat3(), io.normal);

        io.gl_Position = Matrix4f.multiply(io.projection, Matrix4f.multiply(io.view, io.varyingFragPos.withW(1.0f)));

        io.varyingTexCoord = io.texCoord;
    }
}

class CubeFragmentShaderIo extends FragmentShaderIoBase {
    @Uniform public Vector3f viewPos;
    @Uniform public Vector3f lightPos;
    @Uniform public Vector3f lightColor;
    @Uniform public Vector3f chessColor;

    @Uniform public float ambientStrength;
    @Uniform public float diffuseStrength;
    @Uniform public float specularStrength;
    @Uniform public int shininess;

    @Varying public Vector2f varyingTexCoord;
    @Varying public Vector3f varyingFragPos;
    @Varying public Vector3f varyingNormal;
}

class CubeFragmentShader extends AbstractFragmentShader<CubeFragmentShaderIo> {
    @Override
    public void main(CubeFragmentShaderIo io) {
        float scale = 10.0f;
        final int checkX = (int) Math.floor(io.varyingTexCoord.x() * scale);
        final int checkY = (int) Math.floor(io.varyingTexCoord.y() * scale);

        final Vector3f objectColor;
        if ((checkX + checkY) % 2 == 0) {
            objectColor = Vector3f.of(1.0f, 1.0f, 1.0f);
        } else {
            objectColor = io.chessColor;
        }



        Vector3f ambient = io.lightColor.multiply(io.ambientStrength);



        Vector3f norm = io.varyingNormal.normalize();

        Vector3f lightDir = io.lightPos.sub(io.varyingFragPos).normalize();

        float diff = Math.max(norm.dot(lightDir), 0.0f);
        Vector3f diffuse = io.lightColor.multiply(io.diffuseStrength * diff);



        Vector3f viewDir = io.viewPos.sub(io.varyingFragPos).normalize();

        Vector3f reflectDir = Vector3f.reflect(lightDir.negate(), norm);

        float spec = (float) Math.pow(Math.max(viewDir.dot(reflectDir), 0.0f), io.shininess);
        Vector3f specular = io.lightColor.multiply(io.specularStrength * spec);



        io.gl_FragColor = ambient.add(diffuse).add(specular)
                .multiply(objectColor)
                .withW(1.0f);
    }
}