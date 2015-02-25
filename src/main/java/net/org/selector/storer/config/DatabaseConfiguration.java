package net.org.selector.storer.config;


import com.mongodb.Mongo;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.inject.Inject;

@Configuration
@Profile("!cloud")
@EnableMongoRepositories("net.org.selector.storer.repository")
@Import(value = MongoAutoConfiguration.class)
public class DatabaseConfiguration extends AbstractMongoConfiguration {

    @Inject
    private Mongo mongo;

    @Inject
    private MongoProperties mongoProperties;

    @Override
    protected String getDatabaseName() {
        return mongoProperties.getDatabase();
    }

    @Override
    public Mongo mongo() throws Exception {
        return mongo;
    }

    @Bean
    public GridFsTemplate gridFsTemplate() throws Exception {
        return new GridFsTemplate(mongoDbFactory(), mongoTemplate().getConverter());
    }
}
