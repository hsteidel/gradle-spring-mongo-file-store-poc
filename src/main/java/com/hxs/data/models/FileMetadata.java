package com.hxs.data.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import io.swagger.annotations.ApiModel;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This class is the user-friendly JSON representation of GridFSFile.
 * It takes in a GridFSDBFile as an argument and only presents the good stuff.
 *
 *   A necessary evil.
 *
 *   there's also a friendly metadata, whatever you want to stick in json object (map)
 */
@ApiModel(
        value = "FileMetadata",
        description = "A file object representation with its metadata."
)
public class FileMetadata extends GridFSFile {

    public static final String LAST_MODE_DATE_KEY = "lastModDate";

    public FileMetadata() {
        super();
    }

    public FileMetadata(GridFSDBFile file) {
        this.put("_id", file.getId());
        this.put("filename", file.getFilename());
        this.put("contentType", file.getContentType());
        this.put("length", file.getLength());
        this.put("uploadDate", file.getUploadDate());
        this.put("md5", file.getMD5());
        this.put("aliases", file.getAliases());
        this.put(LAST_MODE_DATE_KEY, file.get(LAST_MODE_DATE_KEY));
        this.setMetaData(file.getMetaData());
    }

    @Override
    @NotNull
    public String getFilename() {
        return super.getFilename();
    }

    @Override
    public Object getId() {
        return ((ObjectId)super.getId()).toHexString();
    }

    public void getLastModDate(Date modifiedDate) {
        this.put(LAST_MODE_DATE_KEY, modifiedDate);
    }

    public Date getLastModDate(){
        return (Date) this.get(LAST_MODE_DATE_KEY);
    }

    public void setMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            DBObject dbMetadata = new BasicDBObject(metadata);
            super.setMetaData(dbMetadata);
        }
    }

    public void setFilename(String filename){
        this.put("filename", filename);
    }

    public Map<String, Object> getMetadata() {
        return super.getMetaData() == null ? null : super.getMetaData().toMap();
    }

    public void setAliases(List<String> aliases){
        super.put("aliases", aliases);
    }

    /*These are the non-JSON friendly bits*/

    @Override
    @JsonIgnore
    protected GridFS getGridFS() {
        return super.getGridFS();
    }

    @Override
    @JsonIgnore
    public DBObject getMetaData() {
        return super.getMetaData();
    }

    @Override
    @JsonIgnore
    public boolean isPartialObject() {
        return super.isPartialObject();
    }

    @Override
    @JsonIgnore
    public long getChunkSize() {
        return super.getChunkSize();
    }

}
