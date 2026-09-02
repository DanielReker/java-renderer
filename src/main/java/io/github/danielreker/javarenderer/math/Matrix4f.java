package io.github.danielreker.javarenderer.math;

import java.util.Optional;

import static io.github.danielreker.javarenderer.math.Constants.EPSILON_F;

public record Matrix4f(
        float m00, float m01, float m02, float m03,
        float m10, float m11, float m12, float m13,
        float m20, float m21, float m22, float m23,
        float m30, float m31, float m32, float m33
) {

    public static final Matrix4f IDENTITY = new Matrix4f(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    );


    public Matrix3f mat3() {
        return new Matrix3f(
                m00, m01, m02,
                m10, m11, m12,
                m20, m21, m22
        );
    }

    public Matrix4f transpose() {
        return new Matrix4f(
                m00, m10, m20, m30,
                m01, m11, m21, m31,
                m02, m12, m22, m32,
                m03, m13, m23, m33
        );
    }

    public float determinant() {
        return m00 * m11 * m22 * m33 -
                m00 * m11 * m23 * m32 -
                m00 * m12 * m21 * m33 +
                m00 * m12 * m23 * m31 +
                m00 * m13 * m21 * m32 -
                m00 * m13 * m22 * m31 -
                m01 * m10 * m22 * m33 +
                m01 * m10 * m23 * m32 +
                m01 * m12 * m20 * m33 -
                m01 * m12 * m23 * m30 -
                m01 * m13 * m20 * m32 +
                m01 * m13 * m22 * m30 +
                m02 * m10 * m21 * m33 -
                m02 * m10 * m23 * m31 -
                m02 * m11 * m20 * m33 +
                m02 * m11 * m23 * m30 +
                m02 * m13 * m20 * m31 -
                m02 * m13 * m21 * m30 -
                m03 * m10 * m21 * m32 +
                m03 * m10 * m22 * m31 +
                m03 * m11 * m20 * m32 -
                m03 * m11 * m22 * m30 -
                m03 * m12 * m20 * m31 +
                m03 * m12 * m21 * m30;
    }

    public Optional<Matrix4f> invert() {
        final float det = determinant();
        if (Math.abs(det) <= EPSILON_F) {
            return Optional.empty();
        }

        return Optional.of(new Matrix4f(
                // m00
                (m11 * m22 * m33
                        - m11 * m23 * m32
                        - m12 * m21 * m33
                        + m12 * m23 * m31
                        + m13 * m21 * m32
                        - m13 * m22 * m31) / det,

                // m01
                (-m01 * m22 * m33
                        + m01 * m23 * m32
                        + m02 * m21 * m33
                        - m02 * m23 * m31
                        - m03 * m21 * m32
                        + m03 * m22 * m31) / det,

                // m02
                (m01 * m12 * m33
                        - m01 * m13 * m32
                        - m02 * m11 * m33
                        + m02 * m13 * m31
                        + m03 * m11 * m32
                        - m03 * m12 * m31) / det,

                // m03
                (-m01 * m12 * m23
                        + m01 * m13 * m22
                        + m02 * m11 * m23
                        - m02 * m13 * m21
                        - m03 * m11 * m22
                        + m03 * m12 * m21) / det,

                // m10
                (-m10 * m22 * m33
                        + m10 * m23 * m32
                        + m12 * m20 * m33
                        - m12 * m23 * m30
                        - m13 * m20 * m32
                        + m13 * m22 * m30) / det,

                // m11
                (m00 * m22 * m33
                        - m00 * m23 * m32
                        - m02 * m20 * m33
                        + m02 * m23 * m30
                        + m03 * m20 * m32
                        - m03 * m22 * m30) / det,

                // m12
                (-m00 * m12 * m33
                        + m00 * m13 * m32
                        + m02 * m10 * m33
                        - m02 * m13 * m30
                        - m03 * m10 * m32
                        + m03 * m12 * m30) / det,

                // m13
                (m00 * m12 * m23
                        - m00 * m13 * m22
                        - m02 * m10 * m23
                        + m02 * m13 * m20
                        + m03 * m10 * m22
                        - m03 * m12 * m20) / det,

                // m20
                (m10 * m21 * m33
                        - m10 * m23 * m31
                        - m11 * m20 * m33
                        + m11 * m23 * m30
                        + m13 * m20 * m31
                        - m13 * m21 * m30) / det,

                // m21
                (-m00 * m21 * m33
                        + m00 * m23 * m31
                        + m01 * m20 * m33
                        - m01 * m23 * m30
                        - m03 * m20 * m31
                        + m03 * m21 * m30) / det,

                // m22
                (m00 * m11 * m33
                        - m00 * m13 * m31
                        - m01 * m10 * m33
                        + m01 * m13 * m30
                        + m03 * m10 * m31
                        - m03 * m11 * m30) / det,

                // m23
                (-m00 * m11 * m23
                        + m00 * m13 * m21
                        + m01 * m10 * m23
                        - m01 * m13 * m20
                        - m03 * m10 * m21
                        + m03 * m11 * m20) / det,

                // m30
                (-m10 * m21 * m32
                        + m10 * m22 * m31
                        + m11 * m20 * m32
                        - m11 * m22 * m30
                        - m12 * m20 * m31
                        + m12 * m21 * m30) / det,

                // m31
                (m00 * m21 * m32
                        - m00 * m22 * m31
                        - m01 * m20 * m32
                        + m01 * m22 * m30
                        + m02 * m20 * m31
                        - m02 * m21 * m30) / det,

                // m32
                (-m00 * m11 * m32
                        + m00 * m12 * m31
                        + m01 * m10 * m32
                        - m01 * m12 * m30
                        - m02 * m10 * m31
                        + m02 * m11 * m30) / det,

                // m33
                (m00 * m11 * m22
                        - m00 * m12 * m21
                        - m01 * m10 * m22
                        + m01 * m12 * m20
                        + m02 * m10 * m21
                        - m02 * m11 * m20) / det
        ));
    }


    public static Matrix4f multiply(Matrix4f lhs, Matrix4f rhs) {
        return new Matrix4f(
                lhs.m00 * rhs.m00 + lhs.m01 * rhs.m10 + lhs.m02 * rhs.m20 + lhs.m03 * rhs.m30,
                lhs.m00 * rhs.m01 + lhs.m01 * rhs.m11 + lhs.m02 * rhs.m21 + lhs.m03 * rhs.m31,
                lhs.m00 * rhs.m02 + lhs.m01 * rhs.m12 + lhs.m02 * rhs.m22 + lhs.m03 * rhs.m32,
                lhs.m00 * rhs.m03 + lhs.m01 * rhs.m13 + lhs.m02 * rhs.m23 + lhs.m03 * rhs.m33,

                lhs.m10 * rhs.m00 + lhs.m11 * rhs.m10 + lhs.m12 * rhs.m20 + lhs.m13 * rhs.m30,
                lhs.m10 * rhs.m01 + lhs.m11 * rhs.m11 + lhs.m12 * rhs.m21 + lhs.m13 * rhs.m31,
                lhs.m10 * rhs.m02 + lhs.m11 * rhs.m12 + lhs.m12 * rhs.m22 + lhs.m13 * rhs.m32,
                lhs.m10 * rhs.m03 + lhs.m11 * rhs.m13 + lhs.m12 * rhs.m23 + lhs.m13 * rhs.m33,

                lhs.m20 * rhs.m00 + lhs.m21 * rhs.m10 + lhs.m22 * rhs.m20 + lhs.m23 * rhs.m30,
                lhs.m20 * rhs.m01 + lhs.m21 * rhs.m11 + lhs.m22 * rhs.m21 + lhs.m23 * rhs.m31,
                lhs.m20 * rhs.m02 + lhs.m21 * rhs.m12 + lhs.m22 * rhs.m22 + lhs.m23 * rhs.m32,
                lhs.m20 * rhs.m03 + lhs.m21 * rhs.m13 + lhs.m22 * rhs.m23 + lhs.m23 * rhs.m33,

                lhs.m30 * rhs.m00 + lhs.m31 * rhs.m10 + lhs.m32 * rhs.m20 + lhs.m33 * rhs.m30,
                lhs.m30 * rhs.m01 + lhs.m31 * rhs.m11 + lhs.m32 * rhs.m21 + lhs.m33 * rhs.m31,
                lhs.m30 * rhs.m02 + lhs.m31 * rhs.m12 + lhs.m32 * rhs.m22 + lhs.m33 * rhs.m32,
                lhs.m30 * rhs.m03 + lhs.m31 * rhs.m13 + lhs.m32 * rhs.m23 + lhs.m33 * rhs.m33
        );
    }

    public static Vector4f multiply(Matrix4f matrix4f, Vector4f vector4f) {
        return new Vector4f(
                matrix4f.m00 * vector4f.v0() + matrix4f.m01 * vector4f.v1() + matrix4f.m02 * vector4f.v2() + matrix4f.m03 * vector4f.v3(),
                matrix4f.m10 * vector4f.v0() + matrix4f.m11 * vector4f.v1() + matrix4f.m12 * vector4f.v2() + matrix4f.m13 * vector4f.v3(),
                matrix4f.m20 * vector4f.v0() + matrix4f.m21 * vector4f.v1() + matrix4f.m22 * vector4f.v2() + matrix4f.m23 * vector4f.v3(),
                matrix4f.m30 * vector4f.v0() + matrix4f.m31 * vector4f.v1() + matrix4f.m32 * vector4f.v2() + matrix4f.m33 * vector4f.v3()
        );
    }

    public static Matrix4f translation(Vector3f delta) {
        return new Matrix4f(
                1.0f, 0.0f, 0.0f, delta.x(),
                0.0f, 1.0f, 0.0f, delta.y(),
                0.0f, 0.0f, 1.0f, delta.z(),
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public static Matrix4f rotationAroundX(float angleRad) {
        final float cos = (float) Math.cos(angleRad);
        final float sin = (float) Math.sin(angleRad);
        return new Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, cos, -sin, 0.0f,
                0.0f, sin, cos, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public static Matrix4f rotationAroundY(float angleRad) {
        final float cos = (float) Math.cos(angleRad);
        final float sin = (float) Math.sin(angleRad);
        return new Matrix4f(
                cos, 0.0f, -sin, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                sin, 0.0f, cos, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public static Matrix4f rotationAroundZ(float angleRad) {
        final float cos = (float) Math.cos(angleRad);
        final float sin = (float) Math.sin(angleRad);
        return new Matrix4f(
                cos, -sin, 0.0f, 0.0f,
                sin, cos, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public static Matrix4f perspective(float verticalFovRad, float aspect, float zNear, float zFar) {
        float f = 1 / (float) Math.tan(verticalFovRad / 2);
        return new Matrix4f(
                f / aspect, 0.0f, 0.0f, 0.0f,
                0.0f, f, 0.0f, 0.0f,
                0.0f, 0.0f, (zNear + zFar) / (zNear - zFar), 2.0f * zNear * zFar / (zNear - zFar),
                0.0f, 0.0f, -1.0f, 0.0f
        );
    }

    public static Matrix4f ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        return new Matrix4f(
                2.0f / (right - left), 0.0f, 0.0f, (left + right) / (left - right),
                0.0f, 2.0f / (top - bottom), 0.0f, (bottom + top) / (bottom - top),
                0.0f, 0.0f, 2.0f / (zNear - zFar), (zNear + zFar) / (zNear - zFar),
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

}
