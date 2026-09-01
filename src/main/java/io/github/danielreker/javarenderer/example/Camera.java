package io.github.danielreker.javarenderer.example;

import io.github.danielreker.javarenderer.math.Matrix4f;
import io.github.danielreker.javarenderer.math.Vector3f;

public class Camera {

    public static final float MIN_VERTICAL_FOV_RAD = (float) Math.toRadians(10.0f);
    public static final float MAX_VERTICAL_FOV_RAD = (float) Math.toRadians(150.0f);
    public static final float MAX_PITCH_RAD = (float) Math.toRadians(90.0f);
    public static final float MIN_PITCH_RAD = -MAX_PITCH_RAD;
    public static final float VERTICAL_FOV_PER_MOUSE_SCROLL = (float) Math.toRadians(2.0f);

    public Vector3f position;
    public float yawRad;
    public float pitchRad;
    public float verticalFovRad;
    public float movementSpeed;
    public float mouseSensitivityRadPerPoint;

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
            float mouseSensitivityDegPerPoint
    ) {
        this.position = position;
        this.yawRad = (float) Math.toRadians(yawDeg);
        this.pitchRad = (float) Math.toRadians(pitchDeg);
        this.verticalFovRad = (float) Math.toRadians(verticalFovDeg);
        this.movementSpeed = movementSpeed;
        this.mouseSensitivityRadPerPoint = (float) Math.toRadians(mouseSensitivityDegPerPoint);
    }

    public Matrix4f getViewMatrix() {
        final Matrix4f translation = Matrix4f.translation(position.negate());
        final Matrix4f rotation = Matrix4f.multiply(
                Matrix4f.rotationAroundX(-pitchRad),
                Matrix4f.rotationAroundY(-yawRad)
        );
        return Matrix4f.multiply(rotation, translation);
    }

    public void processMovement(CameraMovement direction, float deltaTime) {
        float distance = movementSpeed * deltaTime;

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

        Vector3f movementDirection = Vector3f.ZERO;
        if (direction == CameraMovement.FORWARD)
            movementDirection = movementDirection.add(cameraForward);
        else if (direction == CameraMovement.BACKWARD)
            movementDirection = movementDirection.sub(cameraForward);
        else if (direction == CameraMovement.LEFT)
            movementDirection = movementDirection.sub(cameraRight);
        else if (direction == CameraMovement.RIGHT)
            movementDirection = movementDirection.add(cameraRight);
        else if (direction == CameraMovement.UP)
            movementDirection = movementDirection.add(worldUp);
        else if (direction == CameraMovement.DOWN)
            movementDirection = movementDirection.sub(worldUp);

        movementDirection = movementDirection.normalize();
        position = position.add(movementDirection.multiply(distance));
    }

    public void processMouseMovement(float xOffsetPoints, float yOffsetPoints) {
        yawRad += xOffsetPoints * mouseSensitivityRadPerPoint;
        pitchRad += yOffsetPoints * mouseSensitivityRadPerPoint;

        pitchRad = Math.clamp(pitchRad, MIN_PITCH_RAD, MAX_PITCH_RAD);
    }

    public void processMouseScroll(int wheelOffset) {
        verticalFovRad -= wheelOffset * VERTICAL_FOV_PER_MOUSE_SCROLL;
        verticalFovRad = Math.clamp(verticalFovRad, MIN_VERTICAL_FOV_RAD, MAX_VERTICAL_FOV_RAD);
    }

}