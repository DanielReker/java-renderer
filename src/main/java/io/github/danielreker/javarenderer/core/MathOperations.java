package io.github.danielreker.javarenderer.core;

import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;
import io.github.danielreker.javarenderer.math.Vector4f;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public final class MathOperations<T> {

    private static final Map<Class<?>, MathOperations<?>> CLASS_TO_MATH_OPERATIONS = Map.of(
            Float.class, new MathOperations<>(
                    Float.class,
                    Float::sum,
                    (value, scalar) -> value * scalar
            ),
            Vector2f.class, new MathOperations<>(
                    Vector2f.class,
                    (v1, v2) -> Vector2f.add(v1, v2),
                    Vector2f::multiply
            ),
            Vector3f.class, new MathOperations<>(
                    Vector3f.class,
                    (v1, v2) -> Vector3f.add(v1, v2),
                    Vector3f::multiply
            ),
            Vector4f.class, new MathOperations<>(
                    Vector4f.class,
                    (v1, v2) -> Vector4f.add(v1, v2),
                    Vector4f::multiply
            )
    );


    private final Class<T> tClass;

    private final BinaryOperator<T> add;

    private final BiFunction<T, Float, T> multiply;


    public MathOperations(
            Class<T> tClass,
            BinaryOperator<T> add,
            BiFunction<T, Float, T> multiply
    ) {
        this.tClass = tClass;
        this.add = add;
        this.multiply = multiply;
    }


    public static MathOperations<?> forClass(Class<?> clazz) {
        return Optional
                .ofNullable(CLASS_TO_MATH_OPERATIONS.get(clazz))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Class " + clazz.getName() + " is not supported"
                ));
    }


    public T add(Object value1, Object value2) {
        return add.apply(tClass.cast(value1), tClass.cast(value2));
    }

    public T multiply(Object value, Float scalar) {
        return multiply.apply(tClass.cast(value), scalar);
    }

}
