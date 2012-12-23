/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.spring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes a list of paths to resources and turns them into different 
 * things (file/IS/resource).
 * This also allows us to look on a relative or absolute path and will
 * automatically check the typical places one might expect to put DSpace
 * config files.
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class ResourceFinder {

    private static Logger log = LoggerFactory.getLogger(ResourceFinder.class);

    public static final String relativePath = DSpaceConfigurationService.DSPACE + "/";
    public static final String environmentPathVariable = DSpaceConfigurationService.DSPACE_HOME;

    private static List<Resource> makeResources(List<String> paths) {
        List<Resource> rs = new ArrayList<Resource>();
        if (paths != null && !paths.isEmpty()) {
            for (String path : paths) {
                try {
                    Resource r = makeResource(path);
                    rs.add(r);
                } catch (IllegalArgumentException e) {
                    // do not add if not found, just skip
                    log.error(e.getMessage() + ", continuing...");
                }
            }
        }
        return rs;
    }

    private static Resource makeResource(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Resource r = findResource(path);
        if (! r.exists()) {
            // try to find just the fileName
            // get the fileName from the path
            int fileStart = path.lastIndexOf('/') + 1;
            String fileName = path.substring(fileStart);
            r = findResource(fileName);
        }
        // try the environment path first
        if (! r.exists()) {
            throw new IllegalArgumentException("Could not find this resource ("+path+") in any of the checked locations");
        }
        return r;
    }

    private static Resource findResource(String path) {
        Resource r;
        String envPath = getEnvironmentPath() + path;
        r = new FileSystemResource(envPath);
        if (! r.exists()) {
            // try the relative path next
            String relPath = getRelativePath() + path;
            r = new FileSystemResource(relPath);         
            if (! r.exists()) {
                // now try the classloaders
                ClassLoader cl = ResourceFinder.class.getClassLoader();
                r = new ClassPathResource(path, cl);
                if (! r.exists()) {
                    // finally try the context classloader
                    cl = Thread.currentThread().getContextClassLoader();
                    r = new ClassPathResource(path, cl);
                }
            }
        }
        return r;
    }

    /**
     * Resolves a list of paths into resources relative to environmental 
     * defaults or relative paths or the classloader.
     *
     * @param paths a list of paths to resources (org/sakaiproject/mystuff/Thing.xml)
     * @return an array of Spring Resource objects
     */
    public static Resource[] getResources(List<String> paths) {
        return makeResources(paths).toArray(new Resource[paths.size()]);
    }

    public static File[] getFiles(List<String> paths) {
        List<Resource> rs = makeResources(paths);
        File[] files = new File[rs.size()];
        for (int i = 0; i < rs.size(); i++) {
            Resource r = rs.get(i);
            try {
                files[i] = r.getFile();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get file for: " + r.getFilename(), e);
            }
        }
        return files;
    }

    public static InputStream[] getInputStreams(List<String> paths) {
        List<Resource> rs = makeResources(paths);
        InputStream[] streams = new InputStream[rs.size()];
        for (int i = 0; i < rs.size(); i++) {
            Resource r = rs.get(i);
            try {
                streams[i] = r.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get inputstream for: " + r.getFilename(), e);
            }
        }
        return streams;
    }

    /**
     * Resolve a path into a resource relative to environmental defaults 
     * or relative paths or the classloader.
     *
     * @param path a path to a resource (org/dspace/mystuff/Thing.xml)
     * @return the Spring Resource object
     * @throws IllegalArgumentException if no resource can be found
     */
    public static Resource getResource(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Invalid null path");
        }

        return makeResource(path);
    }

    /**
     * Attempt to resolve multiple paths in order until one is found.
     *
     * @param paths an array of paths to a resource (org/dspace/mystuff/Thing.xml)
     * @return the Spring Resource object
     * @throws IllegalArgumentException if no resource can be found
     */
    public static Resource getResourceFromPaths(String[] paths) {
        if (paths == null) {
            throw new IllegalArgumentException("Invalid null paths");
        }
        Resource r = null;
        for (String path : paths) {
            try {
                r = makeResource(path);
            } catch (IllegalArgumentException e) {
                continue;
            }
            break;
        }
        if (r == null) {
            throw new IllegalArgumentException("Could not find any resource from paths (" + Arrays.toString(paths) + ") in any of the checked locations");
        }
        return r;
    }

    public static File getFile(String path) {
        Resource r = getResource(path);
        File f = null;
        try {
            f = r.getFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get file for: " + r.getFilename(), e);
        }
        return f;
    }

    public static InputStream getInputStream(String path) {
        Resource r = getResource(path);
        InputStream is = null;
        try {
            is = r.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get inputstream for: " + r.getFilename(), e);
        }
        return is;
    }

    protected static String getRelativePath() {
        File currentPath = new File("");
        File f = new File(currentPath, relativePath);
        if (! f.exists() || ! f.isDirectory()) {
            f = new File(currentPath, DSpaceConfigurationService.DSPACE);
            if (! f.exists() || ! f.isDirectory()) {
                f = currentPath;
            }
        }
        String absPath = f.getAbsolutePath();
        if (! absPath.endsWith(File.separatorChar + "")) {
            absPath += File.separatorChar;
        }
        return absPath;
    }

    protected static String getEnvironmentPath() {
        String envPath = System.getenv(environmentPathVariable);
        if (envPath == null) {
            envPath = System.getProperty(environmentPathVariable);
            if (envPath == null) {
                String container = getContainerHome();
                if (container == null) {
                    container = "";
                }
                envPath = container + File.separatorChar + DSpaceConfigurationService.DSPACE + File.separatorChar;
            }
        }
        return envPath;
    }

    /**
     * If running in Tomcat, get its home directory.
     *
     * @return the container home if one can be found
     */
    public static String getContainerHome() {
        String catalina = System.getProperty("catalina.base");
        if (catalina == null) {
            catalina = System.getProperty("catalina.home");
        }
        return catalina;
    }

}
