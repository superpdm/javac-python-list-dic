package com.sun.source.tree;

import java.util.List;

public interface NewListTree extends ExpressionTree {
    Tree getType();
    List<? extends ExpressionTree> getDimensions();
    List<? extends ExpressionTree> getInitializers();
}
