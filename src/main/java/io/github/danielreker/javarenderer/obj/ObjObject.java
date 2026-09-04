package io.github.danielreker.javarenderer.obj;

import java.util.List;

public record ObjObject(
        String name,
        List<ObjFace> faces
) {
}
