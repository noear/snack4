package org.noear.snack.core;

import org.noear.snack.ONode;
import org.noear.snack.exception.PathResolutionException;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;

/**
 * @author noear 2025/3/17 created
 */
public class JsonPathFuns {

    public static ONode sum(ONode node) {
        if (node.isArray()) {
            double sum = node.getArray().stream()
                    .filter(ONode::isNumber)
                    .mapToDouble(ONode::getDouble)
                    .sum();
            return new ONode(sum);
        } else {
            throw new PathResolutionException("sum() requires an array");
        }
    }


    public static ONode min(ONode node) {
        if (node.isArray()) {
            OptionalDouble min = node.getArray().stream()
                    .filter(ONode::isNumber)
                    .mapToDouble(ONode::getDouble)
                    .min();

            return min.isPresent() ? new ONode(min.getAsDouble()) : new ONode(null);
        } else if (node.isNumber()) {
            return node;
        } else {
            throw new PathResolutionException("min() requires array or number");
        }
    }

    public static ONode max(ONode node) {
        if (node.isArray()) {
            OptionalDouble max = node.getArray().stream()
                    .filter(ONode::isNumber)
                    .mapToDouble(ONode::getDouble)
                    .max();

            return max.isPresent() ? new ONode(max.getAsDouble()) : new ONode(null);
        } else if (node.isNumber()) {
            return node;
        } else {
            throw new PathResolutionException("max() requires array or number");
        }
    }

    public static ONode avg(ONode node) {
        if (!node.isArray()) {
            throw new PathResolutionException("avg() requires an array");
        }

        DoubleSummaryStatistics stats = node.getArray().stream()
                .filter(ONode::isNumber)
                .mapToDouble(ONode::getDouble)
                .summaryStatistics();

        return stats.getCount() > 0 ?
                new ONode(stats.getAverage()) :
                new ONode(null);
    }

    public static ONode first(ONode node) {
        if (node.isArray()) {
            return node.get(0);
        } else {
            throw new PathResolutionException("first() requires array");
        }
    }

    public static ONode last(ONode node) {
        if (node.isArray()) {
            return node.get(-1);
        } else {
            throw new PathResolutionException("last() requires array");
        }
    }

    public static ONode keys(ONode node) {
        if (node.isObject()) {
            return ONode.loadBean(node.getObject().keySet());
        } else {
            throw new PathResolutionException("keys() requires object");
        }
    }

    public static ONode size(ONode node) {
        return new ONode(node.size());
    }

    /* 字符串函数实现 */
    public static ONode length(ONode node) {
        if (node.isString()) {
            return new ONode(node.getString().length());
        } else if (node.isArray()) {
            return new ONode(node.size());
        } else if (node.isObject()) {
            return new ONode(node.getObject().size());
        }
        return new ONode(0);
    }


    public static ONode upper(ONode node) {
        return node.isString() ?
                new ONode(node.getString().toUpperCase()) :
                node;
    }

    public static ONode lower(ONode node) {
        return node.isString() ?
                new ONode(node.getString().toLowerCase()) :
                node;
    }

    public static ONode trim(ONode node) {
        return node.isString() ?
                new ONode(node.getString().trim()) :
                node;
    }
}