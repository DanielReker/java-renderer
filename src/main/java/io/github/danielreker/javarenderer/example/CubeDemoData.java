package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.shader.AbstractFragmentShader;
import io.github.danielreker.javarenderer.core.shader.AbstractVertexShader;
import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import org.joml.*;

import java.lang.Math;


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

    @Varying public Vector2f varyingTexCoord = new Vector2f();
    @Varying public Vector3f varyingFragPos = new Vector3f();
    @Varying public Vector3f varyingNormal = new Vector3f();

    @Uniform public Matrix4f model;
    @Uniform public Matrix4f view;
    @Uniform public Matrix4f projection;
}

class CubeVertexShader extends AbstractVertexShader<CubeVertexShaderIo> {
    @Override
    public void main(CubeVertexShaderIo io) {
        // 1. FragPos = vec3(model * vec4(aPos, 1.0));
        Vector4f pos4 = new Vector4f(io.position, 1.0f);
        Vector4f worldPos4 = new Vector4f();
        io.model.transform(pos4, worldPos4);
        io.varyingFragPos.set(worldPos4.x, worldPos4.y, worldPos4.z);

        // 2. Normal = mat3(transpose(inverse(model))) * aNormal;
        Matrix4f normalMatrix4 = new Matrix4f();
        io.model.invert(normalMatrix4).transpose();
        Matrix3f normalMatrix3 = new Matrix3f(normalMatrix4); // Extract top-left 3x3
        normalMatrix3.transform(io.normal, io.varyingNormal);

        // 3. gl_Position = projection * view * vec4(FragPos, 1.0);
        Vector4f fragPos4 = new Vector4f(io.varyingFragPos, 1.0f);
        Vector4f intermediate = new Vector4f();

        // Multiplies projection * view * fragPos4
        io.view.transform(fragPos4, intermediate);
        io.projection.transform(intermediate, io.gl_Position);

        io.varyingTexCoord.set(io.texCoord);
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
        final int checkX = (int) Math.floor(io.varyingTexCoord.x * scale);
        final int checkY = (int) Math.floor(io.varyingTexCoord.y * scale);

        final Vector3f objectColor = new Vector3f();
        if ((checkX + checkY) % 2 == 0) {
            objectColor.set(1.0f, 1.0f, 1.0f);
        } else {
            objectColor.set(io.chessColor);
        }



        // 1. Ambient (Фоновое освещение)
        Vector3f ambient = new Vector3f(io.lightColor).mul(io.ambientStrength);



        // 2. Diffuse (Диффузное рассеивание)
        Vector3f norm = new Vector3f(io.varyingNormal).normalize();

        // Направление от фрагмента к свету: normalize(lightPos - FragPos)
        Vector3f lightDir = new Vector3f();
        io.lightPos.sub(io.varyingFragPos, lightDir).normalize();

        // float diff = max(dot(norm, lightDir), 0.0);
        float diff = Math.max(norm.dot(lightDir), 0.0f);
        Vector3f diffuse = new Vector3f(io.lightColor).mul(io.diffuseStrength * diff);



        // 3. Specular (Зеркальный блик)
        // Направление от фрагмента к камере: normalize(viewPos - FragPos)
        Vector3f viewDir = new Vector3f();
        io.viewPos.sub(io.varyingFragPos, viewDir).normalize();

        // В GLSL reflect(-lightDir, norm) принимает вектор ОТ источника света
        Vector3f negLightDir = new Vector3f(lightDir).negate();
        Vector3f reflectDir = new Vector3f();
        negLightDir.reflect(norm, reflectDir); // Отражаем вектор относительно нормали

        // float spec = pow(max(dot(viewDir, reflectDir), 0.0), shininess);
        float specFactor = Math.max(viewDir.dot(reflectDir), 0.0f);
        float spec = (float) Math.pow(specFactor, io.shininess);
        Vector3f specular = new Vector3f(io.lightColor).mul(io.specularStrength * spec);



        // Итоговый расчет: result = (ambient + diffuse + specular) * objectColor
        Vector3f result = new Vector3f();
        ambient.add(diffuse).add(specular).mul(objectColor, result);

        io.gl_FragColor = new Vector4f(result, 1.0f);
    }
}