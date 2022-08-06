package com.sap.cap.sdmservice.config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cap.sdmservice.model.SDMUaa;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cloud.security.client.HttpClientFactory;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.config.Service;
import com.sap.cloud.security.xsuaa.client.DefaultOAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.client.XsuaaDefaultEndpoints;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.UserTokenFlow;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;

@Component
public class SDMDataSource {

    private CdsRuntime cdsRuntime;
    private SDMUaa sdmUaa;
    private UserTokenFlow userTokenFlow;

    @Autowired
    public SDMDataSource(CdsRuntime cdsRuntime){
        this.cdsRuntime = cdsRuntime;
        createDataSource();
        setUserTokenFlow();
    }

    public Optional<String> getUserAccessToken(String token) {
        OAuth2TokenResponse oAuth2TokenResponse = null;
        try {
            oAuth2TokenResponse = this.userTokenFlow
            .token(token).execute();
        } catch (TokenFlowException ex) {
            ex.printStackTrace();
        }
        return Optional.ofNullable(oAuth2TokenResponse.getAccessToken());
    }

    public String getBrowerUrl(){
        return this.sdmUaa.getUri()+"browser";
    }

    private void createDataSource() {
        List<SDMUaa> sdmUaas = cdsRuntime.getEnvironment()
        .getServiceBindings()
        .filter(b -> b.matches("sdm", "sdm"))
        .map(b -> new SDMUaa(b.getCredentials()))
        .collect(Collectors.toList());
        this.sdmUaa = sdmUaas.stream().findFirst().orElse(null);
    }

    private void setUserTokenFlow(){

        OAuth2ServiceConfigurationBuilder builder = 
        OAuth2ServiceConfigurationBuilder.forService(Service.XSUAA);
        OAuth2ServiceConfiguration config = builder
            .withClientId(sdmUaa.getCredentials().getClientId())
            .withClientSecret(sdmUaa.getCredentials().getClientSecret())
            .withPrivateKey(sdmUaa.getCredentials().getKey())
            .withUrl(sdmUaa.getCredentials().getUrl())
            .build();

        XsuaaTokenFlows xsuaaTokenFlows = new XsuaaTokenFlows(
            new DefaultOAuth2TokenService(HttpClientFactory
            .create(config.getClientIdentity())), 
            new XsuaaDefaultEndpoints(config), 
            config.getClientIdentity());
        
        this.userTokenFlow= xsuaaTokenFlows.userTokenFlow();
    }
}
