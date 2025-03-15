package features;

import org.junit.jupiter.api.Test;
import org.noear.snack.JsonBeanCodec;
import org.noear.snack.ONode;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class JsonBeanCodecTest {

    // 测试类定义
    static class User {
        private String name;
        private int age;
        private boolean isAdmin;
        private List<String> hobbies;
        private Map<String, String> metadata;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public boolean isAdmin() { return isAdmin; }
        public void setAdmin(boolean admin) { isAdmin = admin; }
        public List<String> getHobbies() { return hobbies; }
        public void setHobbies(List<String> hobbies) { this.hobbies = hobbies; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    // 测试用例
    @Test
    public void testBasicSerialization() {
        User user = new User();
        user.setName("Alice");
        user.setAge(30);
        user.setAdmin(true);

        ONode node = JsonBeanCodec.toNode(user);
        assertEquals("Alice", node.get("name").getString());
        assertEquals(30, node.get("age").getInt());
        assertTrue(node.get("isAdmin").getBoolean());
    }

    @Test
    public void testBasicDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Bob"));
        node.set("age", new ONode(25));
        node.set("isAdmin", new ONode(false));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Bob", user.getName());
        assertEquals(25, user.getAge());
        assertFalse(user.isAdmin());
    }

    @Test
    public void testListSerialization() {
        User user = new User();
        user.setHobbies(Arrays.asList("Reading", "Swimming", "Coding"));

        ONode node = JsonBeanCodec.toNode(user);
        List<String> hobbies = node.get("hobbies").getArray().stream()
                .map(ONode::getString)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("Reading", "Swimming", "Coding"), hobbies);
    }

    @Test
    public void testListDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("hobbies", new ONode(Arrays.asList("Gaming", "Music")));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals(Arrays.asList("Gaming", "Music"), user.getHobbies());
    }

    @Test
    public void testMapSerialization() {
        User user = new User();
        user.setMetadata(new HashMap<>());
        user.getMetadata().put("key1", "value1");
        user.getMetadata().put("key2", "value2");

        ONode node = JsonBeanCodec.toNode(user);
        Map<String, String> metadata = node.get("metadata").getObject().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getString()));
        assertEquals("value1", metadata.get("key1"));
        assertEquals("value2", metadata.get("key2"));
    }

    @Test
    public void testMapDeserialization() {
        ONode node = new ONode(new HashMap<>());
        ONode metadataNode = new ONode(new HashMap<>());
        metadataNode.set("key1", new ONode("value1"));
        metadataNode.set("key2", new ONode("value2"));
        node.set("metadata", metadataNode);

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("value1", user.getMetadata().get("key1"));
        assertEquals("value2", user.getMetadata().get("key2"));
    }

    @Test
    public void testNullValueSerialization() {
        User user = new User();
        user.setName(null);

        ONode node = JsonBeanCodec.toNode(user);
        assertTrue(node.get("name").isNull());
    }

    @Test
    public void testNullValueDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode(null));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertNull(user.getName());
    }

    @Test
    public void testEmptyListSerialization() {
        User user = new User();
        user.setHobbies(new ArrayList<>());

        ONode node = JsonBeanCodec.toNode(user);
        assertTrue(node.get("hobbies").isArray());
        assertEquals(0, node.get("hobbies").getArray().size());
    }

    @Test
    public void testEmptyListDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("hobbies", new ONode(new ArrayList<>()));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertTrue(user.getHobbies().isEmpty());
    }

    @Test
    public void testEmptyMapSerialization() {
        User user = new User();
        user.setMetadata(new HashMap<>());

        ONode node = JsonBeanCodec.toNode(user);
        assertTrue(node.get("metadata").isObject());
        assertEquals(0, node.get("metadata").getObject().size());
    }

    @Test
    public void testEmptyMapDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("metadata", new ONode(new HashMap<>()));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertTrue(user.getMetadata().isEmpty());
    }

    @Test
    public void testMissingFieldDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Charlie"));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Charlie", user.getName());
        assertEquals(0, user.getAge()); // Default value for int
        assertFalse(user.isAdmin()); // Default value for boolean
    }

    @Test
    public void testNestedObjectSerialization() {
        User user = new User();
        user.setName("Dave");
        user.setMetadata(new HashMap<>());
        user.getMetadata().put("nestedKey", "nestedValue");

        ONode node = JsonBeanCodec.toNode(user);
        assertEquals("nestedValue", node.get("metadata").get("nestedKey").getString());
    }

    @Test
    public void testNestedObjectDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Eve"));
        ONode metadataNode = new ONode(new HashMap<>());
        metadataNode.set("nestedKey", new ONode("nestedValue"));
        node.set("metadata", metadataNode);

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("nestedValue", user.getMetadata().get("nestedKey"));
    }

    @Test
    public void testInvalidTypeDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("age", new ONode("not a number"));

        assertThrows(RuntimeException.class, () -> JsonBeanCodec.toBean(node, User.class));
    }

    @Test
    public void testNullInputSerialization() {
        assertThrows(RuntimeException.class, () -> JsonBeanCodec.toNode(null));
    }

    @Test
    public void testNullInputDeserialization() {
        assertThrows(RuntimeException.class, () -> JsonBeanCodec.toBean(null, User.class));
    }

    @Test
    public void testInvalidClassDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Frank"));

        assertThrows(RuntimeException.class, () -> JsonBeanCodec.toBean(node, String.class));
    }

    @Test
    public void testComplexObjectSerialization() {
        User user = new User();
        user.setName("Grace");
        user.setAge(40);
        user.setAdmin(true);
        user.setHobbies(Arrays.asList("Traveling", "Photography"));
        user.setMetadata(new HashMap<>());
        user.getMetadata().put("key", "value");

        ONode node = JsonBeanCodec.toNode(user);
        assertEquals("Grace", node.get("name").getString());
        assertEquals(40, node.get("age").getInt());
        assertTrue(node.get("isAdmin").getBoolean());
        assertEquals(Arrays.asList("Traveling", "Photography"), node.get("hobbies").getArray().stream()
                .map(ONode::getString)
                .collect(Collectors.toList()));
        assertEquals("value", node.get("metadata").get("key").getString());
    }

    @Test
    public void testComplexObjectDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Hank"));
        node.set("age", new ONode(50));
        node.set("isAdmin", new ONode(false));
        node.set("hobbies", new ONode(Arrays.asList("Cooking", "Gardening")));
        ONode metadataNode = new ONode(new HashMap<>());
        metadataNode.set("key", new ONode("value"));
        node.set("metadata", metadataNode);

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Hank", user.getName());
        assertEquals(50, user.getAge());
        assertFalse(user.isAdmin());
        assertEquals(Arrays.asList("Cooking", "Gardening"), user.getHobbies());
        assertEquals("value", user.getMetadata().get("key"));
    }

    @Test
    public void testNonPublicFields() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Ivy"));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Ivy", user.getName());
    }

    @Test
    public void testCustomFieldNames() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Jack"));
        node.set("isAdmin", new ONode(true));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Jack", user.getName());
        assertTrue(user.isAdmin());
    }

    @Test
    public void testEmptyStringDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode(""));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("", user.getName());
    }

    @Test
    public void testSpecialCharactersInString() {
        ONode node = new ONode(new HashMap<>());
        node.set("name", new ONode("Special\"Characters\\\n\r\t"));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals("Special\"Characters\\\n\r\t", user.getName());
    }

    @Test
    public void testLargeNumberSerialization() {
        User user = new User();
        user.setAge(Integer.MAX_VALUE);

        ONode node = JsonBeanCodec.toNode(user);
        assertEquals(Integer.MAX_VALUE, node.get("age").getInt());
    }

    @Test
    public void testLargeNumberDeserialization() {
        ONode node = new ONode(new HashMap<>());
        node.set("age", new ONode(Integer.MAX_VALUE));

        User user = JsonBeanCodec.toBean(node, User.class);
        assertEquals(Integer.MAX_VALUE, user.getAge());
    }
}