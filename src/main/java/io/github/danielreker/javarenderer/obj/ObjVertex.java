package io.github.danielreker.javarenderer.obj;

import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

public record ObjVertex(
        Vector3f position,
        Vector2f textureCoordinates,
        Vector3f normal
) {
}
