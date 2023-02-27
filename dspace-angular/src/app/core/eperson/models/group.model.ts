import { autoserialize, autoserializeAs, deserialize, inheritSerialization } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { PaginatedList } from '../../data/paginated-list.model';
import { RemoteData } from '../../data/remote-data';

import { DSpaceObject } from '../../shared/dspace-object.model';
import { DSPACE_OBJECT } from '../../shared/dspace-object.resource-type';
import { HALLink } from '../../shared/hal-link.model';
import { EPerson } from './eperson.model';
import { EPERSON } from './eperson.resource-type';
import { GROUP } from './group.resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';

@typedObject
@inheritSerialization(DSpaceObject)
export class Group extends DSpaceObject {
  static type = GROUP;

  /**
   * A string representing the unique name of this Group
   */
  @excludeFromEquals
  @autoserializeAs('name')
  protected _name: string;

  /**
   * A string representing the unique handle of this Group
   */
  @autoserialize
  public handle: string;

  /**
   * A boolean denoting whether this Group is permanent
   */
  @autoserialize
  public permanent: boolean;

  /**
   * The {@link HALLink}s for this Group
   */
  @deserialize
  _links: {
    self: HALLink;
    subgroups: HALLink;
    epersons: HALLink;
    object: HALLink;
  };

  /**
   * The list of Groups this Group is part of
   * Will be undefined unless the groups {@link HALLink} has been resolved.
   */
  @link(GROUP, true)
  public subgroups?: Observable<RemoteData<PaginatedList<Group>>>;

  /**
   * The list of EPeople in this group
   * Will be undefined unless the epersons {@link HALLink} has been resolved.
   */
  @link(EPERSON, true)
  public epersons?: Observable<RemoteData<PaginatedList<EPerson>>>;

  /**
   * Connected dspace object, the community or collection connected to a workflow group (204 no content for non-workflow groups)
   * Will be undefined unless the object {@link HALLink} has been resolved (can only be resolved for workflow groups)
   */
  @link(DSPACE_OBJECT)
  public object?: Observable<RemoteData<DSpaceObject>>;

}
