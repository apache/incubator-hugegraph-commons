/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.iterator;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;

public class ToListIterator <V> extends WrappedIterator<V> {

    private final Iterator<V> origin;
    private final Iterator<V> iterator;

    public ToListIterator(Iterator<V> origin) {
        this.origin = origin;
        @SuppressWarnings("unchecked")
        List<V> results = IteratorUtils.toList(origin);
        this.iterator = results.iterator();
    }

    @Override
    public void remove() {
        this.iterator.remove();
    }

    @Override
    protected boolean fetch() {
        assert this.current == none();
        if (!this.iterator.hasNext()) {
            return false;
        }
        this.current = this.iterator.next();
        return true;
    }

    @Override
    protected Iterator<V> originIterator() {
        return this.origin;
    }
}
