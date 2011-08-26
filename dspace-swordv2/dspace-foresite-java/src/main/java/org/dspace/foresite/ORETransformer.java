/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * @Author Richard Jones
 */
public class ORETransformer
{
    public static ResourceMapDocument transformToDocument(String source, String target, InputStream is)
            throws OREParserException, ORESerialiserException
    {
        OREParser parser = OREParserFactory.getInstance(source);
        ResourceMap rem = parser.parse(is);
        ORESerialiser serialiser = ORESerialiserFactory.getInstance(target);
        ResourceMapDocument rmd = serialiser.serialise(rem);
        return rmd;
    }

    public static InputStream transformToStream(String source, String target, InputStream is)
            throws OREParserException, ORESerialiserException
    {
        ResourceMapDocument rmd = ORETransformer.transformToDocument(source, target, is);
        byte[] bytes = rmd.getSerialisation().getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }
}
