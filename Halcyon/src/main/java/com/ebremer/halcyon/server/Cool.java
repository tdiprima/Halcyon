package com.ebremer.halcyon.server;

import com.ebremer.halcyon.server.utils.HalcyonSettings;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.jee.filter.CallbackFilter;
import org.pac4j.jee.filter.LogoutFilter;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 *
 * @author erich
 */

@Configuration
public class Cool {
    
    @Autowired
    private DefaultSslBundleRegistry defaultSslBundleRegistry;
    
    @Bean
    public HalcyonSecurityFilter securityFilter(Config pac4jConfig) {
        HalcyonSecurityFilter securityFilter = new HalcyonSecurityFilter();
        securityFilter.setConfig(pac4jConfig);
        securityFilter.setClients("KeycloakOidcClient");
        //securityFilter.setAuthorizers("authorizerName");
        return securityFilter;
    }
    
    @Bean
    public HalcyonSessionListener httpSessionListener() {
        return new HalcyonSessionListener();
    }
    
    @Bean
    public Config config() {
        final KeycloakOidcConfiguration keyconfig = new KeycloakOidcConfiguration();
        keyconfig.setClientId("account");
        keyconfig.setRealm("Halcyon");
        keyconfig.setConnectTimeout(10000);
        keyconfig.setReadTimeout(10000);    
        keyconfig.setBaseUri(HalcyonSettings.getSettings().getAuthServer()+"/auth");
        if (HalcyonSettings.getSettings().isHTTPS2enabled()) {
            keyconfig.setSslSocketFactory(defaultSslBundleRegistry.getBundle("server").createSslContext().getSocketFactory());
        }
        KeycloakOidcClient keycloakclient = new KeycloakOidcClient(keyconfig);
        System.out.println("HACK : "+keycloakclient);
        final Clients clients = new Clients(HalcyonSettings.getSettings().getProxyHostName()+"/callback", keycloakclient);
        return new Config(clients);
    }    

    @Bean
    public FilterRegistrationBean<CallbackFilter> callbackFilterRegistration(CallbackFilter callbackFilter) {
        FilterRegistrationBean<CallbackFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(callbackFilter);
        registration.addUrlPatterns("/callback");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean logoutFilter() {
        final LogoutFilter filter = new LogoutFilter(config(), "/?defaulturlafterlogout");
        filter.setDestroySession(true);
        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/skunkworks/logout");
        return registrationBean;
    }
    
    @Bean
    public CallbackFilter callbackFilter(Config pac4jConfig) {
        CallbackFilter callbackFilter = new CallbackFilter();
        callbackFilter.setConfig(pac4jConfig);
        callbackFilter.setDefaultUrl("/");
        return callbackFilter;
    }
    
    @Bean
    public FilterRegistrationBean<HalcyonSecurityFilter> securityFilterRegistration(HalcyonSecurityFilter securityFilter) {
        FilterRegistrationBean<HalcyonSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(securityFilter);
        registration.setName("KeycloakOidcClient");
        registration.addUrlPatterns(
                "/ldp*",
                "/blank",
                "/skunkworks/yay",
                "/f*",
                "/callback",
                "/about",
                "/iiif*/",
                "/sparql",
                "/revisionhistory",
                "/collections"
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // Order just after CallbackFilter
        return registration;
    }
}
