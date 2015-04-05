package net.org.selector.storer.security;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.Set;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService, EnvironmentAware {
    private static final String ENV_SPRING_SITE = "site.";
    @Inject
    private RestTemplate restTemplate;
    private RelaxedPropertyResolver propertyResolver;


    @Override
    public UserDetails loadUserByUsername(final String login) {
        ResponseEntity<String> test = restTemplate.getForEntity(propertyResolver.getProperty("userDetailsUrl") + login, String.class);
        ResponseEntity<UserDetailsDTO> forEntity = restTemplate.getForEntity(propertyResolver.getProperty("userDetailsUrl") + login, UserDetailsDTO.class);
        return forEntity.getBody().toUserDetails();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, ENV_SPRING_SITE);
    }

    private static class UserDetailsDTO {
        public String password;
        public String username;
        public Set<GrantedAuthorityDTO> authorities;
        public boolean accountNonExpired;
        public boolean accountNonLocked;
        public boolean credentialsNonExpired;
        public boolean enabled;


        public UserDetails toUserDetails() {
            return new User(this.username, this.password, this.enabled, this.accountNonExpired, this.credentialsNonExpired,
                this.accountNonLocked, this.authorities);
        }

        private static class GrantedAuthorityDTO implements GrantedAuthority {
            public String authority;

            @Override
            public String getAuthority() {
                return authority;
            }
        }
    }

}
