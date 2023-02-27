import { deserialize, inheritSerialization } from 'cerialize';

import { Observable } from 'rxjs';

import { link, typedObject } from '../cache/builders/build-decorators';
import { BUNDLE } from './bundle.resource-type';
import { DSpaceObject } from './dspace-object.model';
import { HALLink } from './hal-link.model';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { BITSTREAM } from './bitstream.resource-type';
import { Bitstream } from './bitstream.model';
import { ITEM } from './item.resource-type';
import { Item } from './item.model';

@typedObject
@inheritSerialization(DSpaceObject)
export class Bundle extends DSpaceObject {
  static type = BUNDLE;

  /**
   * The {@link HALLink}s for this Bundle
   */
  @deserialize
  _links: {
    self: HALLink;
    primaryBitstream: HALLink;
    bitstreams: HALLink;
    item: HALLink;
  };

  /**
   * The primary Bitstream of this Bundle
   * Will be undefined unless the primaryBitstream {@link HALLink} has been resolved.
   */
  @link(BITSTREAM)
  primaryBitstream?: Observable<RemoteData<Bitstream>>;

  /**
   * The list of Bitstreams that are direct children of this Bundle
   * Will be undefined unless the bitstreams {@link HALLink} has been resolved.
   */
  @link(BITSTREAM, true)
  bitstreams?: Observable<RemoteData<PaginatedList<Bitstream>>>;

    /**
     * The owning item for this Bundle
     * Will be undefined unless the Item{@link HALLink} has been resolved.
     */
  @link(ITEM)
  item?: Observable<RemoteData<Item>>;
}
