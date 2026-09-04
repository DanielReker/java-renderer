package io.github.danielreker.javarenderer.obj;

import java.util.Map;
import java.util.Optional;

public record ObjFile(
        Map<String, ObjObject> objectNameToObject
) {

    public Optional<ObjObject> getObject(String objectName) {
        return Optional.ofNullable(objectNameToObject.get(objectName));
    }

}
