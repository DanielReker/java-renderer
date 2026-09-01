package io.github.danielreker.javarenderer.math;

import org.assertj.core.util.DoubleComparator;
import org.assertj.core.util.FloatComparator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static io.github.danielreker.javarenderer.math.Constants.EPSILON;
import static io.github.danielreker.javarenderer.math.Constants.EPSILON_F;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Matrix4fTest {


    static Stream<Arguments> multiplyArgumentProvider() {
        return Stream.of(
                // Identity × matrix
                Arguments.of(
                        new Matrix4f(
                                1.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 1.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 1.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 1.0f
                        ),
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        )
                ),

                // Matrix × identity
                Arguments.of(
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                1.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 1.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 1.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 1.0f
                        ),
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        )
                ),

                // Zero × matrix
                Arguments.of(
                        new Matrix4f(
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f
                        ),
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 0.0f
                        )
                ),

                // Diagonal × matrix
                Arguments.of(
                        new Matrix4f(
                                2.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 3.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 4.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 5.0f
                        ),
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                2.0f, 4.0f, 6.0f, 8.0f,
                                15.0f, 18.0f, 21.0f, 24.0f,
                                36.0f, 40.0f, 44.0f, 48.0f,
                                65.0f, 70.0f, 75.0f, 80.0f
                        )
                ),

                // Matrix × diagonal
                Arguments.of(
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                2.0f, 0.0f, 0.0f, 0.0f,
                                0.0f, 3.0f, 0.0f, 0.0f,
                                0.0f, 0.0f, 4.0f, 0.0f,
                                0.0f, 0.0f, 0.0f, 5.0f
                        ),
                        new Matrix4f(
                                2.0f, 6.0f, 12.0f, 20.0f,
                                10.0f, 18.0f, 28.0f, 40.0f,
                                18.0f, 30.0f, 44.0f, 60.0f,
                                26.0f, 42.0f, 60.0f, 80.0f
                        )
                ),

                // General multiplication
                Arguments.of(
                        new Matrix4f(
                                1.0f, 2.0f, 3.0f, 4.0f,
                                5.0f, 6.0f, 7.0f, 8.0f,
                                9.0f, 10.0f, 11.0f, 12.0f,
                                13.0f, 14.0f, 15.0f, 16.0f
                        ),
                        new Matrix4f(
                                16.0f, 15.0f, 14.0f, 13.0f,
                                12.0f, 11.0f, 10.0f, 9.0f,
                                8.0f, 7.0f, 6.0f, 5.0f,
                                4.0f, 3.0f, 2.0f, 1.0f
                        ),
                        new Matrix4f(
                                80.0f, 70.0f, 60.0f, 50.0f,
                                240.0f, 214.0f, 188.0f, 162.0f,
                                400.0f, 358.0f, 316.0f, 274.0f,
                                560.0f, 502.0f, 444.0f, 386.0f
                        )
                ),

                // General multiplication with negative values
                Arguments.of(
                        new Matrix4f(
                                1.0f, -2.0f, 3.0f, -4.0f,
                                -5.0f, 6.0f, -7.0f, 8.0f,
                                9.0f, -10.0f, 11.0f, -12.0f,
                                -13.0f, 14.0f, -15.0f, 16.0f
                        ),
                        new Matrix4f(
                                16.0f, -15.0f, 14.0f, -13.0f,
                                -12.0f, 11.0f, -10.0f, 9.0f,
                                8.0f, -7.0f, 6.0f, -5.0f,
                                -4.0f, 3.0f, -2.0f, 1.0f
                        ),
                        new Matrix4f(
                                80.0f, -70.0f, 60.0f, -50.0f,
                                -240.0f, 214.0f, -188.0f, 162.0f,
                                400.0f, -358.0f, 316.0f, -274.0f,
                                -560.0f, 502.0f, -444.0f, 386.0f
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("multiplyArgumentProvider")
    void multiply(Matrix4f lhs, Matrix4f rhs, Matrix4f expected) {
        // When
        Matrix4f actual = Matrix4f.multiply(lhs, rhs);

        // Then
        assertEquals(expected, actual);
    }


    static Stream<Arguments> determinantInvertArgumentProvider() {
        return Stream.of(
                // Identity
                Arguments.of(
                        new Matrix4f(
                                1, 0, 0, 0,
                                0, 1, 0, 0,
                                0, 0, 1, 0,
                                0, 0, 0, 1
                        ),
                        1.0f,
                        Optional.of(new Matrix4f(
                                1, 0, 0, 0,
                                0, 1, 0, 0,
                                0, 0, 1, 0,
                                0, 0, 0, 1
                        ))
                ),

                // Diagonal
                Arguments.of(
                        new Matrix4f(
                                2, 0, 0, 0,
                                0, -1, 0, 0,
                                0, 0, 4, 0,
                                0, 0, 0, 1
                        ),
                        -8.0f,
                        Optional.of(new Matrix4f(
                                0.5f, 0, 0, 0,
                                0, -1.0f, 0, 0,
                                0, 0, 0.25f, 0,
                                0, 0, 0, 1.0f
                        ))
                ),

                // Upper triangular
                Arguments.of(
                        new Matrix4f(
                                2, 1, 0, 0,
                                0, 1, 1, 0,
                                0, 0, 1, 1,
                                0, 0, 0, 1
                        ),
                        2.0f,
                        Optional.of(new Matrix4f(
                                0.5f, -0.5f,  0.5f, -0.5f,
                                0.0f,  1.0f, -1.0f,  1.0f,
                                0.0f,  0.0f,  1.0f, -1.0f,
                                0.0f,  0.0f,  0.0f,  1.0f
                        ))
                ),

                // Lower triangular
                Arguments.of(
                        new Matrix4f(
                                1, 0, 0, 0,
                                1, 2, 0, 0,
                                0, 1, 1, 0,
                                0, 0, 1, 2
                        ),
                        4.0f,
                        Optional.of(new Matrix4f(
                                1.0f,  0.0f,  0.0f,  0.0f,
                                -0.5f,  0.5f,  0.0f,  0.0f,
                                0.5f, -0.5f,  1.0f,  0.0f,
                                -0.25f, 0.25f, -0.5f, 0.5f
                        ))
                ),

                // Permutation matrix
                Arguments.of(
                        new Matrix4f(
                                0, 1, 0, 0,
                                1, 0, 0, 0,
                                0, 0, 0, 1,
                                0, 0, 1, 0
                        ),
                        1.0f,
                        Optional.of(new Matrix4f(
                                0, 1, 0, 0,
                                1, 0, 0, 0,
                                0, 0, 0, 1,
                                0, 0, 1, 0
                        ))
                ),

                // Block diagonal
                Arguments.of(
                        new Matrix4f(
                                2, 1, 0, 0,
                                1, 1, 0, 0,
                                0, 0, 1, 1,
                                0, 0, 0, 2
                        ),
                        2.0f,
                        Optional.of(new Matrix4f(
                                1.0f, -1.0f, 0.0f,  0.0f,
                                -1.0f,  2.0f, 0.0f,  0.0f,
                                0.0f,  0.0f, 1.0f, -0.5f,
                                0.0f,  0.0f, 0.0f,  0.5f
                        ))
                ),

                // General non-triangular matrix
                Arguments.of(
                        new Matrix4f(
                                1, 2, 3, 4,
                                0, 1, 4, 2,
                                2, 0, 1, 3,
                                1, 1, 0, 1
                        ),
                        -14.0f,
                        Optional.of(new Matrix4f(
                                -11.0f / 14.0f,  1.0f / 2.0f,   5.0f / 14.0f,  15.0f / 14.0f,
                                1.0f / 7.0f,    0.0f,         -3.0f / 7.0f,    5.0f / 7.0f,
                                -5.0f / 14.0f,  1.0f / 2.0f,   1.0f / 14.0f,   3.0f / 14.0f,
                                9.0f / 14.0f, -1.0f / 2.0f,   1.0f / 14.0f, -11.0f / 14.0f
                        ))
                ),

                // Singular: duplicate rows
                Arguments.of(
                        new Matrix4f(
                                1, 0, 0, 0,
                                0, 1, 0, 0,
                                1, 0, 0, 0,
                                0, 0, 0, 1
                        ),
                        0.0f,
                        Optional.empty()
                ),

                // Singular: one row is a multiple of another
                Arguments.of(
                        new Matrix4f(
                                1, 0, 0, 0,
                                0, 1, 0, 0,
                                0, 2, 0, 0,
                                0, 0, 0, 1
                        ),
                        0.0f,
                        Optional.empty()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("determinantInvertArgumentProvider")
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void determinantInvert(
            Matrix4f matrix4f,
            float expectedDeterminant,
            Optional<Matrix4f> expectedInverse
    ) {
        // When
        float actualDeterminant = matrix4f.determinant();
        Optional<Matrix4f> actualInverse = matrix4f.invert();
        Optional<Matrix4f> actualSelfMultiplication = actualInverse
                .map(inverse -> Matrix4f.multiply(matrix4f, inverse));

        // Then
        assertEquals(expectedDeterminant, actualDeterminant, EPSILON_F);

        assertThat(actualInverse)
                .usingRecursiveComparison()
                .withComparatorForType(new FloatComparator(EPSILON_F), float.class)
                .withComparatorForType(new FloatComparator(EPSILON_F), Float.class)
                .withComparatorForType(new DoubleComparator(EPSILON), double.class)
                .withComparatorForType(new DoubleComparator(EPSILON), Double.class)
                .isEqualTo(expectedInverse);

        actualSelfMultiplication.ifPresent(selfMultiplication ->
                assertThat(selfMultiplication)
                        .usingRecursiveComparison()
                        .withComparatorForType(new FloatComparator(EPSILON_F), float.class)
                        .withComparatorForType(new FloatComparator(EPSILON_F), Float.class)
                        .withComparatorForType(new DoubleComparator(EPSILON), double.class)
                        .withComparatorForType(new DoubleComparator(EPSILON), Double.class)
                        .isEqualTo(Matrix4f.IDENTITY));
    }

}