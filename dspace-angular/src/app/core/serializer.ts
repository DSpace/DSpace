/**
 * A Serializer turns responses from the backend to models
 * and vice versa
 */
export interface Serializer<T> {

  /**
   * Convert a model in to the format expected by the backend
   *
   * @param model The model to serialize
   * @returns An object to send to the backend
   */
  serialize(model: T): any;

  /**
   * Convert an array of models in to the format expected by the backend
   *
   * @param models The array of models to serialize
   * @returns An object to send to the backend
   */
  serializeArray(models: T[]): any;

  /**
   * Convert a response from the backend in to a model.
   *
   * @param response An object returned by the backend
   * @returns a model of type T
   */
  deserialize(response: any): T;

  /**
   * Convert a response from the backend in to an array of models
   *
   * @param response An object returned by the backend
   * @returns an array of models of type T
   */
  deserializeArray(response: any): T[];
}
