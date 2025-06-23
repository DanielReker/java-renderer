package io.github.danielreker.javarenderer.core.shader;

import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;

public abstract class AbstractVertexShader<IO extends VertexShaderIoBase> {
    public abstract void main(IO io);
}