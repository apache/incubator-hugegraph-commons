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

package com.baidu.hugegraph.date;

import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * The SafeDateFormat actually is a proxy for joda DateTimeFormatter
 */
@Deprecated
public class SafeDateFormat {

    private final String pattern;
    private final DateTimeFormatter formatter;

    public SafeDateFormat(String pattern) {
        this.pattern = pattern;
        this.formatter = DateTimeFormat.forPattern(pattern);
    }

    public Date parse(String source) {
        return this.formatter.parseDateTime(source).toDate();
    }

    public String format(Date date) {
        return this.formatter.print(date.getTime());
    }

    public void setLenient(boolean lenient) {
        // pass
    }

    public void setTimeZone(TimeZone timeZone) {
        this.formatter.withZone(DateTimeZone.forTimeZone(timeZone));
    }

    public Object toPattern() {
        return this.pattern;
    }

    @Override
    public Object clone() {
        // No need to clone due to itself is thread safe
        return this;
    }
}
