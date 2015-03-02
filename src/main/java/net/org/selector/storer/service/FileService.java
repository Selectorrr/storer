package net.org.selector.storer.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import net.org.selector.storer.domain.Prop;
import net.org.selector.storer.repository.FilePropsRepository;
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
    private RelaxedPropertyResolver propertyResolver;
    @Inject
    private FilePropsRepository filePropsRepository;


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

    @Scheduled(cron = "1 * * * * ?")
    public void clearUnusedFiles() {
        Prop lastSync = filePropsRepository.findOne("lastSync");
//        DateTime deadTime = new DateTime(lastSync.getValue()).plusMillis(unusedLiveInMillis);
        gridFsTemplate.delete(new Query()
            .addCriteria(Criteria
                    .where("metadata.deleteTime").lt(new DateTime(lastSync.getValue()).getMillis())
//                    .orOperator(
//                        Criteria.where("metadata.deleteTime").exists(false)
//                            .andOperator(
//                                Criteria.where("uploadDate").lt(new DateTime().minusMillis(unusedLiveInMillis).toDate())
//                            )
//                    )
            ));
    }

    public void markAsUsed(List<String> names) {
        List<GridFSDBFile> unusedFiles = gridFsTemplate.find(new Query().addCriteria(Criteria.where("filename").in(names)));
        DBObject metaData = new BasicDBObject();
        Integer unusedLiveInMillis = propertyResolver.getProperty("unusedLiveInMillis", Integer.class, 3600000);
        metaData.put("deleteTime", new DateTime().getMillis() + unusedLiveInMillis);
        for (GridFSDBFile unusedFile : unusedFiles) {
            unusedFile.setMetaData(metaData);
            unusedFile.save();
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, ENV_FILES);
    }
}
