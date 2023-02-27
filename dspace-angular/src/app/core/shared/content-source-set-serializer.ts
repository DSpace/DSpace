import { isEmpty } from '../../shared/empty.util';

/**
 * Serializer to create convert the 'all' value supported by the server to an empty string and vice versa.
 */
export class ContentSourceSetSerializer {

  /**
   * Method to serialize a setId
   * @param {string} setId
   * @returns {string} the provided set ID, unless when an empty set ID is provided. In that case, 'all' will be returned.
   */
  Serialize(setId: string): any {
    if (isEmpty(setId)) {
      return 'all';
    }
    return setId;
  }

  /**
   * Method to deserialize a setId
   * @param {string} setId
   * @returns {string} the provided set ID. When 'all' is provided, an empty set ID will be returned.
   */
  Deserialize(setId: string): string {
    if (setId === 'all') {
      return '';
    }
    return setId;
  }
}
