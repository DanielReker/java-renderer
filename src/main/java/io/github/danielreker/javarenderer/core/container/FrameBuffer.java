package io.github.danielreker.javarenderer.core.container;

import org.joml.Vector4f;

public final class FrameBuffer {
    private final int width;
    private final int height;

    private final RenderBuffer<Vector4f> colorAttachment;
    private final RenderBuffer<Float> depthAttachment;

    public static FrameBuffer create(int width, int height, Vector4f clearColor, float clearDepth) {
        RenderBuffer<Vector4f> color = new RenderBuffer<>(width, height, Vector4f.class, clearColor);
        RenderBuffer<Float> depth = new RenderBuffer<>(width, height, Float.class, clearDepth);
        return new FrameBuffer(width, height, color, depth);
    }

    private FrameBuffer(
            int width,
            int height,
            RenderBuffer<Vector4f> colorAttachment,
            RenderBuffer<Float> depthAttachment
    ) {
        this.width = width;
        this.height = height;
        this.colorAttachment = colorAttachment;
        this.depthAttachment = depthAttachment;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public RenderBuffer<Vector4f> getColorAttachment() { return colorAttachment; }
    public RenderBuffer<Float> getDepthAttachment() { return depthAttachment; }

    public void clear(Vector4f clearColor, float clearDepth) {
        if (colorAttachment != null && clearColor != null) {
            colorAttachment.clear(clearColor);
        }
        if (depthAttachment != null) {
            depthAttachment.clear(clearDepth);
        }
    }
}