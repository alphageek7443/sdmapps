package com.sap.cap.sdmservice.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sap.cloud.security.config.CredentialType;
import com.sap.cloud.security.xsuaa.XsuaaCredentials;

public class SDMUaa {
    private final String uri;
    private final XsuaaCredentials credentials;

    public SDMUaa(Map<String, Object> credentials){
        @SuppressWarnings (value="unchecked")
        Map<String, String> uaa = (LinkedHashMap<String, String>) credentials.get("uaa");
        this.uri =(String) credentials.get("uri");
        this.credentials = new XsuaaCredentials();
        this.credentials.setClientId(uaa.get("clientid"));
        this.credentials.setClientSecret(uaa.get("clientsecret"));
        this.credentials.setUrl(uaa.get("url"));
        this.credentials.setCredentialType(CredentialType.from(uaa.get("credential-type")));
    }

    public String getUri() {
        return this.uri;
    }

    public XsuaaCredentials getCredentials() {
        return this.credentials;
    }
}
