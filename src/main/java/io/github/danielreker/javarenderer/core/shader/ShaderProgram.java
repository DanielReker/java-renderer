package io.github.danielreker.javarenderer.core.shader;

import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;
import org.joml.Vector4f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

public class ShaderProgram<V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase> {

    private final AbstractVertexShader<V_IO> vertexShader;
    private final Class<V_IO> vertexIoClass;
    private final Constructor<V_IO> vertexIoConstructor;

    private final AbstractFragmentShader<F_IO> fragmentShader;
    private final Class<F_IO> fragmentIoClass;
    private final Constructor<F_IO> fragmentIoConstructor;

    private final Map<String, Object> uniformValues = new HashMap<>();

    private final Map<String, Field> vertexShaderAttributeInputFields = new HashMap<>();
    private final Map<String, Field> vertexShaderUniformInputFields = new HashMap<>();
    private final Map<String, Field> vertexShaderVaryingOutputFields = new HashMap<>();
    private final Map<String, Field> fragmentShaderVaryingInputFields = new HashMap<>();
    private final Map<String, Field> fragmentShaderUniformInputFields = new HashMap<>();


    @SuppressWarnings("unchecked")
    private ShaderProgram(AbstractVertexShader<V_IO> vs, AbstractFragmentShader<F_IO> fs) {
        this.vertexShader = Objects.requireNonNull(vs, "Vertex shader cannot be null");
        this.fragmentShader = Objects.requireNonNull(fs, "Fragment shader cannot be null");

        this.vertexIoClass = (Class<V_IO>)
                ((ParameterizedType) vs.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.fragmentIoClass = (Class<F_IO>)
                ((ParameterizedType) fs.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        try {
            this.vertexIoConstructor = this.vertexIoClass.getDeclaredConstructor();
            this.vertexIoConstructor.setAccessible(true);
            this.fragmentIoConstructor = this.fragmentIoClass.getDeclaredConstructor();
            this.fragmentIoConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Shader I/O classes must have a no-arg constructor.", e);
        }

        cacheFields();
    }

    private void cacheFields() {
        Arrays.stream(vertexIoClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Varying.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderVaryingOutputFields.put(field.getName(), field);
                });

        Arrays.stream(fragmentIoClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Varying.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    fragmentShaderVaryingInputFields.put(field.getName(), field);
                    if (!vertexShaderVaryingOutputFields.containsKey(field.getName())) {
                        System.err.println("Warning: Varying input '" + field.getName() +
                                "' in fragment shader I/O " + fragmentIoClass.getSimpleName() +
                                " has no matching varying output in vertex shader I/O " +
                                vertexIoClass.getSimpleName());
                    }
                });

        Arrays.stream(fragmentIoClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Uniform.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    fragmentShaderUniformInputFields.put(field.getName(), field);
                });

