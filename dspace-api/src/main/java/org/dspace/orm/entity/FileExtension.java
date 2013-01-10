/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author Miguel Pinto <mpinto@lyncode.com>
 * @version $Revision$
 */


@Entity
@Table(name = "fileextension")
@SequenceGenerator(name="fileextension_gen", sequenceName="fileextension_seq")
@Configurable
public class FileExtension extends DSpaceObject{
    private BitstreamFormat bitstreamFormat;
    private String extension;

   
    @Id
    @Column(name = "file_extension_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="fileextension_gen")
    public int getID() {
        return id;
    }
    
    @Override
    @Transient
    public DSpaceObjectType getType()
    {
    	return DSpaceObjectType.FILE_EXTENSION;
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
