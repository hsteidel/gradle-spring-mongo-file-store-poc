package com.hxs.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

/**
 * Utility class for all things FileRepository resources
 * @author HSteidel
 */
public class RepoResourceUtils {

    public static final String RESOURCE_TYPE_FILE = "FILE";


    private RepoResourceUtils(){};

    /**
     * Determines whether a file is "streamable" or not.
     * Currently that is, of mime type "audio/*" or "video/*"
     * @param filename
     */
    public static boolean isFileStreamable(String filename){
        String type = new Tika().detect(filename);
        return !StringUtils.isEmpty(type) && (type.startsWith("video") ||  type.startsWith("audio"));
    }

}