        Arrays.stream(vertexIoClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Uniform.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderUniformInputFields.put(field.getName(), field);
                });

        Arrays.stream(vertexIoClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Attribute.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderAttributeInputFields.put(field.getName(), field);
                });
    }


    public static <V_IO extends VertexShaderIoBase, F_IO extends FragmentShaderIoBase>
    ShaderProgram<V_IO, F_IO> create(AbstractVertexShader<V_IO> vs, AbstractFragmentShader<F_IO> fs) {
        return new ShaderProgram<>(vs, fs);
    }

    public <T> void setUniform(String name, T value) {
        uniformValues.put(name, value);
    }


    public V_IO createAndPrepareVertexIO(Object vertexObject) {
        try {
            V_IO vsIo = vertexIoConstructor.newInstance();

            populateFields(vsIo, vertexShaderAttributeInputFields.values(),
                    field -> {
                        try {
                            Field sourceField = vertexObject.getClass().getDeclaredField(field.getName());
                            sourceField.setAccessible(true);
                            return sourceField.get(vertexObject);
                        } catch (NoSuchFieldException e) {
                            System.err.println("Warning: Attribute '" + field.getName()
                                    + "' not found in vertex object " + vertexObject.getClass().getSimpleName());
                            return null;
                        } catch (IllegalAccessException e) {
                            System.err.println("Warning: Failed to get '" + field.getName()
                                    + "' value via reflection from vertex object"
                                    + vertexObject.getClass().getSimpleName());
                            return null;
                        }
                    });

            populateFields(vsIo, vertexShaderUniformInputFields.values(),
                    field -> uniformValues.get(field.getName()));

            return vsIo;
        } catch (ReflectiveOperationException e) {
            System.err.println("Warning: Reflective operation failed: " + e.getMessage());
            return null;
        }
    }

    public F_IO createAndPrepareFragmentIO(Map<String, Object> interpolatedVaryings, Vector4f fragmentCoordinates) {
        try {
            F_IO fsIo = fragmentIoConstructor.newInstance();

            populateFields(fsIo, fragmentShaderVaryingInputFields.values(),
                    field -> interpolatedVaryings.get(field.getName()));

            populateFields(fsIo, fragmentShaderUniformInputFields.values(),
                    field -> uniformValues.get(field.getName()));


            fsIo.gl_FragCoord = fragmentCoordinates;

            return fsIo;
        } catch (ReflectiveOperationException e) {
            System.err.println("Warning: Reflective operation failed: " + e.getMessage());
            return null;
        }
    }

    private void populateFields(
            Object ioInstance,
            Collection<Field> fields,
            Function<Field, Object> getFieldValue
    ) {
                fields.forEach(field -> {
                    field.setAccessible(true);
                    Object value = getFieldValue.apply(field);
                    if (value != null) {
                        if (isCompatibleType(field.getType(), value.getClass())) {
                            try {
                                field.set(ioInstance, value);
                            } catch (IllegalAccessException e) {
                                System.err.println("Warning: Reflective operation failed: " + e.getMessage());
                            }
                        } else {
                            System.err.println("Warning: Type mismatch for field '" + field.getName() +
                                    "'. Expected " + field.getType().getSimpleName() +
                                    ", got " + value.getClass().getSimpleName() + ". Skipping.");
                        }
                    }
                });
    }

    private boolean isCompatibleType(Class<?> fieldType, Class<?> valueType) {
        if (fieldType.isAssignableFrom(valueType)) return true;
        if (fieldType.isPrimitive()) {
            return getWrapperClass(fieldType).isAssignableFrom(valueType);
        }
        if (valueType.isPrimitive()) {
            return fieldType.isAssignableFrom(getWrapperClass(valueType));
        }
        return false;
    }

    private Class<?> getWrapperClass(Class<?> primitiveClass) {
        if (primitiveClass == int.class) return Integer.class;
        if (primitiveClass == float.class) return Float.class;
        if (primitiveClass == boolean.class) return Boolean.class;
        if (primitiveClass == double.class) return Double.class;
        if (primitiveClass == char.class) return Character.class;
        if (primitiveClass == byte.class) return Byte.class;
        if (primitiveClass == short.class) return Short.class;
        if (primitiveClass == long.class) return Long.class;
        return primitiveClass;
    }


    public void executeVertexShader(V_IO vsIo) {
        vertexShader.main(vsIo);
    }

    public void executeFragmentShader(F_IO fsIo) {
        fragmentShader.main(fsIo);
    }

    public Map<String, Field> getVertexShaderVaryingOutputFields() { return Collections.unmodifiableMap(vertexShaderVaryingOutputFields); }
    public Map<String, Field> getFragmentShaderVaryingInputFields() { return Collections.unmodifiableMap(fragmentShaderVaryingInputFields); }

    public Class<V_IO> getVertexIoClass() { return vertexIoClass; }
    public Class<F_IO> getFragmentIoClass() { return fragmentIoClass; }
}