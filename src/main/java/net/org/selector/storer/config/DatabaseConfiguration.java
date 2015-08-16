package net.org.selector.storer.config;


import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.mongodb.Mongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;

@Configuration
public class DatabaseConfiguration implements EnvironmentAware {
    private ImmutableSet<String> siteNames;
    private RelaxedPropertyResolver propertyResolver;
    private Set<AbstractMongoConfiguration> configurations = Sets.newHashSet();
    private final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);
    private ImmutableMap<String, GridFsTemplate> gridFsTemplateRegister;
    private ImmutableMap<String, MongoTemplate> mongoTemplateRegister;


    @Bean
    public ImmutableMap<String, GridFsTemplate> gridFsTemplateRegister() {
        return gridFsTemplateRegister;
    }

    @Bean
    public ImmutableMap<String, MongoTemplate> mongoTemplateRegister() {
        return mongoTemplateRegister;
    }

    @Bean
    public ImmutableSet<String> siteNames() {
        return siteNames;
    }

    private MongoProperties getMongoProperties(String dbPrefix) {
        MongoProperties mongoProperties = new MongoProperties();
        mongoProperties.setHost(propertyResolver.getProperty(dbPrefix + "host"));
        mongoProperties.setPort(propertyResolver.getProperty(dbPrefix + "port", Integer.class));
        mongoProperties.setDatabase(propertyResolver.getProperty(dbPrefix + "database"));
        mongoProperties.setAuthenticationDatabase(propertyResolver.getProperty(dbPrefix + "authenticationDatabase"));
        mongoProperties.setGridFsDatabase(propertyResolver.getProperty(dbPrefix + "gridFsDatabase"));
        mongoProperties.setPassword(propertyResolver.getProperty(dbPrefix + "password", String.class).toCharArray());
        mongoProperties.setUsername(propertyResolver.getProperty(dbPrefix + "username"));
        mongoProperties.setUri(propertyResolver.getProperty(dbPrefix + "uri"));
        return mongoProperties;
    }

    @PostConstruct
    private void init() throws Exception {
        Map<String, GridFsTemplate> gridFsTemplateRegister = Maps.newHashMap();
        Map<String, MongoTemplate> mongoTemplateRegister = Maps.newHashMap();
        for (String siteName : siteNames) {
            String dbPrefix = "sites." + siteName + ".db.";
            MongoProperties mongoProperties = getMongoProperties(dbPrefix);
            Mongo mongo = mongoProperties.createMongoClient(null);
            AbstractMongoConfiguration configuration = new AbstractMongoConfiguration() {
                @Override
                protected String getDatabaseName() {
                    return propertyResolver.getProperty(dbPrefix + "database");
                }

                @Override
                public Mongo mongo() throws Exception {
                    return mongo;
                }
            };
            mongoTemplateRegister.put(siteName, configuration.mongoTemplate());
            gridFsTemplateRegister.put(siteName, new GridFsTemplate(configuration.mongoDbFactory(), configuration.mongoTemplate().getConverter()));
        }
        DatabaseConfiguration.this.gridFsTemplateRegister = ImmutableMap.copyOf(gridFsTemplateRegister);
        DatabaseConfiguration.this.mongoTemplateRegister = ImmutableMap.copyOf(mongoTemplateRegister);
    }

    @PreDestroy
    public void close() {
        for (AbstractMongoConfiguration configuration : configurations) {
            try {
                Mongo mongo = configuration.mongo();
                if (mongo != null) {
                    mongo.close();
                }
            } catch (Exception e) {
                log.error("Error when close db connecion", e);
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        propertyResolver = new RelaxedPropertyResolver(environment);
        Map<String, Object> sites = propertyResolver.getSubProperties("sites");
        Set<String> result = Sets.newHashSet();
        for (Map.Entry<String, Object> entry : sites.entrySet()) {
            Iterable<String> split = Splitter.on('.').split(entry.getKey());
            result.add(Iterables.get(split, 1));
        }
        siteNames = ImmutableSet.copyOf(result);
    }
}
