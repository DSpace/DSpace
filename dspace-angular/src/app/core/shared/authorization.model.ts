import { link, typedObject } from '../cache/builders/build-decorators';
import { AUTHORIZATION } from './authorization.resource-type';
import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { HALLink } from './hal-link.model';
import { Observable } from 'rxjs';
import { RemoteData } from '../data/remote-data';
import { EPerson } from '../eperson/models/eperson.model';
import { EPERSON } from '../eperson/models/eperson.resource-type';
import { FEATURE } from './feature.resource-type';
import { DSpaceObject } from './dspace-object.model';
import { Feature } from './feature.model';
import { ITEM } from './item.resource-type';

/**
 * Class representing a DSpace Authorization
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class Authorization extends DSpaceObject {
  static type = AUTHORIZATION;

  /**
   * Unique identifier for this authorization
   */
  @autoserialize
  id: string;

  @deserialize
  _links: {
    self: HALLink;
    eperson: HALLink;
    feature: HALLink;
    object: HALLink;
  };

  /**
   * The EPerson this Authorization belongs to
   * Null if the authorization grants access to anonymous users
   */
  @link(EPERSON)
  eperson?: Observable<RemoteData<EPerson>>;

  /**
   * The Feature enabled by this Authorization
   */
  @link(FEATURE)
  feature?: Observable<RemoteData<Feature>>;

  /**
   * The Object this authorization applies to
   */
  @link(ITEM)
  object?: Observable<RemoteData<DSpaceObject>>;
}
