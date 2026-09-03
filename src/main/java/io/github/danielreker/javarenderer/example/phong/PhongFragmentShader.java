package io.github.danielreker.javarenderer.example.phong;

import io.github.danielreker.javarenderer.core.shader.AbstractFragmentShader;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

import java.util.function.Function;

public class PhongFragmentShader extends AbstractFragmentShader<PhongFragmentShader.Io> {

    public static class Io extends FragmentShaderIoBase {
        @Uniform public Vector3f viewPos;
        @Uniform public Function<Vector2f, Vector3f> objectColorTexture;
        @Uniform public PhongMaterial material;
        @Uniform public PhongLightSource lightSource;

        @Varying public Vector3f varyingFragPos;
        @Varying public Vector3f varyingNormal;
        @Varying public Vector2f varyingTexCoord;
    }

    @Override
    public void main(Io io) {
        final Vector3f objectColor = io.objectColorTexture.apply(io.varyingTexCoord);

        final Vector3f lightDir = io.lightSource.position().sub(io.varyingFragPos).normalize();
        final Vector3f norm = io.varyingNormal.normalize();
        final Vector3f viewDir = io.viewPos.sub(io.varyingFragPos).normalize();
        final Vector3f reflectDir = Vector3f.reflect(lightDir.negate(), norm);


        final Vector3f ambient = io.lightSource.color().multiply(io.material.ambientStrength());


        final float diff = Math.max(norm.dot(lightDir), 0.0f);
        final Vector3f diffuse = io.lightSource.color().multiply(io.material.diffuseStrength() * diff);


        float spec = (float) Math.pow(Math.max(viewDir.dot(reflectDir), 0.0f), io.material.shininess());
        Vector3f specular = io.lightSource.color().multiply(io.material.specularStrength() * spec);


        io.gl_FragColor = ambient.add(diffuse).add(specular)
                .multiply(objectColor)
                .withW(1.0f);
    }

}
