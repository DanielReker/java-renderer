package io.github.danielreker.javarenderer.core.shader;

import io.github.danielreker.javarenderer.core.shader.annotations.Attribute;
import io.github.danielreker.javarenderer.core.shader.annotations.Uniform;
import io.github.danielreker.javarenderer.core.shader.annotations.Varying;
import io.github.danielreker.javarenderer.core.shader.io.FragmentShaderIoBase;
import io.github.danielreker.javarenderer.core.shader.io.VertexShaderIoBase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Function;

public class ShaderProgram<VS_IO extends VertexShaderIoBase, FS_IO extends FragmentShaderIoBase> {

    private final AbstractVertexShader<VS_IO> vertexShader;
    private final Class<VS_IO> vertexIoClass;
    private final Constructor<VS_IO> vertexIoConstructor;

    private final AbstractFragmentShader<FS_IO> fragmentShader;
    private final Class<FS_IO> fragmentIoClass;
    private final Constructor<FS_IO> fragmentIoConstructor;

    private final Map<String, Object> uniformValues = new HashMap<>();

    private final Map<String, Field> vertexShaderAttributeInputFields = new HashMap<>();
    private final Map<String, Field> vertexShaderUniformInputFields = new HashMap<>();
    private final Map<String, Field> vertexShaderVaryingOutputFields = new HashMap<>();
    private final Map<String, Field> fragmentShaderVaryingInputFields = new HashMap<>();
    private final Map<String, Field> fragmentShaderUniformInputFields = new HashMap<>();


    @SuppressWarnings("unchecked")
    private ShaderProgram(AbstractVertexShader<VS_IO> vs, AbstractFragmentShader<FS_IO> fs) {
        this.vertexShader = Objects.requireNonNull(vs, "Vertex shader cannot be null");
        this.fragmentShader = Objects.requireNonNull(fs, "Fragment shader cannot be null");

        this.vertexIoClass = (Class<VS_IO>)
                ((ParameterizedType) vs.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.fragmentIoClass = (Class<FS_IO>)
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
        Arrays.stream(vertexIoClass.getFields())
                .filter(f -> f.isAnnotationPresent(Varying.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderVaryingOutputFields.put(field.getName(), field);
                });

        Arrays.stream(fragmentIoClass.getFields())
                .filter(f -> f.isAnnotationPresent(Varying.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    fragmentShaderVaryingInputFields.put(field.getName(), field);
                    if (!vertexShaderVaryingOutputFields.containsKey(field.getName())) {
                        throw new IllegalStateException("Varying input '" + field.getName() +
                                "' in fragment shader I/O " + fragmentIoClass.getSimpleName() +
                                " has no matching varying output in vertex shader I/O " +
                                vertexIoClass.getSimpleName());
                    }
                });

        Arrays.stream(fragmentIoClass.getFields())
                .filter(f -> f.isAnnotationPresent(Uniform.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    fragmentShaderUniformInputFields.put(field.getName(), field);
                });

        Arrays.stream(vertexIoClass.getFields())
                .filter(f -> f.isAnnotationPresent(Uniform.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderUniformInputFields.put(field.getName(), field);
                });

        Arrays.stream(vertexIoClass.getFields())
                .filter(f -> f.isAnnotationPresent(Attribute.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    vertexShaderAttributeInputFields.put(field.getName(), field);
                });
    }


    public static <VS_IO extends VertexShaderIoBase, FS_IO extends FragmentShaderIoBase>
    ShaderProgram<VS_IO, FS_IO> create(AbstractVertexShader<VS_IO> vs, AbstractFragmentShader<FS_IO> fs) {
        return new ShaderProgram<>(vs, fs);
    }

    public <T> void setUniform(String name, T value) {
        uniformValues.put(name, value);
    }


    public VS_IO createAndPrepareVertexIO() {
        try {
            VS_IO vsIo = vertexIoConstructor.newInstance();
            populateFields(vsIo, vertexShaderUniformInputFields.values(),
                    field -> uniformValues.get(field.getName()));
            return vsIo;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create Vertex IO", e);
        }
    }

    public VS_IO createAndPrepareVertexIO(Object vertexObject) {
        try {
            VS_IO vsIo = vertexIoConstructor.newInstance();

            populateFields(vsIo, vertexShaderAttributeInputFields.values(),
                    field -> {
                        try {
                            Field sourceField = vertexObject.getClass()
                                    .getDeclaredField(field.getName());
                            sourceField.setAccessible(true);
                            return sourceField.get(vertexObject);
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException("Failed to get field value", e);
                        }
                    });

            populateFields(vsIo, vertexShaderUniformInputFields.values(),
                    field -> uniformValues.get(field.getName()));

            return vsIo;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create Vertex IO", e);
        }
    }

    public FS_IO createAndPrepareFragmentIO(
            Map<String, Object> interpolatedVaryings
    ) {
        try {
            FS_IO fsIo = fragmentIoConstructor.newInstance();

            populateFields(fsIo, fragmentShaderVaryingInputFields.values(),
                    field -> interpolatedVaryings.get(field.getName()));

            populateFields(fsIo, fragmentShaderUniformInputFields.values(),
                    field -> uniformValues.get(field.getName()));

            return fsIo;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create Fragment IO", e);
        }
    }

    private void populateFields(
            Object ioInstance,
            Collection<Field> fields,
            Function<Field, Object> getFieldValue
    ) {
        for (final Field field : fields) {
            field.setAccessible(true);
            final Object value = getFieldValue.apply(field);
            if (value != null) {
                try {
                    field.set(ioInstance, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to populate field " + field.getName(), e);
                }
            }
        }
    }

    public void executeVertexShader(VS_IO vsIo) {
        vertexShader.main(vsIo);
    }

    public void executeFragmentShader(FS_IO fsIo) {
        fragmentShader.main(fsIo);
    }

    public Map<String, Field> getVertexShaderVaryingOutputFields() {
        return Collections.unmodifiableMap(vertexShaderVaryingOutputFields);
    }

}