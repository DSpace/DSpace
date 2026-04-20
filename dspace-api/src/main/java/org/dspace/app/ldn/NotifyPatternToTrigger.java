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
import org.dspace.content.Item;
import org.dspace.core.ReloadableEntity;

/**
 * Database object representing notify patterns to be triggered
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "notifypatterns_to_trigger")
public class NotifyPatternToTrigger implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifypatterns_to_trigger_id_seq")
    @SequenceGenerator(name = "notifypatterns_to_trigger_id_seq",
        sequenceName = "notifypatterns_to_trigger_id_seq",
        allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "item_id", referencedColumnName = "uuid")
    private Item item;

    @ManyToOne
    @JoinColumn(name = "service_id", referencedColumnName = "id")
    private NotifyServiceEntity notifyService;

    @Column(name = "pattern")
    private String pattern;

    public void setId(Integer id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    @Override
    public Integer getID() {
        return id;
    }
}
