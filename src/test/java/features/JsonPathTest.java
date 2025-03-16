package features;

import org.noear.snack.ONode;
import org.noear.snack.schema.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPathTest {

    private static final String JSON = "{\n" +
            "  \"store\": {\n" +
            "    \"book\": [\n" +
            "      {\n" +
            "        \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      {\n" +
            "        \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12.99\n" +
            "      },\n" +
            "      {\n" +
            "        \"category\": \"fiction\",\n" +
            "        \"author\": \"Herman Melville\",\n" +
            "        \"title\": \"Moby Dick\",\n" +
            "        \"price\": 8.99\n" +
            "      },\n" +
            "      {\n" +
            "        \"category\": \"fiction\",\n" +
            "        \"author\": \"J. R. R. Tolkien\",\n" +
            "        \"title\": \"The Lord of the Rings\",\n" +
            "        \"price\": 22.99\n" +
            "      }\n" +
            "    ],\n" +
            "    \"bicycle\": {\n" +
            "      \"color\": \"red\",\n" +
            "      \"price\": 19.95\n" +
            "    }\n" +
            "  },\n" +
            "  \"expensive\": 10\n" +
            "}";

    @Test
    public void testGetRoot() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$");
        assertNotNull(result);
        assertTrue(result.isObject());
    }

    @Test
    public void testGetStore() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store");
        assertNotNull(result);
        assertTrue(result.isObject());
    }

    @Test
    public void testGetBooks() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(4, result.size());
    }

    @Test
    public void testGetFirstBook() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[0]");
        assertNotNull(result);
        assertEquals("Nigel Rees", result.get("author").getString());
    }

    @Test
    public void testGetLastBook() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[-1]");
        assertNotNull(result);
        assertEquals("J. R. R. Tolkien", result.get("author").getString());
    }

    @Test
    public void testGetBookByIndex() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[2]");
        assertNotNull(result);
        assertEquals("Herman Melville", result.get("author").getString());
    }

    @Test
    public void testGetBookCategory() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[0].category");
        assertNotNull(result);
        assertEquals("reference", result.getString());
    }

    @Test
    public void testGetBicycle() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.bicycle");
        assertNotNull(result);
        assertTrue(result.isObject());
    }

    @Test
    public void testGetBicycleColor() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.bicycle.color");
        assertNotNull(result);
        assertEquals("red", result.getString());
    }

    @Test
    public void testGetBicyclePrice() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.bicycle.price");
        assertNotNull(result);
        assertEquals(19.95, result.getDouble());
    }

    @Test
    public void testGetExpensive() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.expensive");
        assertNotNull(result);
        assertEquals(10, result.getInt());
    }

    @Test
    public void testGetNonExistentKey() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.nonExistentKey");
        });
    }

    @Test
    public void testGetInvalidIndex() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[10]");
        });
    }

    @Test
    public void testGetNegativeIndex() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[-10]");
        });
    }

    @Test
    public void testGetNestedNonExistentKey() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[0].nonExistentKey");
        });
    }

    @Test
    public void testGetInvalidPath() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "invalidPath");
        });
    }

    @Test
    public void testGetNullNode() {
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(null, "$.store");
        });
    }

    @Test
    public void testGetEmptyPath() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "");
        });
    }

    @Test
    public void testGetRootArray() throws IOException {
        String json = "[1, 2, 3]";
        ONode root = ONode.load(json);
        ONode result = JsonPath.query(root, "$");
        assertNotNull(result);
        assertTrue(result.isArray());
    }

    @Test
    public void testGetArrayElement() throws IOException {
        String json = "[1, 2, 3]";
        ONode root = ONode.load(json);
        ONode result = JsonPath.query(root, "$[1]");
        assertNotNull(result);
        assertEquals(2, result.getInt());
    }

    @Test
    public void testGetArrayOutOfBounds() throws IOException {
        String json = "[1, 2, 3]";
        ONode root = ONode.load(json);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$[10]");
        });
    }

    @Test
    public void testGetArrayNegativeIndex() throws IOException {
        String json = "[1, 2, 3]";
        ONode root = ONode.load(json);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$[-1]");
        });
    }

    @Test
    public void testGetNestedArray() throws IOException {
        String json = "{\"nested\": [1, 2, 3]}";
        ONode root = ONode.load(json);
        ONode result = JsonPath.query(root, "$.nested[1]");
        assertNotNull(result);
        assertEquals(2, result.getInt());
    }

    @Test
    public void testGetNestedArrayOutOfBounds() throws IOException {
        String json = "{\"nested\": [1, 2, 3]}";
        ONode root = ONode.load(json);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.nested[10]");
        });
    }

    @Test
    public void testGetNestedArrayNegativeIndex() throws IOException {
        String json = "{\"nested\": [1, 2, 3]}";
        ONode root = ONode.load(json);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.nested[-1]");
        });
    }

    @Test
    public void testGetComplexPath() throws IOException {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[2].title");
        assertNotNull(result);
        assertEquals("Moby Dick", result.getString());
    }

    @Test
    public void testGetComplexPathNonExistent() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[2].nonExistentKey");
        });
    }

    @Test
    public void testGetComplexPathInvalidIndex() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[10].title");
        });
    }

    @Test
    public void testGetComplexPathNegativeIndex() throws IOException {
        ONode root = ONode.load(JSON);
        assertThrows(JsonPath.PathResolutionException.class, () -> {
            JsonPath.query(root, "$.store.book[-1].title");
        });
    }
}