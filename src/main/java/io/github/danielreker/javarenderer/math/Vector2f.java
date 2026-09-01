package io.github.danielreker.javarenderer.math;

public record Vector2f(
        float v0,
        float v1
) {

    public static final Vector2f ZERO = new Vector2f(0.0f, 0.0f);


    public static Vector2f of(float v0, float v1) {
        return new Vector2f(v0, v1);
    }


    public float x() {
        return v0;
    }

    public float y() {
        return v1;
    }


    public Vector2f negate() {
        return new Vector2f(-v0, -v1);
    }

    public Vector2f add(Vector2f other) {
        return add(this, other);
    }

    public Vector2f sub(Vector2f other) {
        return add(this, other.negate());
    }

    public Vector2f multiply(Vector2f other) {
        return multiply(this, other);
    }

    public Vector2f multiply(float scalar) {
        return multiply(scalar, this);
    }

    public float dot(Vector2f other) {
        return dot(this, other);
    }

    public float lengthSquared() {
        return v0 * v0 + v1 * v1;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Vector2f normalize() {
        return multiply(1.0f / length());
    }


    public static Vector2f add(Vector2f lhs, Vector2f rhs) {
        return new Vector2f(
                lhs.v0 + rhs.v0,
                lhs.v1 + rhs.v1
        );
    }

    public static Vector2f multiply(Vector2f lhs, Vector2f rhs) {
        return new Vector2f(
                lhs.v0 * rhs.v0,
                lhs.v1 * rhs.v1
        );
    }

    public static Vector2f multiply(float scalar, Vector2f vector2f) {
        return new Vector2f(
                scalar * vector2f.v0,
                scalar * vector2f.v1
        );
    }

    public static float dot(Vector2f lhs, Vector2f rhs) {
        return lhs.v0 * rhs.v0 + lhs.v1 * rhs.v1;
    }

    public static Vector2f reflect(Vector2f incident, Vector2f normal) {
        return incident.sub(normal.multiply(2.0f * normal.dot(incident)));
    }

}
