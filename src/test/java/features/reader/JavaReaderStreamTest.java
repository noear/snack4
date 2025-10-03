package features.reader;

import org.noear.snack4.ONode;
import org.noear.snack4.stream.JsonStreamHandler;
import org.noear.snack4.stream.JsonStreamParser;

import java.io.IOException;

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
