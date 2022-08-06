package com.sap.cap.sdmservice.config;

import java.util.HashMap;

import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cds.services.EventContext;
import com.sap.cds.services.authentication.JwtTokenAuthenticationInfo;

@Component
public class ConnectSDMDataSource {

    private HashMap<String, String> parameters;
    private SDMDataSource sdmDataSource;

    @Autowired
    public ConnectSDMDataSource(SDMDataSource sdmDataSource){
        this.sdmDataSource = sdmDataSource;
        this.parameters =  new HashMap<String, String>();
        parameters.put(SessionParameter.BINDING_TYPE, 
        BindingType.BROWSER.value());
        parameters.put(SessionParameter.AUTH_HTTP_BASIC, "false");
        parameters.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN, "false");
        parameters.put(SessionParameter.AUTH_OAUTH_BEARER, "true");
    }
    
    public ConnectSDMDataSource setParameter(String parameter, String value){
        this.parameters.put(parameter, value);
        return this;
    }

    public Session getSession(EventContext context,String repositoryId){

        JwtTokenAuthenticationInfo jwtTokenAuthenticationInfo =
        context.getAuthenticationInfo().as(JwtTokenAuthenticationInfo.class);
        String token = sdmDataSource.getUserAccessToken(
            jwtTokenAuthenticationInfo.getToken()).get();
  
        this.setParameter(SessionParameter.BROWSER_URL, 
        sdmDataSource.getBrowerUrl())
        .setParameter(SessionParameter.OAUTH_ACCESS_TOKEN, token)
		.setParameter(SessionParameter.REPOSITORY_ID, repositoryId);
        
        SessionFactory factory = SessionFactoryImpl.newInstance();
        return factory.createSession(parameters);
    }
}
