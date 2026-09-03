package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector3f;

public class Camera {

    private static final float MIN_VERTICAL_FOV_RAD = (float) Math.toRadians(10.0f);
    private static final float MAX_VERTICAL_FOV_RAD = (float) Math.toRadians(150.0f);
    private static final float MAX_PITCH_RAD = (float) Math.toRadians(90.0f);
    private static final float MIN_PITCH_RAD = -MAX_PITCH_RAD;
    private static final float VERTICAL_FOV_PER_MOUSE_SCROLL = (float) Math.toRadians(2.0f);

    private Vector3f position;
    private float yawRad;
    private float pitchRad;
    private float verticalFovRad;
    private final float movementSpeed;
    private final float speedUpMultiplier;
    private final float mouseSensitivity;

    public enum CameraMovement {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public Camera(
            Vector3f position,
            float yawDeg,
            float pitchDeg,
            float verticalFovDeg,
            float movementSpeed,
            float speedUpMultiplier,
            float mouseSensitivity
    ) {
        this.position = position;
        this.yawRad = (float) Math.toRadians(yawDeg);
        this.pitchRad = (float) Math.toRadians(pitchDeg);
        this.verticalFovRad = (float) Math.toRadians(verticalFovDeg);
        this.movementSpeed = movementSpeed;
        this.speedUpMultiplier = speedUpMultiplier;
        this.mouseSensitivity = mouseSensitivity;
    }

    public Matrix4f getViewMatrix() {
        final Matrix4f translation = Matrix4f.translation(position.negate());
        final Matrix4f rotation = Matrix4f.multiply(
                Matrix4f.rotationAroundX(-pitchRad),
                Matrix4f.rotationAroundY(-yawRad)
        );
        return Matrix4f.multiply(rotation, translation);
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getVerticalFovRad() {
        return verticalFovRad;
    }


    public void processMovement(
            CameraMovement direction,
            float deltaTime,
            boolean speedUp
    ) {
        float distance = movementSpeed * deltaTime;
        if (speedUp) {
            distance *= speedUpMultiplier;
        }

        float yawCos = (float) Math.cos(yawRad);
        float yawSin = (float) Math.sin(yawRad);
        float pitchCos = (float) Math.cos(pitchRad);
        float pitchSin = (float) Math.sin(pitchRad);

        Vector3f cameraForward = Vector3f
                .of(yawSin * pitchCos, pitchSin, -yawCos * pitchCos)
                .normalize();

        Vector3f worldUp = Vector3f.of(0.0f, 1.0f, 0.0f);

        Vector3f cameraRight = Vector3f
                .of(yawCos, 0.0f, yawSin)
                .normalize();

        Vector3f movementDirection = switch (direction) {
            case FORWARD -> cameraForward;
            case BACKWARD -> cameraForward.negate();
            case LEFT -> cameraRight.negate();
            case RIGHT -> cameraRight;
            case UP -> worldUp;
            case DOWN -> worldUp.negate();
        };

        movementDirection = movementDirection.normalize();
        position = position.add(movementDirection.multiply(distance));
    }

    public void processMouseMovement(float xOffsetPoints, float yOffsetPoints) {
        yawRad += xOffsetPoints * mouseSensitivity;
        pitchRad += yOffsetPoints * mouseSensitivity;

        pitchRad = Math.clamp(pitchRad, MIN_PITCH_RAD, MAX_PITCH_RAD);
    }

    public void processMouseScroll(int wheelOffset) {
        verticalFovRad -= wheelOffset * VERTICAL_FOV_PER_MOUSE_SCROLL;
        verticalFovRad = Math.clamp(verticalFovRad, MIN_VERTICAL_FOV_RAD, MAX_VERTICAL_FOV_RAD);
    }

}