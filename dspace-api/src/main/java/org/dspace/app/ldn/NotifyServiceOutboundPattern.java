/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Database object representing notify services outbound patterns. Every {@link org.dspace.app.ldn.NotifyServiceEntity}
 * may have inbounds and outbounds. Outbounds are to be sent to the external service.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "notifyservice_outbound_pattern")
public class NotifyServiceOutboundPattern {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifyservice_outbound_pattern_id_seq")
    @SequenceGenerator(name = "notifyservice_outbound_pattern_id_seq",
        sequenceName = "notifyservice_outbound_pattern_id_seq",
        allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private NotifyServiceEntity notifyService;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "constraint_name")
    private String constraint;

    public Integer getId() {
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
     * https://notify.coar-repositories.org/patterns/
     * @return pattern of the outbound notification
     */
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }
}
