import { Operation } from 'fast-json-patch';
import { FieldUpdates } from '../field-updates.model';

/**
 * Interface for a service dealing with the transformations of patch operations from the object-updates store
 * The implementations of this service know how to deal with the fields of a FieldUpdate and how to transform them
 * into patch Operations.
 */
export interface PatchOperationService {
  /**
   * Transform a {@link FieldUpdates} object into an array of fast-json-patch Operations
   * @param fieldUpdates
   */
  fieldUpdatesToPatchOperations(fieldUpdates: FieldUpdates): Operation[];
}
