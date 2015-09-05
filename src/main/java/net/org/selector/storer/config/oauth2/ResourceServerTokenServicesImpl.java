package net.org.selector.storer.config.oauth2;

import com.google.common.collect.ImmutableMap;
import com.mongodb.DBObject;
import net.org.selector.storer.service.util.RequestInfoUtil;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.io.Serializable;

/**
 * Created by Selector on 05.09.2015.
 */
public class ResourceServerTokenServicesImpl implements ResourceServerTokenServices {
    private ImmutableMap<String, MongoTemplate> mongoTemplateRegister;
    private OAuth2AuthenticationReadConverter authenticationReadConverter = new OAuth2AuthenticationReadConverter();

    public ResourceServerTokenServicesImpl(ImmutableMap<String, MongoTemplate> mongoTemplateRegister) {
        this.mongoTemplateRegister = mongoTemplateRegister;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String tokenId) throws AuthenticationException, InvalidTokenException {
        OAuth2AuthenticationAccessToken defaultOAuth2AccessTokens = findByTokenId(tokenId, "authentication");
        return authenticationReadConverter.convert(defaultOAuth2AccessTokens.authentication);
    }

    private OAuth2AuthenticationAccessToken findByTokenId(String tokenId, String field) {
        Query q = new Query();
        q.addCriteria(Criteria.where("tokenId").is(tokenId));
        q.fields().include(field);
        String siteName = RequestInfoUtil.getSiteName();
        return mongoTemplateRegister
            .get(siteName)
            .findOne(q, OAuth2AuthenticationAccessToken.class);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        OAuth2AuthenticationAccessToken token = findByTokenId(tokenValue, "oAuth2AccessToken");
        if (token == null) {
            return null;
        }
        return token.oAuth2AccessToken;
    }

    @Document(collection = "OAUTH_AUTHENTICATION_ACCESS_TOKEN")
    public static class OAuth2AuthenticationAccessToken implements Serializable {
        public String id;
        public String tokenId;
        public DefaultOAuth2AccessToken oAuth2AccessToken;
        public String authenticationId;
        public String userName;
        public String clientId;
        public DBObject authentication;
        public String refreshToken;
    }
}
