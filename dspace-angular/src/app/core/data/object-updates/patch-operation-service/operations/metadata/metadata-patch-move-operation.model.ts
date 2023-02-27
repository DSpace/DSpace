import { MetadataPatchOperation } from './metadata-patch-operation.model';
import { Operation } from 'fast-json-patch';

/**
 * Wrapper object for a metadata patch move Operation
 */
export class MetadataPatchMoveOperation extends MetadataPatchOperation {
  static operationType = 'move';

  /**
   * The original place of the metadata value to move
   */
  from: number;

  /**
   * The new place to move the metadata value to
   */
  to: number;

  constructor(field: string, from: number, to: number) {
    super(MetadataPatchMoveOperation.operationType, field);
    this.from = from;
    this.to = to;
  }

  /**
   * Transform the MetadataPatchOperation into a fast-json-patch Operation by constructing its path and other properties
   * using the information provided.
   */
  toOperation(): Operation {
    return { op: this.op as any, from: `/metadata/${this.field}/${this.from}`, path: `/metadata/${this.field}/${this.to}` };
  }
}
