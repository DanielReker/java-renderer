package io.github.danielreker.javarenderer.math;

public record Vector4f(
        float v0,
        float v1,
        float v2,
        float v3
) {

    public static final Vector4f ZERO = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);


    public static Vector4f of(float v0, float v1, float v2, float v3) {
        return new Vector4f(v0, v1, v2, v3);
    }

    public static Vector4f of(Vector3f vector3f, float v3) {
        return new Vector4f(
                vector3f.v0(),
                vector3f.v1(),
                vector3f.v2(),
                v3
        );
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

    public float w() {
        return v3;
    }


    public Vector3f xyz() {
        return new Vector3f(v0, v1, v2);
    }


    public Vector4f negate() {
        return new Vector4f(-v0, -v1, -v2, -v3);
    }

    public Vector4f add(Vector4f other) {
        return add(this, other);
    }

    public Vector4f sub(Vector4f other) {
        return add(this, other.negate());
    }

    public Vector4f multiply(Vector4f other) {
        return multiply(this, other);
    }

    public Vector4f multiply(float scalar) {
        return multiply(scalar, this);
    }

    public float dot(Vector4f other) {
        return dot(this, other);
    }

    public float lengthSquared() {
        return v0 * v0 + v1 * v1 + v2 * v2 + v3 * v3;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Vector4f normalize() {
        return multiply(1.0f / length());
    }


    public static Vector4f add(Vector4f lhs, Vector4f rhs) {
        return new Vector4f(
                lhs.v0 + rhs.v0,
                lhs.v1 + rhs.v1,
                lhs.v2 + rhs.v2,
                lhs.v3 + rhs.v3
        );
    }

    public static Vector4f multiply(Vector4f lhs, Vector4f rhs) {
        return new Vector4f(
                lhs.v0 * rhs.v0,
                lhs.v1 * rhs.v1,
                lhs.v2 * rhs.v2,
                lhs.v3 * rhs.v3
        );
    }

    public static Vector4f multiply(float scalar, Vector4f vector4f) {
        return new Vector4f(
                scalar * vector4f.v0,
                scalar * vector4f.v1,
                scalar * vector4f.v2,
                scalar * vector4f.v3
        );
    }

    public static float dot(Vector4f lhs, Vector4f rhs) {
        return lhs.v0 * rhs.v0 + lhs.v1 * rhs.v1 + lhs.v2 * rhs.v2 + lhs.v3 * rhs.v3;
    }

    public static Vector4f reflect(Vector4f incident, Vector4f normal) {
        return incident.sub(normal.multiply(2.0f * normal.dot(incident)));
    }

}
