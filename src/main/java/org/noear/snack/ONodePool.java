package org.noear.snack;

import java.util.LinkedList;
import java.util.Queue;

// 内存池实现（简化版）
public class ONodePool {
    private static final int DEFAULT_MAX_POOL_SIZE = 100;
    private final Queue<ONode> pool = new LinkedList<>();
    private final int maxSize;

    public ONodePool() {
        this(DEFAULT_MAX_POOL_SIZE);
    }

    public ONodePool(int maxSize) {
        this.maxSize = maxSize;
    }

    public ONode acquire(Object value) {
        ONode node = pool.poll();
        return (node != null) ? node.reset(value) : new ONode(value);
    }

    public void release(ONode node) {
        if (pool.size() < maxSize) {
            pool.offer(node.clear());
        }
    }
}