/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.dspace.core.Constants;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "fileextension")
public class FileExtension implements IDSpaceObject{
    private int id;
    private BitstreamFormat bitstreamFormat;
    private String extension;

   
    @Id
    @Column(name = "file_extension_id")
    @GeneratedValue
    public int getID() {
        return id;
    }
    
    public int setID(int id) {
        return this.id= id;
    }
    
    @Override
    @Transient
    public int getType()
    {
    	return Constants.FILEEXTENSION;
    }

    @Column(name = "extension", nullable = true)
	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_format_id", nullable = true)
	public BitstreamFormat getBitstreamFormat() {
		return bitstreamFormat;
	}

	public void setBitstreamFormat(BitstreamFormat bitstreamFormat) {
		this.bitstreamFormat = bitstreamFormat;
	}

	
}
