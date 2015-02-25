package net.org.selector.storer.service;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.sun.istack.internal.Nullable;
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
import java.util.List;
import java.util.Set;

/**
 * SLitvinov on 25.02.2015.
 */
@Service
public class FileService implements EnvironmentAware {
    @Inject
    private GridFsTemplate gridFsTemplate;
    private static final String ENV_FILES = "files.";
    private RelaxedPropertyResolver propertyResolver;

    @Scheduled(cron = "0 1 * * * ?")
    public void clearUnusedFiles() {
        List<GridFSDBFile> tmpFiles = gridFsTemplate.find(new Query().addCriteria(Criteria.where("metadata.used").exists(false)));
        Set<GridFSDBFile> oldFiles = FluentIterable
            .from(tmpFiles)
            .filter(new Predicate<GridFSDBFile>() {
                @Override
                public boolean apply(GridFSDBFile file) {
                    return new DateTime(file.getUploadDate()).isBefore(new DateTime().minusMillis(propertyResolver.getProperty("unusedLiveInMillis", Integer.class, 3600000)));
                }
            }).toSet();
        Set<String> filesForRemove = Sets.newHashSet(Iterables.transform(oldFiles, new Function<GridFSDBFile, String>() {
            @Nullable
            @Override
            public String apply(GridFSDBFile input) {
                return input.getFilename();
            }
        }));
        gridFsTemplate.delete(new Query().addCriteria(Criteria.where("filename").in(filesForRemove)));
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

    @Override
    public void setEnvironment(Environment environment) {
        this.propertyResolver = new RelaxedPropertyResolver(environment, ENV_FILES);
    }
}
