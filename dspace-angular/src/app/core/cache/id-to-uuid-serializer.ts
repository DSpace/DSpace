import { hasValue } from '../../shared/empty.util';

/**
 * Serializer to create unique fake UUID's from id's that might otherwise be the same across multiple object types
 */
export class IDToUUIDSerializer {
  /**
   * @param {string} prefix To prepend the original ID with
   */
  constructor(private prefix: string) {
  }

  /**
   * Method to serialize a UUID
   * @param {string} uuid
   * @returns {any} undefined Fake UUID's should not be sent back to the server, but only be used in the UI
   */
  Serialize(uuid: string): any {
    return undefined;
  }

  /**
   * Method to deserialize a UUID
   * @param {string} id Identifier to transform in to a UUID
   * @returns {string} UUID based on the prefix and the given id
   */
  Deserialize(id: string): string {
    if (hasValue(id)) {
      return `${this.prefix}-${id}`;
    } else {
      return id;
    }

  }
}
