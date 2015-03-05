package net.org.selector.storer.service;

import com.mongodb.gridfs.GridFSDBFile;
import org.joda.time.DateTime;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.List;

/**
 * SLitvinov on 25.02.2015.
 */
@Service
public class FileService implements EnvironmentAware {
    @Inject
    private GridFsTemplate gridFsTemplate;
    private static final String ENV_FILES = "files.";
    private RelaxedPropertyResolver propertyResolver;


    public void save(String filename, InputStream inputStream, String contentType, List<String> result) {
        Query byName = new Query().addCriteria(Criteria.where("filename").is(filename));
        gridFsTemplate.delete(byName);
        gridFsTemplate.store(inputStream, filename, contentType);
        result.add(filename);
    }

    public void delete(String s) {
        Query filename = new Query().addCriteria(Criteria.where("filename").is(s));
        gridFsTemplate.delete(filename);
    }


    public GridFSDBFile findOneByName(String filename) {
        Query criteria = new Query().addCriteria(Criteria.where("filename").is(filename));
        return gridFsTemplate.findOne(criteria);
    }

    public void actualize(List<String> names) {
        Integer tmpTimeout = propertyResolver.getProperty("tmpTimeout", Integer.class, 3600000);
        gridFsTemplate.delete(new Query()
                .addCriteria(Criteria.where("filename").nin(names))
                .addCriteria(Criteria.where("uploadDate").lt(new DateTime().minusMillis(tmpTimeout).toDate()))
        );
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, ENV_FILES);
    }
}
