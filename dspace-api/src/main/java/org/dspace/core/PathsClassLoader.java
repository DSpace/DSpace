/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Load classes from a custom class path.  This loader first delegates to the
 * parent loader in the usual way.  If no parent can load the class, this loader
 * will then search the path in the order given.  Each element of the path may
 * name a directory (which will be searched for .class files) or a JAR file
 * (likewise searched for .class files).  Searching consists of converting the
 * binary name of the class to a path within the directory or archive, and looking
 * it up there, as with the system class path.  The only reason that this loader
 * exists is to facilitate the loading of classes into a Servlet application from
 * paths other than the class path established by the servlet container -- for
 * example, paths provided by the application's private configuration.
 *
 * @author Mark H. Wood
 */
public class PathsClassLoader
        extends ClassLoader
{
    /** Filesystem paths to be searched. */
    private final String[] classpath;

    /**
     * Instantiate to use a custom class path.
     *
     * @param parent delegate to this ClassLoader first.
     * @param classpath filesystem paths to be searched for classes and JARs.
     */
    PathsClassLoader(ClassLoader parent, String[] classpath)
    {
        super(parent);
        this.classpath = classpath;
    }

    @Override
    protected Class findClass(String name) throws ClassNotFoundException
    {
        Class found = null;
        for (String aPath : classpath)
        {
            String bodyPath = name.replace('.', '/');
            File pathFile = new File(aPath);
            if (pathFile.isDirectory())
            {
                byte[] body;
                int bodySize;
                File bodyFile = new File(pathFile, bodyPath + ".class");
                if (!bodyFile.exists())
                {
                    continue;
                }
                bodySize = (int) bodyFile.length();
                body = new byte[bodySize];
                FileInputStream bodyStream = null;
                try
                {
                    bodyStream = new FileInputStream(bodyFile);
                    int pos = 0;
                    int len;
                    do
                    {
                        len = bodyStream.read(body, pos, bodySize);
                        pos += len;
                    } while (pos < bodySize);
                } catch (IOException e)
                {
                    throw new ClassNotFoundException("Class body not read", e);
                } finally
                {
                    if (null != bodyStream)
                    {
                        try
                        {
                            bodyStream.close();
                        } catch (IOException ex)
                        {
                            /* don't care */
                        }
                    }
                }
                found = defineClass(name, body, 0, bodySize);
                break;
            }
            else if (pathFile.isFile())
            {
                byte[] body;
                int bodySize;
                InputStream bodyStream = null;
                JarFile jar = null;
                try
                {
                    jar = new JarFile(pathFile);
                    JarEntry entry = jar.getJarEntry(bodyPath + ".class");
                    if (null == entry)
                    {
                        continue;
                    }
                    bodyStream = jar.getInputStream(entry);
                    bodySize = (int) entry.getSize();
                    body = new byte[bodySize];
                    int pos = 0;
                    int len;
                    do
                    {
                        len = bodyStream.read(body, pos, bodySize);
                        pos += len;
                    } while (pos < bodySize);
                } catch (IOException e)
                {
                    throw new ClassNotFoundException("Class body not read", e);
                } finally
                {
                    if (null != bodyStream)
                    {
                        try
                        {
                            bodyStream.close();
                        } catch (IOException e)
                        {
                            /* don't care */
                        }
                    }
                    if (null != jar)
                    {
                        try
                        {
                            jar.close();
                        } catch (IOException e)
                        {
                            /* don't care */
                        }
                    }
                }
                found = defineClass(name, body, 0, bodySize);
                break;
            }
            else
            {
                // Just skip this path element -- probably just file not found here.
            }
        }
        if (null == found)
        {
            throw new ClassNotFoundException(name);
        }
        else
        {
            resolveClass(found);
            return found;
        }
    }
    /*
    @Override
    public URL getResource(String name)
    {
    }
     */
}
