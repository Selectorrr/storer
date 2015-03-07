package net.org.selector.storer.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import org.joda.time.DateTime;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private Integer tmpTimeout;


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

    public void delete(List<String> s) {
        Query filename = new Query().addCriteria(Criteria.where("filename").in(s));
        gridFsTemplate.delete(filename);
    }


    public GridFSDBFile findOneByName(String filename) {
        Query criteria = new Query().addCriteria(Criteria.where("filename").is(filename));
        return gridFsTemplate.findOne(criteria);
    }

    public void actualize(List<String> names) {
        gridFsTemplate.delete(new Query()
                .addCriteria(Criteria.where("filename").nin(names))
                .addCriteria(Criteria.where("uploadDate").lt(new DateTime().minusMillis(tmpTimeout).toDate()))
        );
    }

    @Override
    public void setEnvironment(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, ENV_FILES);
        tmpTimeout = propertyResolver.getProperty("tmpTimeout", Integer.class, 7200000);
    }


    public void markAsUsed(List<String> names) {
        List<GridFSDBFile> unusedFiles = gridFsTemplate.find(new Query().addCriteria(Criteria.where("filename").in(names)));
        DBObject metaData = new BasicDBObject();
        metaData.put("used", true);
        for (GridFSDBFile unusedFile : unusedFiles) {
            unusedFile.setMetaData(metaData);
            unusedFile.save();
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void clearUnusedFiles() {
        gridFsTemplate.delete(new Query()
                .addCriteria(Criteria.where("metadata.used").exists(false))
                .addCriteria(Criteria.where("uploadDate").lt(new DateTime().minusMillis(tmpTimeout).toDate()))
        );
    }
}
