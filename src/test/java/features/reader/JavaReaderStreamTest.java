package features.reader;

import org.noear.snack.ONode;
import org.noear.snack.core.JsonReader;
import org.noear.snack.stream.JsonStreamHandler;
import org.noear.snack.stream.JsonStreamParser;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author noear 2025/5/10 created
 */
public class JavaReaderStreamTest {
    public void case1() throws Exception {
        new JsonStreamParser("{}").parse(new JsonStreamHandler() {
            @Override
            public void startObject() throws IOException {

            }

            @Override
            public void endObject() throws IOException {

            }

            @Override
            public void startArray() throws IOException {

            }

            @Override
            public void endArray() throws IOException {

            }

            @Override
            public void key(String key) throws IOException {

            }

            @Override
            public void value(ONode value) throws IOException {

            }
        });
    }
}
