/*
 * Copyright (C) 2019 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.license;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LicenseVerifyParam {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("public_alias")
    private String publicAlias;

    @JsonProperty("store_password")
    private String storePassword;

    @JsonProperty("publickey_path")
    private String publicKeyPath;

    @JsonProperty("license_path")
    private String licensePath;

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPublicAlias() {
        return this.publicAlias;
    }

    public void setPublicAlias(String publicAlias) {
        this.publicAlias = publicAlias;
    }

    public String getStorePassword() {
        return this.storePassword;
    }

    public void setStorePassword(String storePassword) {
        this.storePassword = storePassword;
    }

    public String getLicensePath() {
        return this.licensePath;
    }

    public void setLicensePath(String licensePath) {
        this.licensePath = licensePath;
    }

    public String getPublicKeyPath() {
        return this.publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
}

