import { deserialize, inheritSerialization } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../cache/builders/build-decorators';
import { PaginatedList } from '../data/paginated-list.model';
import { RemoteData } from '../data/remote-data';
import { Bitstream } from './bitstream.model';
import { BITSTREAM } from './bitstream.resource-type';
import { COLLECTION } from './collection.resource-type';
import { DSpaceObject } from './dspace-object.model';
import { HALLink } from './hal-link.model';
import { License } from './license.model';
import { LICENSE } from './license.resource-type';
import { ResourcePolicy } from '../resource-policy/models/resource-policy.model';
import { RESOURCE_POLICY } from '../resource-policy/models/resource-policy.resource-type';
import { COMMUNITY } from './community.resource-type';
import { Community } from './community.model';
import { ChildHALResource } from './child-hal-resource.model';
import { HandleObject } from './handle-object.model';

@typedObject
@inheritSerialization(DSpaceObject)
export class Collection extends DSpaceObject implements ChildHALResource, HandleObject {
  static type = COLLECTION;

  /**
   * The {@link HALLink}s for this Collection
   */
  @deserialize
  _links: {
    license: HALLink;
    harvester: HALLink;
    mappedItems: HALLink;
    itemtemplate: HALLink;
    defaultAccessConditions: HALLink;
    logo: HALLink;
    parentCommunity: HALLink;
    workflowGroups: HALLink[];
    adminGroup: HALLink;
    submittersGroup: HALLink;
    itemReadGroup: HALLink;
    bitstreamReadGroup: HALLink;
    self: HALLink;
  };

  /**
   * The license for this Collection
   * Will be undefined unless the license {@link HALLink} has been resolved.
   */
  @link(LICENSE)
  license?: Observable<RemoteData<License>>;

  /**
   * The logo for this Collection
   * Will be undefined unless the logo {@link HALLink} has been resolved.
   */
  @link(BITSTREAM)
  logo?: Observable<RemoteData<Bitstream>>;

  /**
   * The default access conditions for this Collection
   * Will be undefined unless the defaultAccessConditions {@link HALLink} has been resolved.
   */
  @link(RESOURCE_POLICY, true)
  defaultAccessConditions?: Observable<RemoteData<PaginatedList<ResourcePolicy>>>;

  /**
   * The Community that is a direct parent of this Collection
   * Will be undefined unless the parent community HALLink has been resolved.
   */
  @link(COMMUNITY, false)
  parentCommunity?: Observable<RemoteData<Community>>;

  /**
   * A string representing the unique handle of this Collection
   */
  get handle(): string {
    return this.firstMetadataValue('dc.identifier.uri');
  }

  /**
   * The introductory text of this Collection
   * Corresponds to the metadata field dc.description
   */
  get introductoryText(): string {
    return this.firstMetadataValue('dc.description');
  }

  /**
   * The short description: HTML
   * Corresponds to the metadata field dc.description.abstract
   */
  get shortDescription(): string {
    return this.firstMetadataValue('dc.description.abstract');
  }

  /**
   * The copyright text of this Collection
   * Corresponds to the metadata field dc.rights
   */
  get copyrightText(): string {
    return this.firstMetadataValue('dc.rights');
  }

  /**
   * The license of this Collection
   * Corresponds to the metadata field dc.rights.license
   */
  get dcLicense(): string {
    return this.firstMetadataValue('dc.rights.license');
  }

  /**
   * The sidebar text of this Collection
   * Corresponds to the metadata field dc.description.tableofcontents
   */
  get sidebarText(): string {
    return this.firstMetadataValue('dc.description.tableofcontents');
  }

  getParentLinkKey(): keyof this['_links'] {
    return 'parentCommunity';
  }
}
