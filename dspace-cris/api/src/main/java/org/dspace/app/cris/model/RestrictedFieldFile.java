/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.springframework.web.multipart.MultipartFile;

/**
 * This class extend RestrictedField to manage specific file information coming
 * from the browser. The value attribute of the base RestrictedField class is
 * used to store the file extension (.jpg, .gif, etc.).
 * 
 * @author cilea
 * 
 */
@Embeddable
@MappedSuperclass
public class RestrictedFieldFile extends RestrictedField
{

    @Type(type = "text")
    /**
     * the mime type of the image as supplied from the browser request
     */
    private String mimeType;

    @Transient
    /**
     * the MultipartFile supplied from the browser request. Needed to use the
     * Spring MVC autobinder
     */    
    private MultipartFile file;

    /**
     * Getter method.
     * 
     * @return the mime type of the image as supplied from the browser request
     */
    public String getMimeType()
    {
        return mimeType;
    }

    /**
     * Setter method.
     * 
     * @param mimeType
     *            the mime type of the image as supplied from the browser
     *            request
     */
    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Getter method. Needed to use the Spring MVC autobinder
     * 
     * @return the MultipartFile supplied from the browser request.
     */
    public MultipartFile getFile()
    {
        return file;
    }

    /**
     * Setter method. Needed to use the Spring MVC autobinder
     * 
     * @param file the MultipartFile supplied from the browser request.
     */
    public void setFile(MultipartFile file)
    {
        this.file = file;
    }
}
