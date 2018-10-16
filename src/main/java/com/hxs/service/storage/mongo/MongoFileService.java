package com.hxs.service.storage.mongo;

import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *  GridFSFile DAO/Wrapper Service with pre-built CRUD methods
 *  for re-usability amongst true "logic" service layers
 *
 *  Note: very little, if any, logic should be here
 *
 * @author HSteidel
 */
@Service
public class MongoFileService {

    private static final Logger logger = LoggerFactory.getLogger(MongoFileService.class);

    private GridFsTemplate gridFsTemplate;

    public MongoFileService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }


    public GridFSFile storeFile(InputStream inputStream, String filename){
        return gridFsTemplate.store(inputStream, filename);
    }


    public GridFSDBFile findFileByName(String name) {
        return gridFsTemplate.findOne(Query.query(GridFsCriteria.where("filename").is(name)));
    }

    public List<GridFSDBFile> findAllFiles(){
        return new ArrayList<>(gridFsTemplate.find(Query.query(Criteria.where("_id").exists(true))));
    }


    /**
     * Simply deletes a single file
     * @param file
     */
    public void deleteSingleGridFile(GridFSDBFile file){
        Query deleteQuery = Query.query(Criteria.where("_id").is(file.getId()));
        gridFsTemplate.delete(deleteQuery);
        logger.info("Deleted file : " + file.getId());
    }

}
