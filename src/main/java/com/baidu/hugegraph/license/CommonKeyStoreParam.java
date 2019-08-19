/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.schlichtherle.license.AbstractKeyStoreParam;

/**
 * Custom KeyStoreParam to store public and private key storage files to
 * other disk locations instead of projects
 */
public class CommonKeyStoreParam extends AbstractKeyStoreParam {

    private String storePath;
    private String alias;
    private String keyPwd;
    private String storePwd;

    public CommonKeyStoreParam(Class clazz, String resource, String alias,
                               String storePwd, String keyPwd) {
        super(clazz, resource);
        this.storePath = resource;
        this.alias = alias;
        this.storePwd = storePwd;
        this.keyPwd = keyPwd;
    }

    @Override
    public String getAlias() {
        return this.alias;
    }

    @Override
    public String getStorePwd() {
        return this.storePwd;
    }

    @Override
    public String getKeyPwd() {
        return this.keyPwd;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FileInputStream(new File(this.storePath));
    }
}
