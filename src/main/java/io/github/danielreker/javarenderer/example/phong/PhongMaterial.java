package io.github.danielreker.javarenderer.example.phong;

public record PhongMaterial(
        float ambientStrength,
        float diffuseStrength,
        float specularStrength,
        int shininess
) {
}
