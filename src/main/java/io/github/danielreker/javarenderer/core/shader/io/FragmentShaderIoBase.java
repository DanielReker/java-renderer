package io.github.danielreker.javarenderer.core.shader.io;

import io.github.danielreker.javarenderer.core.shader.annotations.BuiltIn;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

public abstract class FragmentShaderIoBase {

    @BuiltIn
    public Vector4f glFragCoord;

    public Vector4f glFragColor = Vector3f.ZERO.withW(1.0f);
    public Float glFragDepth = null;
    public Boolean discarded = false;

}