import { Deserialize, Serialize } from 'cerialize';

import { Serializer } from '../serializer';
import { GenericConstructor } from '../shared/generic-constructor';

/**
 * This Serializer turns responses from DSpace's REST API
 * to models and vice versa
 */
export class DSpaceSerializer<T> implements Serializer<T> {

  /**
   * Create a new DSpaceSerializer instance
   *
   * @param modelType a class or interface to indicate
   * the kind of model this serializer should work with
   */
  constructor(private modelType: GenericConstructor<T>) {
  }

  /**
   * Convert a model in to the format expected by the backend
   *
   * @param model The model to serialize
   * @returns An object to send to the backend
   */
  serialize(model: T): any {
    return Serialize(model, this.modelType);
  }

  /**
   * Convert an array of models in to the format expected by the backend
   *
   * @param models The array of models to serialize
   * @returns An object to send to the backend
   */
  serializeArray(models: T[]): any {
    return Serialize(models, this.modelType);
  }

  /**
   * Convert a response from the backend in to a model.
   *
   * @param response An object returned by the backend
   * @returns a model of type T
   */
  deserialize(response: any): T {
    if (Array.isArray(response)) {
      throw new Error('Expected a single model, use deserializeArray() instead');
    }
    return Deserialize(response, this.modelType) as T;
  }

  /**
   * Convert a response from the backend in to an array of models
   *
   * @param response An object returned by the backend
   * @returns an array of models of type T
   */
  deserializeArray(response: any): T[] {
    if (!Array.isArray(response)) {
      throw new Error('Expected an Array, use deserialize() instead');
    }
    return Deserialize(response, this.modelType) as T[];
  }
}
