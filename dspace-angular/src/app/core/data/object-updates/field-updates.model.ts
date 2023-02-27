import { FieldUpdate } from './field-update.model';

/**
 * The states of all field updates available for a single page, mapped by uuid
 */
export interface FieldUpdates {
  [uuid: string]: FieldUpdate;
}
