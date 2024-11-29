/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.core.ReloadableEntity;

/**
 * Database object representing notify service inbound patterns. Every {@link org.dspace.app.ldn.NotifyServiceEntity}
 * may have inbounds and outbounds. Inbounds are to be sent to the external service.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "notifyservice_inbound_pattern")
public class NotifyServiceInboundPattern implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifyservice_inbound_pattern_id_seq")
    @SequenceGenerator(name = "notifyservice_inbound_pattern_id_seq",
        sequenceName = "notifyservice_inbound_pattern_id_seq",
        allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private NotifyServiceEntity notifyService;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "constraint_name")
    private String constraint;

    @Column(name = "automatic")
    private boolean automatic;

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public NotifyServiceEntity getNotifyService() {
        return notifyService;
    }

    public void setNotifyService(NotifyServiceEntity notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * @see <a href="https://notify.coar-repositories.org/patterns">coar documentation</a>
     * @return pattern of the inbound notification
     */
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the condition checked for automatic evaluation
     */
    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    /**
     * when true - the notification is automatically when constraints are respected.
     * @return the automatic flag
     */
    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }
}
