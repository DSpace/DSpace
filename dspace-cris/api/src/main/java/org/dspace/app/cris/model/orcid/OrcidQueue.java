/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.orcid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import it.cilea.osd.common.model.IdentifiableObject;

@Entity
@Table(name = "cris_orcid_queue")
@NamedQueries({
    @NamedQuery(name = "OrcidQueue.findAll", query = "from OrcidQueue order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByOwner", query = "from OrcidQueue where owner = ? order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByOwnerAndTypeId", query = "from OrcidQueue where owner = ? and typeId = ? order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByProjectId", query = "from OrcidQueue where entityId = ? and typeId = 10 order by id"),
    @NamedQuery(name = "OrcidQueue.uniqueOrcidQueueByProjectIdAndOwner", query = "from OrcidQueue where entityId = ? and typeId = 10 and owner = ? order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByPublicationId", query = "from OrcidQueue where entityId = ? and typeId = 2 order by id"),
    @NamedQuery(name = "OrcidQueue.uniqueOrcidQueueByPublicationIdAndOwner", query = "from OrcidQueue where entityId = ? and typeId = 2 and owner = ? order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByEntityIdAndTypeId", query = "from OrcidQueue where entityId = ? and typeId = ? order by id"),
    @NamedQuery(name = "OrcidQueue.uniqueOrcidQueueByEntityIdAndTypeIdAndOwner", query = "from OrcidQueue where entityId = ? and typeId = ? and owner = ? order by id"),    
    @NamedQuery(name = "OrcidQueue.deleteByOwnerAndTypeId", query = "delete from OrcidQueue where owner = ? and typeId = ?"),
    @NamedQuery(name = "OrcidQueue.deleteByOwnerAndUuid", query = "delete from OrcidQueue where owner = ? and fastlookupUuid = ?")
})
public class OrcidQueue extends IdentifiableObject {

    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_ORCIDQUEUE_SEQ")
    @SequenceGenerator(name = "CRIS_ORCIDQUEUE_SEQ", sequenceName = "CRIS_ORCIDQUEUE_SEQ", allocationSize = 1)
    private Integer id;
    
    private String owner;
    
    private Integer entityId;
    
    private Integer typeId;
    
    private String mode;
    
    @Type(type="org.hibernate.type.StringClobType")
    private String fastlookupObjectName;
    
    private String fastlookupUuid;
    
	@Override
	public Integer getId() {
		return id;
	}
	
    public void setId(Integer id)
    {
        this.id = id;
    }

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getFastlookupObjectName() {
		return fastlookupObjectName;
	}

	public void setFastlookupObjectName(String fastlookupObjectName) {
		this.fastlookupObjectName = fastlookupObjectName;
	}

	public String getFastlookupUuid() {
		return fastlookupUuid;
	}

	public void setFastlookupUuid(String fastlookupUuid) {
		this.fastlookupUuid = fastlookupUuid;
	}

}
