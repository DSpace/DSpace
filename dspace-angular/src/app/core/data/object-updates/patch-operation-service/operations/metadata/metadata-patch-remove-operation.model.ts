import { MetadataPatchOperation } from './metadata-patch-operation.model';
import { Operation } from 'fast-json-patch';

/**
 * Wrapper object for a metadata patch remove Operation
 */
export class MetadataPatchRemoveOperation extends MetadataPatchOperation {
  static operationType = 'remove';

  /**
   * The place of the metadata value to remove within its field
   */
  place: number;

  constructor(field: string, place: number) {
    super(MetadataPatchRemoveOperation.operationType, field);
    this.place = place;
  }

  /**
   * Transform the MetadataPatchOperation into a fast-json-patch Operation by constructing its path and other properties
   * using the information provided.
   */
  toOperation(): Operation {
    return { op: this.op as any, path: `/metadata/${this.field}/${this.place}` };
  }
}
