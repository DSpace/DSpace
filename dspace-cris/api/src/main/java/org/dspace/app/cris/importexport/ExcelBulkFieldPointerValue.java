/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;

import org.dspace.app.cris.util.UtilsXML;
import org.w3c.dom.Element;

public class ExcelBulkFieldPointerValue extends ExcelBulkFieldValue
        implements IBulkChangeFieldPointerValue
{

    public static String REGEX_POINTER_CRISID = "\\[.*CRISID=([\\w\\p{P}]+).*\\](.*)";

    public static String REGEX_POINTER_SOURCEID = "\\[.*SOURCEID=([\\w\\p{P}]+).*\\](.*)";

    public static String REGEX_POINTER_SOURCEREF = "\\[.*SOURCEREF=([\\w\\p{P}]+).*\\](.*)";
    
    public static String REGEX_POINTER_UUID= "\\[.*UUID=([\\w\\p{P}]+).*\\](.*)";

    private static Pattern patternCrisId = Pattern
            .compile(REGEX_POINTER_CRISID);

    private static Pattern patternSourceId = Pattern
            .compile(REGEX_POINTER_SOURCEID);

    private static Pattern patternSourceRef = Pattern
            .compile(REGEX_POINTER_SOURCEREF);

    private static Pattern patternUuid = Pattern
            .compile(REGEX_POINTER_UUID);
    
    private String crisID;

    private String sourceID;

    private String sourceRef;
    
    private String uuid;

    public ExcelBulkFieldPointerValue(Cell element, int position)
    {
        super(element, position);
        String val = element.getContents()
                .split(ExcelBulkField.REGEX_REPEATABLE_SPLIT)[position];
        Matcher tagCrisMatch = patternCrisId.matcher(val);
        if (tagCrisMatch.find())
        {
            this.crisID = tagCrisMatch.group(1);
        }
        Matcher tagSourceIdMatch = patternSourceId.matcher(val);
        if (tagSourceIdMatch.find())
        {
            this.sourceID = tagSourceIdMatch.group(1);
        }
        Matcher tagSourceRefMatch = patternSourceRef.matcher(val);
        if (tagSourceRefMatch.find())
        {
            this.sourceRef = tagSourceRefMatch.group(1);
        }
        Matcher tagUuidMatch = patternUuid.matcher(val);
        if (tagUuidMatch.find())
        {
            this.uuid = tagUuidMatch.group(1);
        }
    }

    public String getCrisID()
    {
        return crisID;
    }

    @Override
    public String getSourceRef()
    {
        return sourceRef;
    }

    @Override
    public String getSourceID()
    {
        return sourceID;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

}
