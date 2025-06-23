package io.github.danielreker.javarenderer.core.container;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class VertexBuffer<V> {

    private final List<V> vertices;


    private VertexBuffer(List<V> data) {
        this.vertices = List.copyOf(data);
    }

    public static <V> VertexBuffer<V> create(List<V> data) {
        Objects.requireNonNull(data, "Vertex data list cannot be null.");
        return new VertexBuffer<>(data);
    }

    public V getVertex(int vertexIndex) {
        if (vertexIndex < 0 || vertexIndex >= vertices.size()) {
            throw new IndexOutOfBoundsException("Vertex index " + vertexIndex +
                    " is out of bounds for VertexBuffer of size " + vertices.size());
        }
        return vertices.get(vertexIndex);
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public Stream<V> stream() {
        return vertices.stream();
    }

    public Stream<V> streamRange(int firstVertex, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative: " + count);
        }
        if (firstVertex < 0 || firstVertex + count > vertices.size()) {
            throw new IndexOutOfBoundsException("Range [" + firstVertex + ", " + (firstVertex + count -1) +
                    "] is out of bounds for VertexBuffer of size " + vertices.size());
        }
        if (count == 0) {
            return Stream.empty();
        }

        return vertices.subList(firstVertex, firstVertex + count).stream();
    }

    public List<V> getVertices() {
        return vertices;
    }
}