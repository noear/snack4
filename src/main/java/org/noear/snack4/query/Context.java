package org.noear.snack4.query;

import org.noear.snack4.ONode;

public class Context {
    public boolean flattened = false;
    public final ONode root;

    public Context(ONode root) {
        this.root = root;
    }
}