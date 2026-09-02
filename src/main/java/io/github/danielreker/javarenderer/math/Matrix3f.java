package io.github.danielreker.javarenderer.math;

public record Matrix3f(
        float m00, float m01, float m02,
        float m10, float m11, float m12,
        float m20, float m21, float m22
) {

    public static final Matrix3f IDENTITY = new Matrix3f(
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
    );

    public static Vector3f multiply(Matrix3f matrix3f, Vector3f vector3f) {
        return new Vector3f(
                matrix3f.m00 * vector3f.v0() + matrix3f.m01 * vector3f.v1() + matrix3f.m02 * vector3f.v2(),
                matrix3f.m10 * vector3f.v0() + matrix3f.m11 * vector3f.v1() + matrix3f.m12 * vector3f.v2(),
                matrix3f.m20 * vector3f.v0() + matrix3f.m21 * vector3f.v1() + matrix3f.m22 * vector3f.v2()
        );
    }

}
