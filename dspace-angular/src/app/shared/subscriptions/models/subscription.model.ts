import { Observable } from 'rxjs';
import { autoserialize, deserialize, inheritSerialization } from 'cerialize';

import { link, typedObject } from '../../../core/cache/builders/build-decorators';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { HALLink } from '../../../core/shared/hal-link.model';
import { SUBSCRIPTION } from './subscription.resource-type';
import { EPerson } from '../../../core/eperson/models/eperson.model';
import { RemoteData } from '../../../core/data/remote-data';
import { EPERSON } from '../../../core/eperson/models/eperson.resource-type';
import { DSPACE_OBJECT } from '../../../core/shared/dspace-object.resource-type';

@typedObject
@inheritSerialization(DSpaceObject)
export class Subscription extends DSpaceObject {
  static type = SUBSCRIPTION;

  /**
   * A string representing subscription type
   */
  @autoserialize
  public id: string;

  /**
   * A string representing subscription type
   */
  @autoserialize
  public subscriptionType: string;

  /**
   * An array of parameters for the subscription
   */
  @autoserialize
  public subscriptionParameterList: SubscriptionParameterList[];

  /**
   * The {@link HALLink}s for this Subscription
   */
  @deserialize
  _links: {
    self: HALLink;
    eperson: HALLink;
    resource: HALLink;
  };

  /**
   * The logo for this Community
   * Will be undefined unless the logo {@link HALLink} has been resolved.
   */
  @link(EPERSON)
  eperson?: Observable<RemoteData<EPerson>>;

  /**
   * The logo for this Community
   * Will be undefined unless the logo {@link HALLink} has been resolved.
   */
  @link(DSPACE_OBJECT)
  resource?: Observable<RemoteData<DSpaceObject>>;
  /**
   * The embedded ePerson & dSpaceObject for this Subscription
   */
  /*  @deserialize
    _embedded: {
      ePerson: EPerson;
      dSpaceObject: DSpaceObject;
    };*/
}

export interface SubscriptionParameterList {
  id: string;
  name: string;
  value: string;
}
