package io.github.danielreker.javarenderer.core.shader;

import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;

public abstract class AbstractFragmentShader<IO extends FragmentShaderIoBase> {
    public abstract void main(IO io);
}