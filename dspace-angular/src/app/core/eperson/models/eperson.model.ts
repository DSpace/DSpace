import { autoserialize, inheritSerialization } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { PaginatedList } from '../../data/paginated-list.model';
import { RemoteData } from '../../data/remote-data';

import { DSpaceObject } from '../../shared/dspace-object.model';
import { HALLink } from '../../shared/hal-link.model';
import { EPERSON } from './eperson.resource-type';
import { Group } from './group.model';
import { GROUP } from './group.resource-type';

@typedObject
@inheritSerialization(DSpaceObject)
export class EPerson extends DSpaceObject {
  static type = EPERSON;

  /**
   * A string representing the unique handle of this Collection
   */
  @autoserialize
  public handle: string;

  /**
   * A string representing the netid of this EPerson
   */
  @autoserialize
  public netid: string;

  /**
   * A string representing the last active date for this EPerson
   */
  @autoserialize
  public lastActive: string;

  /**
   * A boolean representing if this EPerson can log in
   */
  @autoserialize
  public canLogIn: boolean;

  /**
   * The EPerson email address
   */
  @autoserialize
  public email: string;

  /**
   * A boolean representing if this EPerson require certificate
   */
  @autoserialize
  public requireCertificate: boolean;

  /**
   * A boolean representing if this EPerson registered itself
   */
  @autoserialize
  public selfRegistered: boolean;

  /**
   * The password of this EPerson
   */
  @autoserialize
  public password: string;

  /**
   * Getter to retrieve the EPerson's full name as a string
   */
  get name(): string {
    return this.firstMetadataValue('eperson.firstname') + ' ' + this.firstMetadataValue('eperson.lastname');
  }

  _links: {
    self: HALLink;
    groups: HALLink;
  };

  /**
   * The list of Groups this EPerson is part of
   * Will be undefined unless the groups {@link HALLink} has been resolved.
   */
  @link(GROUP, true)
  public groups?: Observable<RemoteData<PaginatedList<Group>>>;

}
