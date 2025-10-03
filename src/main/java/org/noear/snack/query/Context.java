package org.noear.snack.query;

import org.noear.snack.ONode;

public class Context {
    public boolean flattened = false;
    public final ONode root;

    public Context(ONode root) {
        this.root = root;
    }
}