package io.github.danielreker.javarenderer.example.phong;

import io.github.danielreker.javarenderer.math.Vector3f;

public record PhongLightSource(
        Vector3f position,
        Vector3f color
) {
}
