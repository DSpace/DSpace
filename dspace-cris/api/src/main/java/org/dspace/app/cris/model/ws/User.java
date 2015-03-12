/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.ws;

import it.cilea.osd.common.core.HasTimeStampInfo;
import it.cilea.osd.common.core.ITimeStampInfo;
import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.common.model.Identifiable;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;



@Entity
@Table(name = "cris_ws_user")
@NamedQueries( {
        @NamedQuery(name = "User.findAll", query = "from User order by id"),
        @NamedQuery(name = "User.count", query = "select count(*) from User"),
        @NamedQuery(name = "User.paginate.id.asc", query = "from User order by id asc"),
        @NamedQuery(name = "User.paginate.id.desc", query = "from User order by id desc"),
        @NamedQuery(name = "User.uniqueByUsernameAndPassword", query = "from User where normalAuth.username = ? AND normalAuth.password = ?"),
        @NamedQuery(name = "User.uniqueByToken", query = "from User where specialAuth.token = ?")        
})
public class User implements Identifiable, HasTimeStampInfo
{
    
    @Transient
    public static final String TYPENORMAL = "normal"; 
    @Transient
    public static final String TYPESPECIAL = "token";
    
    /** DB Primary key */
    @Id
    @GeneratedValue(generator = "CRIS_WS_USER_SEQ")
    @SequenceGenerator(name = "CRIS_WS_USER_SEQ", sequenceName = "CRIS_WS_USER_SEQ", allocationSize = 1)
    private Integer id;
    
    /** timestamp info for creation and last modify */
    @Embedded
    private TimeStampInfo timeStampInfo;
    
    private boolean enabled; 
    
    private String typeDef;
    
    @Embedded
    private UsernamePassword normalAuth;
    
    @Embedded
    private TokenIP specialAuth;   
    
    @OneToMany(cascade=CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "cris_ws_user2crit")   
    private List<Criteria> criteria;
        
    /**
     * Show or hide the hidden metadata
     */
    private boolean showHiddenMetadata = false;
    
    public User()
    {       
        if(this.getTypeDef()==null) {
            this.setTypeDef(TYPENORMAL);
        }
    }
    
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public UsernamePassword getNormalAuth()    
    {
        if(this.normalAuth==null) {
            this.normalAuth = new UsernamePassword();
        }
        return normalAuth;
    }

    public void setNormalAuth(UsernamePassword normalAuth)
    {
        this.normalAuth = normalAuth;
    }

    public TokenIP getSpecialAuth()
    {
        if(this.specialAuth==null) {
            this.specialAuth = new TokenIP();
        }
        return specialAuth;
    }

    public void setSpecialAuth(TokenIP specialAuth)
    {
        this.specialAuth = specialAuth;
    }

    
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public ITimeStampInfo getTimeStampInfo()
    {
        if (timeStampInfo == null)
        {
            timeStampInfo = new TimeStampInfo();
        }
        return timeStampInfo;
    }

    public void setCriteria(List<Criteria> criteria)
    {
        this.criteria = criteria;
    }

    public List<Criteria> getCriteria()    
    {
        if(this.criteria==null) {
            criteria = new LinkedList<Criteria>();
        }
        return criteria;
    }

    public String getTypeDef()
    {
        return typeDef;
    }

    public void setTypeDef(String type)
    {
        this.typeDef = type;
    }
    
    public String getUsername() {
        if(getNormalAuth()!=null) {
            return getNormalAuth().getUsername();
        }
        return "";
    }
    
    public String getPassword() {
        if(getNormalAuth()!=null) {
            return getNormalAuth().getPassword();
        }
        return "";        
    }
    
    public String getToken() {
        if(getSpecialAuth()!=null) {
            return getSpecialAuth().getToken();
        }
        return "";
                
    }
    
    public String getFromIP() {
        if(getSpecialAuth()!=null) {
            return getSpecialAuth().getFromIP();
        }
        return "";       
    }
    
    public String getToIP() {
        if(getSpecialAuth()!=null) {
            return getSpecialAuth().getToIP();
        }
        return "";        
    }

    public void setShowHiddenMetadata(boolean skipRespondeValidation)
    {
        this.showHiddenMetadata = skipRespondeValidation;
    }

    public boolean isShowHiddenMetadata()
    {
        return showHiddenMetadata;
    }
}
