package io.github.danielreker.javarenderer.math;

public record Vector3f(
        float v0,
        float v1,
        float v2
) {

    public static final Vector3f ZERO = new Vector3f(0.0f, 0.0f, 0.0f);


    public static Vector3f of(float v0, float v1, float v2) {
        return new Vector3f(v0, v1, v2);
    }

    public Vector4f withW(float w) {
        return Vector4f.of(this, w);
    }


    public float x() {
        return v0;
    }

    public float y() {
        return v1;
    }

    public float z() {
        return v2;
    }


    public Vector3f negate() {
        return new Vector3f(-v0, -v1, -v2);
    }

    public Vector3f add(Vector3f other) {
        return add(this, other);
    }

    public Vector3f sub(Vector3f other) {
        return add(this, other.negate());
    }

    public Vector3f multiply(Vector3f other) {
        return multiply(this, other);
    }

    public Vector3f multiply(float scalar) {
        return multiply(scalar, this);
    }

    public float dot(Vector3f other) {
        return dot(this, other);
    }

    public Vector3f cross(Vector3f other) {
        return cross(this, other);
    }

    public float lengthSquared() {
        return v0 * v0 + v1 * v1 + v2 * v2;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Vector3f normalize() {
        return multiply(1.0f / length());
    }


    public static Vector3f add(Vector3f lhs, Vector3f rhs) {
        return new Vector3f(
                lhs.v0 + rhs.v0,
                lhs.v1 + rhs.v1,
                lhs.v2 + rhs.v2
        );
    }

    public static Vector3f multiply(Vector3f lhs, Vector3f rhs) {
        return new Vector3f(
                lhs.v0 * rhs.v0,
                lhs.v1 * rhs.v1,
                lhs.v2 * rhs.v2
        );
    }

    public static Vector3f multiply(float scalar, Vector3f vector3f) {
        return new Vector3f(
                scalar * vector3f.v0,
                scalar * vector3f.v1,
                scalar * vector3f.v2
        );
    }

    public static float dot(Vector3f lhs, Vector3f rhs) {
        return lhs.v0 * rhs.v0 + lhs.v1 * rhs.v1 + lhs.v2 * rhs.v2;
    }

    public static Vector3f cross(Vector3f lhs, Vector3f rhs) {
        return new Vector3f(
                lhs.v1 * rhs.v2 - lhs.v2 * rhs.v1,
                lhs.v2 * rhs.v0 - lhs.v0 * rhs.v2,
                lhs.v0 * rhs.v1 - lhs.v1 * rhs.v0
        );
    }

    public static Vector3f reflect(Vector3f incident, Vector3f normal) {
        return incident.sub(normal.multiply(2.0f * normal.dot(incident)));
    }

}
