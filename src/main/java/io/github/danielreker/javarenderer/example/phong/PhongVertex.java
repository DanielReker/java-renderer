package io.github.danielreker.javarenderer.example.phong;

import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

public class PhongVertex {
    @Attribute public Vector3f aPosition;
    @Attribute public Vector2f aTexCoord;
    @Attribute public Vector3f aNormal;

    public PhongVertex(
            Vector3f position,
            Vector2f texCoord,
            Vector3f normal
    ) {
        this.aPosition = position;
        this.aTexCoord = texCoord;
        this.aNormal = normal;
    }
}