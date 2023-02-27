import { autoserialize } from 'cerialize';

/**
 * Identifier model. Identifiers using this model are returned in lists from the /item/{id}/identifiers endpoint
 *
 * @author Kim Shepherd
 */
export class Identifier {
  /**
   * The value of the identifier, eg. http://hdl.handle.net/123456789/123 or https://doi.org/test/doi/1234
   */
  @autoserialize
  value: string;
  /**
   * The type of identiifer, eg. "doi", or "handle", or "other"
   */
  @autoserialize
  identifierType: string;
  /**
   * The status of the identifier. Some schemes, like DOI, will have a different status based on whether it is
   * queued for remote registration, reservation, or update, or has been registered, simply minted locally, etc.
   */
  @autoserialize
  identifierStatus: string;
  /**
   * The type of resource, in this case Identifier
   */
  @autoserialize
  type: string;
}
