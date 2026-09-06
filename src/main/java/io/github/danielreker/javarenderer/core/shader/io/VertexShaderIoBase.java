package io.github.danielreker.javarenderer.core.shader.io;

import io.github.danielreker.javarenderer.core.shader.annotations.BuiltIn;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.math.Vector4f;

public abstract class VertexShaderIoBase {
    @BuiltIn @Varying
    public Vector4f gl_Position;
}