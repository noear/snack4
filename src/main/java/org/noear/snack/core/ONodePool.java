package org.noear.snack.core;

import org.noear.snack.ONode;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 对象池，用于ONode对象复用管理
 */
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

    /**
     * 从池中获取对象实例
     * @param value 初始值
     * @return 复用或新建的ONode实例
     */
    public ONode acquire(Object value) {
        ONode node = pool.poll();
        return (node != null) ? node.reset(value) : new ONode(value);
    }

    /**
     * 释放对象到池中
     * @param releasedNode 要释放的实例
     */
    public void release(ONode releasedNode) {
        if (pool.size() < maxSize) {
            pool.offer(releasedNode.clear());
        }
    }
}