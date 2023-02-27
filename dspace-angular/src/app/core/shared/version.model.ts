import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { Item } from './item.model';
import { RemoteData } from '../data/remote-data';
import { Observable } from 'rxjs';
import { VersionHistory } from './version-history.model';
import { EPerson } from '../eperson/models/eperson.model';
import { VERSION } from './version.resource-type';
import { HALLink } from './hal-link.model';
import { link, typedObject } from '../cache/builders/build-decorators';
import { VERSION_HISTORY } from './version-history.resource-type';
import { ITEM } from './item.resource-type';
import { EPERSON } from '../eperson/models/eperson.resource-type';
import { DSpaceObject } from './dspace-object.model';

/**
 * Class representing a DSpace Version
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class Version extends DSpaceObject {
  static type = VERSION;

  @deserialize
  _links: {
    self: HALLink;
    item: HALLink;
    versionhistory: HALLink;
    eperson: HALLink;
  };

  /**
   * The identifier of this Version
   */
  @autoserialize
  id: string;

  /**
   * The version number of the version's history this version represents
   */
  @autoserialize
  version: number;

  /**
   * The summary for the changes made in this version
   */
  @autoserialize
  summary: string;

  /**
   * The name of the submitter of this version
   */
  @autoserialize
  submitterName: string;

  /**
   * The Date this version was created
   */
  @deserialize
  created: Date;

  /**
   * The full version history this version is apart of
   */
  @excludeFromEquals
  @link(VERSION_HISTORY)
  versionhistory: Observable<RemoteData<VersionHistory>>;

  /**
   * The item this version represents
   */
  @excludeFromEquals
  @link(ITEM)
  item: Observable<RemoteData<Item>>;

  /**
   * The e-person who created this version
   */
  @excludeFromEquals
  @link(EPERSON)
  eperson: Observable<RemoteData<EPerson>>;
}
