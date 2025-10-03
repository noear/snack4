package org.noear.snack4.codec.factory;

import org.noear.snack4.Options;
import org.noear.snack4.codec.ObjectFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class ListFactory implements ObjectFactory<List> {
    private static final ListFactory instance = new ListFactory();

    public static ListFactory getInstance() {
        return instance;
    }

    @Override
    public List create(Options opts, Class<List> clazz) {
        return new ArrayList();
    }
}
