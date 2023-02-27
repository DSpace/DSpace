import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { Observable } from 'rxjs';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { Version } from './version.model';
import { VERSION_HISTORY } from './version-history.resource-type';
import { link, typedObject } from '../cache/builders/build-decorators';
import { DSpaceObject } from './dspace-object.model';
import { HALLink } from './hal-link.model';
import { VERSION } from './version.resource-type';

/**
 * Class representing a DSpace Version History
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class VersionHistory extends DSpaceObject {
  static type = VERSION_HISTORY;

  @deserialize
  _links: {
    self: HALLink;
    versions: HALLink;
    draftVersion: HALLink;
  };

  /**
   * The identifier of this Version History
   */
  @autoserialize
  id: string;

  /**
   * The summary of this Version History
   */
  @autoserialize
  summary: string;

  /**
   * The name of the submitter of this Version History
   */
  @autoserialize
  submitterName: string;

  /**
   * Whether exist a workspace item
   */
  @autoserialize
  draftVersion: boolean;

  /**
   * The list of versions within this history
   */
  @excludeFromEquals
  @link(VERSION, true)
  versions: Observable<RemoteData<PaginatedList<Version>>>;
}
