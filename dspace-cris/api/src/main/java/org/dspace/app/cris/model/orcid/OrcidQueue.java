package org.dspace.app.cris.model.orcid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import it.cilea.osd.common.model.IdentifiableObject;

@Entity
@Table(name = "cris_orcid_queue")
@NamedQueries({
    @NamedQuery(name = "OrcidQueue.findAll", query = "from OrcidQueue order by id"),
    @NamedQuery(name = "OrcidQueue.findOrcidQueueByResearcherId", query = "from OrcidQueue where researcherId = ? order by id")
})
public class OrcidQueue extends IdentifiableObject {

    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_ORCIDQUEUE_SEQ")
    @SequenceGenerator(name = "CRIS_ORCIDQUEUE_SEQ", sequenceName = "CRIS_ORCIDQUEUE_SEQ", allocationSize = 1)
    private Integer id;
    
    private Integer itemId;
    
    private Integer projectId;
    
    private Integer researcherId;
    
    private boolean send;
    
    private String mode;
    
	@Override
	public Integer getId() {
		return id;
	}
	
    public void setId(Integer id)
    {
        this.id = id;
    }

	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	public Integer getProjectId() {
		return projectId;
	}

	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	public Integer getResearcherId() {
		return researcherId;
	}

	public void setResearcherId(Integer researcherId) {
		this.researcherId = researcherId;
	}

	public boolean isSend() {
		return send;
	}

	public void setSend(boolean send) {
		this.send = send;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
