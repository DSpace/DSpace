package org.dspace.app.cris.model.orcid;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import it.cilea.osd.common.core.SingleTimeStampInfo;
import it.cilea.osd.common.model.IdentifiableObject;

@Entity
@Table(name = "cris_orcid_history")
@NamedQueries({
    @NamedQuery(name = "OrcidHistory.findAll", query = "from OrcidHistory order by id") 
})
public class OrcidHistory extends IdentifiableObject {
    
	/** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_ORCIDHISTORY_SEQ")
    @SequenceGenerator(name = "CRIS_ORCIDHISTORY_SEQ", sequenceName = "CRIS_ORCIDHISTORY_SEQ", allocationSize = 1)
    private Integer id;
    
    private Integer itemId;
    
    private Integer projectId;
    
    private Integer researcherId;
    
    @Embedded
    @AttributeOverride(name = "timestamp", column = @Column(name = "lastAttempt"))
    private SingleTimeStampInfo timestampLastAttempt;

    @Embedded
    @AttributeOverride(name = "timestamp", column = @Column(name = "lastSuccess"))
    private SingleTimeStampInfo timestampSuccessAttempt;
        
    @Type(type="org.hibernate.type.StringClobType")
    private String responseMessage;
    
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

	public SingleTimeStampInfo getTimestampLastAttempt() {
		return timestampLastAttempt;
	}

	public void setTimestampLastAttempt(SingleTimeStampInfo timestampLastAttempt) {
		this.timestampLastAttempt = timestampLastAttempt;
	}

	public SingleTimeStampInfo getTimestampSuccessAttempt() {
		return timestampSuccessAttempt;
	}

	public void setTimestampSuccessAttempt(SingleTimeStampInfo timestampSuccessAttempt) {
		this.timestampSuccessAttempt = timestampSuccessAttempt;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}
}
