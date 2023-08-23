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
 * Database object representing notify services outbound patterns
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "notifyservices_outbound_patterns")
public class NotifyServiceOutboundPattern {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifyservices_outbound_patterns_id_seq")
    @SequenceGenerator(name = "notifyservices_outbound_patterns_id_seq",
        sequenceName = "notifyservices_outbound_patterns_id_seq",
        allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private NotifyServiceEntity notifyService;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "constrain_name")
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
