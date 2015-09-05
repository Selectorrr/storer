package net.org.selector.storer;


import com.google.common.collect.ImmutableMap;
import net.org.selector.storer.config.oauth2.ResourceServerTokenServicesImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import javax.inject.Inject;

@Configuration
public class OAuth2ServerConfiguration {

    @Inject
    private ImmutableMap<String, MongoTemplate> mongoTemplateRegister;

    @Bean
    public ResourceServerTokenServices tokenServices() {
        return new ResourceServerTokenServicesImpl(mongoTemplateRegister);
    }


    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/files/**").permitAll()
                .antMatchers(HttpMethod.POST, "/files/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/files/**").authenticated()
                .antMatchers("/api/files/**").authenticated();

        }
    }
}
