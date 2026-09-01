package io.github.danielreker.javarenderer.core.shader.io;

import io.github.danielreker.javarenderer.core.shader.annotations.BuiltIn;
import io.github.danielreker.javarenderer.math.Vector4f;

public abstract class FragmentShaderIoBase {
    @BuiltIn public Vector4f gl_FragCoord;

    public Vector4f gl_FragColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    public Float gl_FragDepth = null;
    public boolean discarded = false;

}