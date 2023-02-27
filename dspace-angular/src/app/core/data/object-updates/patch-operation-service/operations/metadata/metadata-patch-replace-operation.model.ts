import { MetadataPatchOperation } from './metadata-patch-operation.model';
import { Operation } from 'fast-json-patch';

/**
 * Wrapper object for a metadata patch replace Operation
 */
export class MetadataPatchReplaceOperation extends MetadataPatchOperation {
  static operationType = 'replace';

  /**
   * The place of the metadata value within its field to modify
   */
  place: number;

  /**
   * The new value to replace the metadata with
   */
  value: any;

  constructor(field: string, place: number, value: any) {
    super(MetadataPatchReplaceOperation.operationType, field);
    this.place = place;
    this.value = value;
  }

  /**
   * Transform the MetadataPatchOperation into a fast-json-patch Operation by constructing its path and other properties
   * using the information provided.
   */
  toOperation(): Operation {
    return { op: this.op as any, path: `/metadata/${this.field}/${this.place}`, value: this.value };
  }
}
