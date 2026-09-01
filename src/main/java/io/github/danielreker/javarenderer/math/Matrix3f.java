package io.github.danielreker.javarenderer.math;

public record Matrix3f(
        float m00, float m10, float m20,
        float m01, float m11, float m21,
        float m02, float m12, float m22
) {

    public static final Matrix3f IDENTITY = new Matrix3f(
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    );

    public static Vector3f multiply(Matrix3f Matrix3f, Vector3f Vector3f) {
        return new Vector3f(
                Matrix3f.m00 * Vector3f.v0() + Matrix3f.m10 * Vector3f.v1() + Matrix3f.m20 * Vector3f.v2(),
                Matrix3f.m01 * Vector3f.v0() + Matrix3f.m11 * Vector3f.v1() + Matrix3f.m21 * Vector3f.v2(),
                Matrix3f.m02 * Vector3f.v0() + Matrix3f.m12 * Vector3f.v1() + Matrix3f.m22 * Vector3f.v2()
        );
    }

}
