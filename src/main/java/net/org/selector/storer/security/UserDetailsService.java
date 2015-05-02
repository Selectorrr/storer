package net.org.selector.storer.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import net.org.selector.storer.service.util.RequestInfoUtil;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService, EnvironmentAware {
    private static final String ENV_SPRING_SITE = "site.";
    @Inject
    private ImmutableMap<String, MongoTemplate> mongoTemplateRegister;

    private RelaxedPropertyResolver propertyResolver;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public UserDetails loadUserByUsername(String login) {
        String siteName = RequestInfoUtil.getSiteName();
        String result = mongoTemplateRegister.get(siteName).getDb().getCollection("T_USER")
            .findOne(new BasicDBObject("login", login)).toString();
        try {
            UserDTO userDetailsDTO = mapper.readValue(result, UserDTO.class);
            List<GrantedAuthority> grantedAuthorities = userDetailsDTO.authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.name))
                .collect(Collectors.toList());
            return new User(userDetailsDTO.login, userDetailsDTO.password, grantedAuthorities);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, ENV_SPRING_SITE);
    }

    public static class UserDTO implements Serializable {
        public UserDTO() {
        }

        public String login;
        public String password;
        public Set<Authority> authorities = new HashSet<>();

        public static class Authority implements Serializable {
            @JsonProperty("_id")
            public String name;
        }
    }


    @PostConstruct
    private void init() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

}
