/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.cache;

import org.apache.commons.io.FileUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.xoai.services.api.cache.XOAILastCompilationCacheService;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DSpaceXOAILastCompilationCacheService implements XOAILastCompilationCacheService {

    private static final ThreadLocal<DateFormat> format = new ThreadLocal<DateFormat>(){
                            @Override
                            protected DateFormat initialValue() {
                                return new SimpleDateFormat();
                            }
                          };
    private static final String DATEFILE = File.separator + "date.file";

    private static File file = null;

    private static File getFile()
    {
        if (file == null)
        {
            String dir = ConfigurationManager.getProperty("oai", "cache.dir") + DATEFILE;
            file = new File(dir);
        }
        return file;
    }


    @Override
    public boolean hasCache() {
        return getFile().exists();
    }





    @Override
    public void put(Date date) throws IOException {
        FileUtils.write(getFile(), format.get().format(date));
    }





    @Override
    public Date get() throws IOException {
        try {
            return format.get().parse(FileUtils.readFileToString(getFile()).trim());
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

}
