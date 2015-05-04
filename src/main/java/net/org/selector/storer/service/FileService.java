package net.org.selector.storer.service;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import net.org.selector.storer.domain.FileInfo;
import net.org.selector.storer.security.AuthoritiesConstants;
import net.org.selector.storer.security.SecurityUtils;
import net.org.selector.storer.service.util.RequestInfoUtil;
import org.apache.commons.io.FilenameUtils;
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
    private ImmutableMap<String, GridFsTemplate> gridFsTemplateRegister;
    @Inject
    private ImmutableSet<String> siteNames;
    private static final String ENV_FILES = "files.";
    private Integer tmpTimeout;


    public void save(String filename, InputStream inputStream, String contentType, List<FileInfo> result) {
        GridFsTemplate gridFsTemplate = gridFsTemplateRegister.get(RequestInfoUtil.getSiteName());
        Query query = new Query().addCriteria(Criteria.where("filename").is(filename));
        List<GridFSDBFile> existFiles = gridFsTemplate.find(query);
        if (existFiles.size() > 0) {
            throw new IllegalArgumentException(String.format("file with name %s already exist", filename));
        }
        GridFSFile file = gridFsTemplate.store(inputStream, filename, contentType, getOwnerMetadata());
        result.add(new FileInfo(FilenameUtils.getName(filename), filename, String.valueOf(file.getLength())));
    }

    public void delete(String s) {
        delete(ImmutableList.of(s));
    }

    public void delete(List<String> s) {
        GridFsTemplate gridFsTemplate = gridFsTemplateRegister.get(RequestInfoUtil.getSiteName());
        Query query = new Query()
            .addCriteria(Criteria.where("filename").in(s));
        if (!SecurityUtils.isUserInRole(AuthoritiesConstants.ADMIN)) {
            query.addCriteria(Criteria.where("metadata.owner").is(SecurityUtils.getCurrentLogin()));
        }
        List<GridFSDBFile> existFiles = gridFsTemplate.find(query);
        if (existFiles.size() != s.size()) {
            throw new IllegalArgumentException(String.format("file with name %s and owner %s not found", s,
                SecurityUtils.getCurrentLogin()));
        }
        gridFsTemplate.delete(query);
    }


    public GridFSDBFile findOneByName(String filename) {
        GridFsTemplate gridFsTemplate = gridFsTemplateRegister.get(RequestInfoUtil.getSiteName());
        Query criteria = new Query().addCriteria(Criteria.where("filename").is(filename));
        return gridFsTemplate.findOne(criteria);
    }

//    public void actualize(List<String> names) {
//        gridFsTemplateRegister.get(RequestInfoUtil.getSiteName()).delete(new Query()
//                .addCriteria(Criteria.where("filename").nin(names))
//                .addCriteria(Criteria.where("uploadDate").lt(new DateTime().minusMillis(tmpTimeout).toDate()))
//        );
//    }

    @Override
    public void setEnvironment(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, ENV_FILES);
        tmpTimeout = propertyResolver.getProperty("tmpTimeout", Integer.class, 7200000);
    }


    public void markAsUsed(List<String> names) {
        Query query = new Query().addCriteria(Criteria.where("filename").in(names));
        if (!SecurityUtils.isUserInRole(AuthoritiesConstants.ADMIN)) {
            query.addCriteria(Criteria.where("metadata.owner").is(SecurityUtils.getCurrentLogin()));
        }
        List<GridFSDBFile> unusedFiles = gridFsTemplateRegister.get(RequestInfoUtil.getSiteName())
            .find(query);
        for (GridFSDBFile unusedFile : unusedFiles) {
            DBObject metaData = Objects.firstNonNull(unusedFile.getMetaData(), new BasicDBObject());
            metaData.put("used", true);
            unusedFile.setMetaData(metaData);
            unusedFile.save();
        }
    }

    @Scheduled(fixedDelay = 600000)
    public void clearUnusedFiles() {
        for (String siteName : siteNames) {
            gridFsTemplateRegister.get(siteName).delete(new Query()
                    .addCriteria(Criteria.where("metadata.used").exists(false))
                    .addCriteria(Criteria.where("uploadDate").lt(new DateTime().minusMillis(tmpTimeout).toDate()))
            );
        }
    }

    private DBObject getOwnerMetadata() {
        DBObject metadata = new BasicDBObject();
        metadata.put("owner", SecurityUtils.getCurrentLogin());
        return metadata;
    }
}
