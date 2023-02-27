import { MetadataPatchOperation } from './metadata-patch-operation.model';
import { Operation } from 'fast-json-patch';

/**
 * Wrapper object for a metadata patch add Operation
 */
export class MetadataPatchAddOperation extends MetadataPatchOperation {
  static operationType = 'add';

  /**
   * The metadata value(s) to add to the field
   */
  value: any;

  constructor(field: string, value: any) {
    super(MetadataPatchAddOperation.operationType, field);
    this.value = value;
  }

  /**
   * Transform the MetadataPatchOperation into a fast-json-patch Operation by constructing its path and other properties
   * using the information provided.
   */
  toOperation(): Operation {
    return { op: this.op as any, path: `/metadata/${this.field}/-`, value: this.value };
  }
}
