package org.dspace.eperson;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
/**
 * @author Alba Aliu (alba.aliu at alba.aliu@atis.al)
 *
 */
@Entity
@DiscriminatorValue("FREQUENCY")
public class FrequencySubscriptionParameter extends SubscriptionParameter {
}
