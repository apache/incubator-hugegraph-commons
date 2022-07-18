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

package com.baidu.hugegraph.unit;

import com.baidu.hugegraph.testutil.AssertTest;
import com.baidu.hugegraph.testutil.WhiteboxTest;
import com.baidu.hugegraph.unit.concurrent.*;
import com.baidu.hugegraph.unit.config.HugeConfigTest;
import com.baidu.hugegraph.unit.config.OptionSpaceTest;
import com.baidu.hugegraph.unit.date.SafeDateFormatTest;
import com.baidu.hugegraph.unit.event.EventHubTest;
import com.baidu.hugegraph.unit.iterator.*;
import com.baidu.hugegraph.unit.license.*;
import com.baidu.hugegraph.unit.perf.PerfUtilTest;
import com.baidu.hugegraph.unit.perf.StopwatchTest;
import com.baidu.hugegraph.unit.rest.RestClientTest;
import com.baidu.hugegraph.unit.rest.RestResultTest;
import com.baidu.hugegraph.unit.util.*;
import com.baidu.hugegraph.unit.version.VersionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    LockManagerTest.class,
    LockGroupTest.class,
    AtomicLockTest.class,
    KeyLockTest.class,
    RowLockTest.class,
    PausableScheduledThreadPoolTest.class,

    HugeConfigTest.class,
    OptionSpaceTest.class,
    SafeDateFormatTest.class,
    BarrierEventTest.class,
    EventHubTest.class,
    PerfUtilTest.class,
    StopwatchTest.class,
    RestClientTest.class,
    RestResultTest.class,
    VersionTest.class,

    ExtendableIteratorTest.class,
    FilterIteratorTest.class,
    LimitIteratorTest.class,
    MapperIteratorTest.class,
    FlatMapperIteratorTest.class,
    FlatMapperFilterIteratorTest.class,
    ListIteratorTest.class,
    BatchMapperIteratorTest.class,

    BytesTest.class,
    CollectionUtilTest.class,
    EcheckTest.class,
    HashUtilTest.class,
    InsertionOrderUtilTest.class,
    LogTest.class,
    NumericUtilTest.class,
    ReflectionUtilTest.class,
    StringUtilTest.class,
    TimeUtilTest.class,
    VersionUtilTest.class,
    LongEncodingTest.class,
    OrderLimitMapTest.class,
    DateUtilTest.class,
    UnitUtilTest.class,

    LicenseExtraParamTest.class,
    LicenseCreateParamTest.class,
    LicenseInstallParamTest.class,
    LicenseParamsTest.class,
    MachineInfoTest.class,

    AssertTest.class,
    WhiteboxTest.class
})
public class UnitTestSuite {
}
