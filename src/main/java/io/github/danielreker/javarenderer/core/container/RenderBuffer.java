package io.github.danielreker.javarenderer.core.container;

public class RenderBuffer<T> {
    private final int width;
    private final int height;
    private final T[][] data;
    private final Class<T> dataType;

    @SuppressWarnings("unchecked")
    public RenderBuffer(int width, int height, Class<T> dataType, T initialValue) {
        this.width = width;
        this.height = height;
        this.dataType = dataType;
        this.data = (T[][]) new Object[height][width];
        if (initialValue != null) {
            clear(initialValue);
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Class<T> getDataType() { return dataType; }

    public void setValue(int x, int y, T value) {
        if (0 <= x && x < width && 0 <= y && y < height) {
            this.data[y][x] = value;
        }
    }

    public T getValue(int x, int y) {
        if (0 <= x && x < width && 0 <= y && y < height) {
            return this.data[y][x];
        }
        return null;
    }

    public void clear(T clearValue) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.data[y][x] = clearValue;
            }
        }
    }
}