package com.ebremer.halcyon.server;

import com.ebremer.halcyon.server.utils.HalcyonSettings;
import org.pac4j.core.authorization.authorizer.IsAnonymousAuthorizer;
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.matching.matcher.PathMatcher;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HalcyonConfigFactory implements ConfigFactory {
    
    @Autowired
    KeycloakOidcClient keycloakclient;  
    
    @Override
    public Config build(final Object... parameters) {
        keycloakclient.setProfileCreator(new HalcyonProfileCreator());
        final Clients clients = new Clients(HalcyonSettings.getSettings().getProxyHostName()+"/callback", keycloakclient);
        final Config config = new Config(clients);        
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer("custom", new CustomAuthorizer());
        config.addAuthorizer("mustBeAnon", new IsAnonymousAuthorizer("/?mustBeAnon"));
        config.addAuthorizer("mustBeAuth", new IsAuthenticatedAuthorizer("/?mustBeAuth"));
        config.addMatcher("excludedPath", new PathMatcher().excludeRegex("^/callback$"));
        return config;
    }
}
