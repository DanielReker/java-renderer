package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.Renderer;
import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.RenderBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.shader.*;
import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferStrategy;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


class DemoVertex {
    Vector3f position;
    Vector3f color;

    public DemoVertex(Vector3f position, Vector3f color) {
        this.position = position;
        this.color = color;
    }
}

class DemoVertexShaderIo extends VertexShaderIoBase {
    @Uniform public Matrix4f mvpMatrix;
    @Attribute public Vector3f position;
    @Attribute public Vector3f color;

    @Varying public Vector3f colorVarying = new Vector3f();
}

class DemoVertexShader extends AbstractVertexShader<DemoVertexShaderIo> {
    @Override
    public void main(DemoVertexShaderIo io) {
        io.gl_Position.set(io.position, 1.0f);
        io.mvpMatrix.transform(io.gl_Position);
        io.colorVarying.set(io.color);
    }
}

class DemoFragmentShaderIo extends FragmentShaderIoBase {
    @Uniform public float intensityUniform;
    @Varying public Vector3f colorVarying;
}

class DemoFragmentShader extends AbstractFragmentShader<DemoFragmentShaderIo> {
    @Override
    public void main(DemoFragmentShaderIo io) {
        io.gl_FragColor.set(io.colorVarying, 1.0f);
        io.gl_FragColor.mul(io.intensityUniform);
    }
}


public class TriangleDemo {
    private static final int FRAME_WIDTH = 512;
    private static final int FRAME_HEIGHT = 512;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Renderer Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);

        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        canvas.setIgnoreRepaint(true);

        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas.createBufferStrategy(2);
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();

        Renderer renderer = new Renderer();
        BufferedImage displayImage = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Thread renderThread = new Thread(() -> {
            long startTimeSec = System.nanoTime();
            long frameCount = 0;
            long lastFpsTime = System.nanoTime();

            while (running) {
                long nowSec = System.nanoTime();
                float timeElapsed = (float)(nowSec - startTimeSec);


                VertexBuffer<DemoVertex> vbo = VertexBuffer.create(List.of(
                        new DemoVertex(new Vector3f(0.5f, -0.5f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f)),
                        new DemoVertex(new Vector3f(-0.5f, -0.5f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
                        new DemoVertex(new Vector3f( 0.0f,  0.5f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f))
                ));

                ShaderProgram<DemoVertexShaderIo, DemoFragmentShaderIo> prog = ShaderProgram.create(new DemoVertexShader(), new DemoFragmentShader());
                Matrix4f modelViewProjection = new Matrix4f()
                        .rotate(timeElapsed * 1e-9f, 0.0f, 0.0f, 1.0f)
                        .ortho(-1, 1, -1, 1, -1, 1);
                prog.setUniform("mvpMatrix", modelViewProjection);
                prog.setUniform("intensityUniform", (float)(Math.sin(timeElapsed * 1e-9f) + 1.0f) / 2.0f);


                FrameBuffer myCanvasFrameBuffer = FrameBuffer.create(FRAME_WIDTH, FRAME_HEIGHT, new Vector4f(0.1f, 0.1f, 0.1f, 1f), 1.0f);
                renderer.render(myCanvasFrameBuffer, prog, vbo, PrimitiveType.TRIANGLES, 0, 3);

                RenderBuffer<Vector4f> colorBuffer = myCanvasFrameBuffer.getColorAttachment();
                for (int y = 0; y < FRAME_HEIGHT; y++) {
                    for (int x = 0; x < FRAME_WIDTH; x++) {
                        Vector4f pixelColorVec = colorBuffer.getValue(x, FRAME_HEIGHT - 1 - y);

                        if (pixelColorVec != null) {
                            int red = (int) (Math.min(Math.max(pixelColorVec.x, 0.0f), 1.0f) * 255.0f);
                            int green = (int) (Math.min(Math.max(pixelColorVec.y, 0.0f), 1.0f) * 255.0f);
                            int blue = (int) (Math.min(Math.max(pixelColorVec.z, 0.0f), 1.0f) * 255.0f);
                            int alpha = (int) (Math.min(Math.max(pixelColorVec.w, 0.0f), 1.0f) * 255.0f);
                            displayImage.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                        } else {
                            displayImage.setRGB(x, y, 0xFF000000);
                        }
                    }
                }

                Graphics2D g = null;
                try {
                    g = (Graphics2D) bufferStrategy.getDrawGraphics();
                    g.drawImage(displayImage, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
                } finally {
                    if (g != null) {
                        g.dispose();
                    }
                }
                if (!bufferStrategy.contentsLost()) {
                    bufferStrategy.show();
                }

                frameCount++;
                if (System.nanoTime() - lastFpsTime >= 1_000_000_000) {
                    System.out.printf("FPS: %d\n", frameCount);
                    frameCount = 0;
                    lastFpsTime = System.nanoTime();
                }
            }
        });
        renderThread.setName("RenderThread");
        renderThread.start();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                running = false;
                try {
                    renderThread.join(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}