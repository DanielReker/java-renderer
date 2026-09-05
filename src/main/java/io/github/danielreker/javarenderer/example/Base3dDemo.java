package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.core.Renderer;
import io.github.danielreker.javarenderer.core.container.FrameBuffer;
import io.github.danielreker.javarenderer.core.container.RenderBuffer;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Base3dDemo {

    private final String title;
    protected final int frameWidth;
    protected final int frameHeight;
    private final Vector3f backgroundColor;
    private final float logicLoopFrequencyHz;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ConcurrentHashMap<Integer, Boolean> keyCodeToIsPressed = new ConcurrentHashMap<>();
    private final ScheduledExecutorService logicScheduler = Executors.newScheduledThreadPool(1);

    private JFrame frame;
    private Canvas canvas;
    private Robot robot;
    private BufferedImage awtFrameBuffer;
    private BufferStrategy bufferStrategy;

    protected final Camera camera = new Camera(
            Vector3f.ZERO,
            0.0f, 0.0f, 70.0f, 2.5f, 3.0f, 0.0017f
    );

    protected final Renderer renderer = new Renderer();


    public Base3dDemo(
            int frameWidth,
            int frameHeight,
            String title,
            Vector3f backgroundColor,
            float logicLoopFrequencyHz
    ) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.title = title;
        this.backgroundColor = backgroundColor;
        this.logicLoopFrequencyHz = logicLoopFrequencyHz;
    }


    public void run() throws AWTException {
        initializeWindow();

        scheduleLogicUpdates();

        Thread renderThread = new Thread(() -> {
            float lastFpsCaptureTime = getCurrentTime();
            int frameCounter = 0;

            while (running.get()) {
                float currentTime = getCurrentTime();
                float timeFromLastFpsCapture = currentTime - lastFpsCaptureTime;
                if (timeFromLastFpsCapture >= 1.0f) {
                    final int framesRendered = frameCounter;
                    EventQueue.invokeLater(() ->
                            frame.setTitle(String.format(title + " | FPS: %d", framesRendered)));
                    frameCounter = 0;
                    lastFpsCaptureTime = currentTime;
                }
                frameCounter++;

                FrameBuffer frameBuffer = FrameBuffer.create(
                        frameWidth, frameHeight,
                        backgroundColor.withW(1.0f), 1.0f
                );

                render(frameBuffer);

                RenderBuffer<Vector4f> colorBuffer = frameBuffer.getColorAttachment();

                displayColorBuffer(colorBuffer);
            }
        });
        renderThread.setName("RenderThread");
        renderThread.start();

        registerListeners(renderThread);
    }

    private void initializeWindow() throws AWTException {
        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);

        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(frameWidth, frameHeight));
        canvas.setIgnoreRepaint(true);
        canvas.setFocusable(true);

        frame.add(canvas);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas.createBufferStrategy(1);
        bufferStrategy = canvas.getBufferStrategy();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.createImage(new byte[0]);
        Cursor blankCursor = toolkit.createCustomCursor(image, new Point(0, 0), "blank_cursor");
        frame.setCursor(blankCursor);

        robot = new Robot();

        canvas.requestFocus();

        awtFrameBuffer = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
    }

    private void displayColorBuffer(RenderBuffer<Vector4f> colorBuffer) {
        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                Vector4f pixelColorVec = colorBuffer.getValue(x, frameHeight - 1 - y);
                if (pixelColorVec != null) {
                    int r = (int) (Math.clamp(pixelColorVec.x(), 0.0f, 1.0f) * 255);
                    int g = (int) (Math.clamp(pixelColorVec.y(), 0.0f, 1.0f) * 255);
                    int b = (int) (Math.clamp(pixelColorVec.z(), 0.0f, 1.0f) * 255);
                    int a = (int) (Math.clamp(pixelColorVec.w(), 0.0f, 1.0f) * 255);
                    awtFrameBuffer.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                } else {
                    awtFrameBuffer.setRGB(x, y, 0xFF000000);
                }
            }
        }

        Graphics2D g2d = null;
        try {
            g2d = (Graphics2D) bufferStrategy.getDrawGraphics();
            g2d.drawImage(awtFrameBuffer, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
        } finally {
            if (g2d != null) {
                g2d.dispose();
            }
        }
        if (!bufferStrategy.contentsLost()) {
            bufferStrategy.show();
        }
    }

    private void registerListeners(Thread renderThread) {
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyCodeToIsPressed.put(e.getKeyCode(), true);

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                }

                onKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyCodeToIsPressed.put(e.getKeyCode(), false);
            }
        });

        canvas.addMouseWheelListener(e ->
                camera.processMouseScroll(-e.getWheelRotation()));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running.set(false);
                try {
                    renderThread.join(1000);

                    logicScheduler.shutdown();
                    if (!logicScheduler.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                        logicScheduler.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void processInput(float deltaTime) {
        boolean speedUp = keyCodeToIsPressed.getOrDefault(KeyEvent.VK_CONTROL, false);

        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_W, false)) {
            camera.processMovement(Camera.CameraMovement.FORWARD, deltaTime, speedUp);
        }
        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_S, false)) {
            camera.processMovement(Camera.CameraMovement.BACKWARD, deltaTime, speedUp);
        }
        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_A, false)) {
            camera.processMovement(Camera.CameraMovement.LEFT, deltaTime, speedUp);
        }
        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_D, false)) {
            camera.processMovement(Camera.CameraMovement.RIGHT, deltaTime, speedUp);
        }
        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_SPACE, false)) {
            camera.processMovement(Camera.CameraMovement.UP, deltaTime, speedUp);
        }
        if (keyCodeToIsPressed.getOrDefault(KeyEvent.VK_SHIFT, false)) {
            camera.processMovement(Camera.CameraMovement.DOWN, deltaTime, speedUp);
        }

        final Point canvasCenterOnScreen = canvas.getLocationOnScreen();
        int centerX = canvasCenterOnScreen.x + canvas.getWidth() / 2;
        int centerY = canvasCenterOnScreen.y + canvas.getHeight() / 2;

        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        float xOffset = mousePos.x - centerX;
        float yOffset = centerY - mousePos.y;

        camera.processMouseMovement(xOffset, yOffset);

        robot.mouseMove(centerX, centerY);
    }

    private void scheduleLogicUpdates() {
        final long intervalMs = Math.round(1000.0f / logicLoopFrequencyHz);
        final float intervalSec = intervalMs / 1000.0f;

        logicScheduler.scheduleAtFixedRate(
                () -> {
                    processInput(intervalSec);
                    processLogic(intervalSec);
                },
                intervalMs, intervalMs, TimeUnit.MILLISECONDS
        );
    }

    protected void processLogic(float deltaTimeSec) { }

    private static float getCurrentTime() {
        return System.nanoTime() / 1_000_000_000f;
    }

    protected abstract void render(FrameBuffer frameBuffer);

    protected void onKeyPressed(int keyCode) { }

}