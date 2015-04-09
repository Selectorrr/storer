package net.org.selector.storer.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.joda.time.DateTime;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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

    @Inject
    private RestTemplate restTemplate;

    private RelaxedPropertyResolver propertyResolver;
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public UserDetails loadUserByUsername(String login) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String site = "petsroom";
        login = "user";
        String result = mongoTemplateRegister.get(site).getDb().getCollection("T_USER").findOne(new BasicDBObject("login", login)).toString();
        try {
            UserDTO userDetailsDTO = mapper.readValue(result, UserDTO.class);
            List<GrantedAuthority> grantedAuthorities = userDetailsDTO.authorities.stream()
                .map(authority -> new SimpleGrantedAuthority(authority.name))
                .collect(Collectors.toList());
            User user = new User(userDetailsDTO.login, userDetailsDTO.password, grantedAuthorities);
//            ResponseEntity<String> test = restTemplate.getForEntity(propertyResolver.getProperty("userDetailsUrl") + login, String.class);
//            ResponseEntity<UserDetailsDTO> forEntity = restTemplate.getForEntity(propertyResolver.getProperty("userDetailsUrl") + login, UserDetailsDTO.class);
            return user;
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

        @JsonProperty("_id")
        private String id;
        private String login;
        private String password;
        private String first_name;
        private String last_name;
        private String email;
        private String avatar;
        private boolean activated = false;
        private String lang_key;
        private String activation_key;
        private String third_party_token;
        private Set<Authority> authorities = new HashSet<>();
        private String created_by;
        private DateTime created_date = DateTime.now();
        private String last_modified_by;
        private DateTime last_modified_date = DateTime.now();

        public static class Authority implements Serializable {
            public String name;
        }
    }


    @PostConstruct
    private void init() {
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

}
