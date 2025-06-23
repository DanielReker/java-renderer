package io.github.danielreker.javarenderer.core.shader.io;

import io.github.danielreker.javarenderer.core.shader.annotations.BuiltIn;
import org.joml.Vector4f;

public abstract class FragmentShaderIoBase {
    @BuiltIn public Vector4f gl_FragCoord = new Vector4f();

    public Vector4f gl_FragColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    public Float gl_FragDepth = null;
    public boolean discarded = false;


    public void discardFragment() {
        this.discarded = true;
    }
}