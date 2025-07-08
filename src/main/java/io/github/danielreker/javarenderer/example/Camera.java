package io.github.danielreker.javarenderer.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    public Vector3f position;
    public Vector3f front;
    public Vector3f up;
    public Vector3f right;
    public Vector3f worldUp;

    public float yaw;
    public float pitch;

    public float movementSpeed;
    public float mouseSensitivity;
    public float zoom;

    private static final float DEFAULT_YAW = -90.0f;
    private static final float DEFAULT_PITCH = 0.0f;
    private static final float DEFAULT_SPEED = 2.5f;
    private static final float DEFAULT_SENSITIVITY = 0.1f;
    private static final float DEFAULT_ZOOM = 45.0f;

    public enum CameraMovement {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        UP,
        DOWN
    }

    public Camera(Vector3f position, Vector3f up, float yaw, float pitch) {
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.movementSpeed = DEFAULT_SPEED;
        this.mouseSensitivity = DEFAULT_SENSITIVITY;
        this.zoom = DEFAULT_ZOOM;

        this.position = new Vector3f(position);
        this.worldUp = new Vector3f(up);
        this.yaw = yaw;
        this.pitch = pitch;
        updateCameraVectors();
    }

    public Camera(Vector3f position) {
        this(position, new Vector3f(0.0f, 1.0f, 0.0f), DEFAULT_YAW, DEFAULT_PITCH);
    }

    public Camera(float posX, float posY, float posZ, float upX, float upY, float upZ, float yaw, float pitch) {
        this(new Vector3f(posX, posY, posZ), new Vector3f(upX, upY, upZ), yaw, pitch);
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), up);
    }

    public void processKeyboard(CameraMovement direction, float deltaTime) {
        float velocity = movementSpeed * deltaTime;
        Vector3f deltaPos = new Vector3f();
        if (direction == CameraMovement.FORWARD)
            deltaPos.set(front);
        if (direction == CameraMovement.BACKWARD)
            deltaPos.set(front).negate();
        if (direction == CameraMovement.LEFT)
            deltaPos.set(right).negate();
        if (direction == CameraMovement.RIGHT)
            deltaPos.set(right);
        if (direction == CameraMovement.UP)
            deltaPos.set(worldUp);
        if (direction == CameraMovement.DOWN)
            deltaPos.set(worldUp).negate();

        position.add(deltaPos.mul(velocity));
    }

    public void processMouseMovement(float xoffset, float yoffset, boolean constrainPitch) {
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw += xoffset;
        pitch += yoffset;

        if (constrainPitch) {
            if (pitch > 89.0f)
                pitch = 89.0f;
            if (pitch < -89.0f)
                pitch = -89.0f;
        }
        updateCameraVectors();
    }

    public void processMouseScroll(float yoffset) {
        zoom -= yoffset;
        if (zoom < 1.0f)
            zoom = 1.0f;
        if (zoom > 75.0f)
            zoom = 75.0f;
    }

    private void updateCameraVectors() {
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        newFront.y = (float) (Math.sin(Math.toRadians(pitch)));
        newFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front = newFront.normalize();

        right = new Vector3f(front).cross(worldUp).normalize();
        up = new Vector3f(right).cross(front).normalize();
    }
}