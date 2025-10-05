package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.Renderer;
import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.RenderBuffer;
import io.github.danielreker.javarenderer.core.container.VertexBuffer;
import io.github.danielreker.javarenderer.core.enums.PrimitiveType;
import io.github.danielreker.javarenderer.core.shader.ShaderProgram;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class CubeDemo {
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    private static volatile boolean running = true;

    private static final Camera camera = new Camera(new Vector3f(0.0f, 0.0f, 3.0f));
    private static boolean firstMouse = true;

    private static Robot robot;

    private static float deltaTime = 0.0f;
    private static long lastFrameTime = System.nanoTime();

    private static final List<CubeVertex> cubeVertexData = Arrays.asList(
            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f, -0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 0.0f)),

            new CubeVertex(new Vector3f(-0.5f, -0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f,  0.5f), new Vector2f(0.0f, 1.0f)),

            new CubeVertex(new Vector3f(-0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),

            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),

            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f, -0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f(-0.5f, -0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),

            new CubeVertex(new Vector3f(-0.5f,  0.5f, -0.5f), new Vector2f(0.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f, -0.5f), new Vector2f(1.0f, 1.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f( 0.5f,  0.5f,  0.5f), new Vector2f(1.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f,  0.5f), new Vector2f(0.0f, 0.0f)),
            new CubeVertex(new Vector3f(-0.5f,  0.5f, -0.5f), new Vector2f(0.0f, 1.0f))
    );

    private static final Vector3f[] cubePositions = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.0f,  5.0f, -15.0f),
            new Vector3f(-1.5f, -2.2f, -2.5f),
            new Vector3f(-3.8f, -2.0f, -12.3f),
            new Vector3f( 2.4f, -0.4f, -3.5f),
            new Vector3f(-1.7f,  3.0f, -7.5f),
            new Vector3f( 1.3f, -2.0f, -2.5f),
            new Vector3f( 1.5f,  2.0f, -2.5f),
            new Vector3f( 1.5f,  0.2f, -1.5f),
            new Vector3f(-1.3f,  1.0f, -1.5f)
    };

    private static final boolean[] keyStates = new boolean[256];


    public static void main(String[] args) throws AWTException {
        JFrame frame = new JFrame("Java Renderer - Cube Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);

        Canvas canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        canvas.setIgnoreRepaint(true);
        canvas.setFocusable(true);


        frame.add(canvas);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas.createBufferStrategy(2);
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(new byte[0]);
        Cursor blankCursor = toolkit.createCustomCursor(image, new Point(0, 0), "blank_cursor");
        frame.setCursor(blankCursor);

        robot = new Robot();

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() < keyStates.length) keyStates[e.getKeyCode()] = true;
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) running = false;
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() < keyStates.length) keyStates[e.getKeyCode()] = false;
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                if (firstMouse) {
                    firstMouse = false;
                    return;
                }

                final Point canvasCenterOnScreen = canvas.getLocationOnScreen();
                int centerX = canvasCenterOnScreen.x + canvas.getWidth() / 2;
                int centerY = canvasCenterOnScreen.y + canvas.getHeight() / 2;

                float xOffset = event.getXOnScreen() - centerX;
                float yOffset = centerY - event.getYOnScreen();

                if (Math.abs(xOffset) > canvas.getWidth() / 2.0f || Math.abs(yOffset) > canvas.getHeight() / 2.0f) {
                    robot.mouseMove(centerX, centerY);
                    return;
                }

                camera.processMouseMovement(xOffset, yOffset, true);

                robot.mouseMove(centerX, centerY);
            }
        });

        canvas.addMouseWheelListener(e -> camera.processMouseScroll(-e.getWheelRotation()));

        canvas.requestFocus();


        Renderer renderer = new Renderer();
        BufferedImage displayImage = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        VertexBuffer<CubeVertex> cubeVbo = VertexBuffer.create(cubeVertexData);
        ShaderProgram<CubeVertexShaderIo, CubeFragmentShaderIo> cubeProgram =
                ShaderProgram.create(new CubeVertexShader(), new CubeFragmentShader());

        Thread renderThread = new Thread(() -> {
            long frameCounter = 0;
            long fpsTimer = System.nanoTime();
            lastFrameTime = System.nanoTime();

            while (running) {
                long currentTime = System.nanoTime();
                deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
                lastFrameTime = currentTime;

                processInput();

                FrameBuffer frameBuffer = FrameBuffer.create(FRAME_WIDTH, FRAME_HEIGHT,
                        new Vector4f(0.1f, 0.1f, 0.1f, 1.0f), 1.0f);

                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(camera.zoom),
                        (float) FRAME_WIDTH / FRAME_HEIGHT,
                        0.1f,
                        100.0f);
                Matrix4f view = camera.getViewMatrix();

                cubeProgram.setUniform("projection", projection);
                cubeProgram.setUniform("view", view);

                for (Vector3f position : cubePositions) {
                    Matrix4f model = new Matrix4f().translate(position);
                    float angle = (System.nanoTime() / 1_000_000_000.0f) * 0.5f;
                    if (position.lengthSquared() > 0.1f) {
                        angle += position.x + position.y;
                    }
                    model.rotate(angle, 0.5f, 1.0f, 0.0f);
                    cubeProgram.setUniform("model", model);

                    renderer.render(frameBuffer, cubeProgram, cubeVbo, PrimitiveType.TRIANGLES, 0, cubeVbo.getVertexCount());
                }


                RenderBuffer<Vector4f> colorBuffer = frameBuffer.getColorAttachment();
                for (int y = 0; y < FRAME_HEIGHT; y++) {
                    for (int x = 0; x < FRAME_WIDTH; x++) {
                        Vector4f pixelColorVec = colorBuffer.getValue(x, FRAME_HEIGHT - 1 - y);
                        if (pixelColorVec != null) {
                            int r = (int) (Math.min(Math.max(pixelColorVec.x, 0.0f), 1.0f) * 255);
                            int g = (int) (Math.min(Math.max(pixelColorVec.y, 0.0f), 1.0f) * 255);
                            int b = (int) (Math.min(Math.max(pixelColorVec.z, 0.0f), 1.0f) * 255);
                            int a = (int) (Math.min(Math.max(pixelColorVec.w, 0.0f), 1.0f) * 255);
                            displayImage.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        } else {
                            displayImage.setRGB(x, y, 0xFF000000);
                        }
                    }
                }

                Graphics2D g2d = null;
                try {
                    g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
                    g2d.drawImage(displayImage, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
                } finally {
                    if (g2d != null) {
                        g2d.dispose();
                    }
                }
                if (!bufferStrategy.contentsLost()) {
                    bufferStrategy.show();
                }

                frameCounter++;
                if (System.nanoTime() - fpsTimer >= 1_000_000_000) {
                    frame.setTitle(String.format("Java Renderer - Cube Demo | FPS: %d", frameCounter));
                    frameCounter = 0;
                    fpsTimer = System.nanoTime();
                }
            }
        });
        renderThread.setName("RenderThread");
        renderThread.start();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
                try {
                    renderThread.join(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private static void processInput() {
        if (keyStates[KeyEvent.VK_W]) camera.processKeyboard(Camera.CameraMovement.FORWARD, deltaTime);
        if (keyStates[KeyEvent.VK_S]) camera.processKeyboard(Camera.CameraMovement.BACKWARD, deltaTime);
        if (keyStates[KeyEvent.VK_A]) camera.processKeyboard(Camera.CameraMovement.LEFT, deltaTime);
        if (keyStates[KeyEvent.VK_D]) camera.processKeyboard(Camera.CameraMovement.RIGHT, deltaTime);
        if (keyStates[KeyEvent.VK_SPACE]) camera.processKeyboard(Camera.CameraMovement.UP, deltaTime);
        if (keyStates[KeyEvent.VK_SHIFT]) camera.processKeyboard(Camera.CameraMovement.DOWN, deltaTime);
    }
}