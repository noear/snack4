package org.noear.snack4.codec.factory;

import org.noear.snack4.Options;
import org.noear.snack4.codec.ObjectFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author noear 2025/10/3 created
 *
 */
public class SetFactory implements ObjectFactory<Set> {
    private static final SetFactory instance = new SetFactory();

    public static SetFactory getInstance() {
        return instance;
    }

    @Override
    public Set create(Options opts, Class<Set> clazz) {
        return new HashSet();
    }
}
