import { Operation } from 'fast-json-patch';

/**
 * Wrapper object for metadata patch Operations
 * It should contain at least the operation type and metadata field. An abstract method to transform this object
 * into a fast-json-patch Operation is defined in each instance extending from this.
 */
export abstract class MetadataPatchOperation {
  /**
   * The operation to perform
   */
  op: string;

  /**
   * The metadata field this operation is intended for
   */
  field: string;

  constructor(op: string, field: string) {
    this.op = op;
    this.field = field;
  }

  /**
   * Transform the MetadataPatchOperation into a fast-json-patch Operation by constructing its path and other properties
   * using the information provided.
   */
  abstract toOperation(): Operation;
}
