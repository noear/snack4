package features;

import org.noear.snack.ONode;
import org.noear.snack.schema.JsonPath;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonPathComplexTest {

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
    public void testRecursiveSearch() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..price");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(5, result.size());
    }

    @Test
    public void testWildcard() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[*].author");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(4, result.size());
    }

    @Test
    public void testComplexPath() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book[2].title");
        assertNotNull(result);
        assertEquals("Moby Dick", result.getString());
    }

    @Test
    public void testCountFunction() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$.store.book.count()");
        assertNotNull(result);
        assertEquals(4, result.getInt());
    }

    @Test
    public void testSumFunction() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..price.sum()");
        assertNotNull(result);
        assertEquals(73.87, result.getDouble(), 0.01);
    }

    @Test
    public void testRecursiveSearchWithFilter() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price > 10)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter2() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price < 10)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter3() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'fiction')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(3, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter4() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.author == 'J. R. R. Tolkien')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter5() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.title == 'Moby Dick')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter6() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price > 20)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter7() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price < 5)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(0, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter8() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'reference')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter9() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.author == 'Nigel Rees')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter10() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.title == 'Sayings of the Century')]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter11() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price > 15)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter12() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.price < 15)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter13() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'fiction' && @.price > 10)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter14() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'fiction' && @.price < 10)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter15() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'fiction' && @.price > 20)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    public void testRecursiveSearchWithFilter16() {
        ONode root = ONode.load(JSON);
        ONode result = JsonPath.query(root, "$..book[?(@.category == 'fiction' && @.price < 20)]");
        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }
}