import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { Observable } from 'rxjs';
import { link } from '../../cache/builders/build-decorators';

import { SubmissionDefinitionsModel } from '../../config/models/config-submission-definitions.model';
import { RemoteData } from '../../data/remote-data';
import { EPerson } from '../../eperson/models/eperson.model';
import { EPERSON } from '../../eperson/models/eperson.resource-type';
import { Collection } from '../../shared/collection.model';
import { COLLECTION } from '../../shared/collection.resource-type';
import { DSpaceObject } from '../../shared/dspace-object.model';
import { HALLink } from '../../shared/hal-link.model';
import { ITEM } from '../../shared/item.resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { WorkspaceitemSectionsObject } from './workspaceitem-sections.model';
import { CacheableObject } from '../../cache/cacheable-object.model';
import { SUPERVISION_ORDER } from '../../supervision-order/models/supervision-order.resource-type';
import { PaginatedList } from '../../data/paginated-list.model';
import { SupervisionOrder } from '../../supervision-order/models/supervision-order.model';

export interface SubmissionObjectError {
  message: string;
  paths: string[];
}

/**
 * An abstract model class for a SubmissionObject.
 */
@inheritSerialization(DSpaceObject)
export abstract class SubmissionObject extends DSpaceObject implements CacheableObject {

  @excludeFromEquals
  @autoserialize
  id: string;

  /**
   * The SubmissionObject last modified date
   */
  @autoserialize
  lastModified: Date;

  /**
   * The collection this submission applies to
   * Will be undefined unless the collection {@link HALLink} has been resolved.
   */
  @link(COLLECTION)
  collection?: Observable<RemoteData<Collection>> | Collection;

  /**
   * The SubmissionObject's last section's data
   */
  @autoserialize
  sections: WorkspaceitemSectionsObject;

  /**
   * The SubmissionObject's last section's errors
   */
  @autoserialize
  errors: SubmissionObjectError[];

  /**
   * The {@link HALLink}s for this SubmissionObject
   */
  @deserialize
  _links: {
    self: HALLink;
    collection: HALLink;
    item: HALLink;
    submissionDefinition: HALLink;
    submitter: HALLink;
    supervisionOrders: HALLink;
  };

  get self(): string {
    return this._links.self.href;
  }

  /**
   * The submission item
   * Will be undefined unless the item {@link HALLink} has been resolved.
   */
  @link(ITEM)
  /* This was changed from 'Observable<RemoteData<Item>> | Item' to 'any' to prevent issues in templates with async */
  item?: any;

  /**
   * The configuration object that define this submission
   * Will be undefined unless the submissionDefinition {@link HALLink} has been resolved.
   */
  @link(SubmissionDefinitionsModel.type)
  submissionDefinition?: Observable<RemoteData<SubmissionDefinitionsModel>> | SubmissionDefinitionsModel;

  /**
   * The submitter for this SubmissionObject
   * Will be undefined unless the submitter {@link HALLink} has been resolved.
   */
  @link(EPERSON)
  submitter?: Observable<RemoteData<EPerson>> | EPerson;

  /**
   * The submission supervision order
   * Will be undefined unless the workspace item {@link HALLink} has been resolved.
   */
  @link(SUPERVISION_ORDER)
  /* This was changed from 'Observable<RemoteData<WorkspaceItem>> | WorkspaceItem' to 'any' to prevent issues in templates with async */
  supervisionOrders?: Observable<RemoteData<PaginatedList<SupervisionOrder>>>;

}
