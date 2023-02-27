import { Identifiable } from './identifiable.model';
import { FieldChangeType } from './field-change-type.model';

/**
 * The state of a single field update
 */
export interface FieldUpdate {
  field: Identifiable;
  changeType: FieldChangeType;
}
