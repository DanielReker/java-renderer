package io.github.danielreker.javarenderer.obj;

import io.github.danielreker.javarenderer.math.Vector2f;
import io.github.danielreker.javarenderer.math.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ObjParser {

    private final List<Vector3f> vertices = new ArrayList<>();
    private final List<Vector2f> textureCoordinates = new ArrayList<>();
    private final List<Vector3f> normals = new ArrayList<>();

    private static class ParsedObject {
        private final List<ObjFace> faces = new ArrayList<>();
    }
    private final Map<String, ParsedObject> objectNameToParsedObject = new HashMap<>();


    private ObjParser() { }


    public static ObjFile parse(InputStream inputStream) {
        final ObjParser parser = new ObjParser();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream)
        )) {
            parser.parseInternal(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ObjFile(parser
                .objectNameToParsedObject
                .entrySet()
                .stream()
                .filter(entry ->
                        !entry.getValue().faces.isEmpty())
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new ObjObject(
                                entry.getKey(),
                                Collections.unmodifiableList(entry.getValue().faces)
                        )
                )));
    }

    private void parseInternal(BufferedReader reader) throws IOException {
        ParsedObject parsedObject = getParsedObject("");

        String line;
        while ((line = reader.readLine()) != null) {
            final List<String> lineTokens = List
                    .of(line.strip().split("\\s+"));

            switch (lineTokens.getFirst()) {
                case "o" -> {
                    if (lineTokens.size() >= 2) {
                        parsedObject = getParsedObject(lineTokens.get(1));
                    }
                }
                case "v" -> vertices.add(parseVector3f(lineTokens));
                case "vt" -> textureCoordinates.add(parseVector2f(lineTokens));
                case "vn" -> normals.add(parseVector3f(lineTokens));
                case "f" -> parsedObject.faces.add(parseObjFace(lineTokens));
            }
        }
    }

    private static Vector2f parseVector2f(List<String> lineTokens) {
        return Vector2f.of(
                Float.parseFloat(lineTokens.get(1)),
                Float.parseFloat(lineTokens.get(2))
        );
    }

    private static Vector3f parseVector3f(List<String> lineTokens) {
        return Vector3f.of(
                Float.parseFloat(lineTokens.get(1)),
                Float.parseFloat(lineTokens.get(2)),
                Float.parseFloat(lineTokens.get(3))
        );
    }

    private ObjFace parseObjFace(List<String> lineTokens) {
        return new ObjFace(
                parseObjVertex(lineTokens.get(1)),
                parseObjVertex(lineTokens.get(2)),
                parseObjVertex(lineTokens.get(3))
        );
    }

    private ObjVertex parseObjVertex(String token) {
        List<Integer> indices = Stream
                .of(token.split("/"))
                .map(indexStr -> {
                    if (indexStr.isBlank()) {
                        return null;
                    } else {
                        return Integer.parseInt(indexStr);
                    }
                })
                .toList();

        return new ObjVertex(
                Optional.ofNullable(indices.getFirst())
                        .map(index -> vertices.get(index - 1))
                        .orElse(null),
                Optional.ofNullable(indices.get(1))
                        .map(index -> textureCoordinates.get(index - 1))
                        .orElse(null),
                Optional.ofNullable(indices.get(2))
                        .map(index -> normals.get(index - 1))
                        .orElse(null)
        );
    }

    private ParsedObject getParsedObject(String objectName) {
        return objectNameToParsedObject
                .computeIfAbsent(objectName, name -> new ParsedObject());
    }

}
