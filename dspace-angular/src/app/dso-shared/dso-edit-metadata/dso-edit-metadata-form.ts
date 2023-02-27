/* eslint-disable max-classes-per-file */
import { MetadataMap, MetadataValue } from '../../core/shared/metadata.models';
import { hasNoValue, hasValue, isEmpty, isNotEmpty } from '../../shared/empty.util';
import { MoveOperation, Operation } from 'fast-json-patch';
import { MetadataPatchReplaceOperation } from '../../core/data/object-updates/patch-operation-service/operations/metadata/metadata-patch-replace-operation.model';
import { MetadataPatchRemoveOperation } from '../../core/data/object-updates/patch-operation-service/operations/metadata/metadata-patch-remove-operation.model';
import { MetadataPatchAddOperation } from '../../core/data/object-updates/patch-operation-service/operations/metadata/metadata-patch-add-operation.model';
import { ArrayMoveChangeAnalyzer } from '../../core/data/array-move-change-analyzer.service';
import { MetadataPatchMoveOperation } from '../../core/data/object-updates/patch-operation-service/operations/metadata/metadata-patch-move-operation.model';

/**
 * Enumeration for the type of change occurring on a metadata value
 */
export enum DsoEditMetadataChangeType {
  UPDATE = 1,
  ADD = 2,
  REMOVE = 3
}

/**
 * Class holding information about a metadata value and its changes within an edit form
 */
export class DsoEditMetadataValue {
  /**
   * The original metadata value (should stay the same!) used to compare changes with
   */
  originalValue: MetadataValue;

  /**
   * The new value, dynamically changing
   */
  newValue: MetadataValue;

  /**
   * A value that can be used to undo any discarding that took place
   */
  reinstatableValue: MetadataValue;

  /**
   * Whether or not this value is currently being edited or not
   */
  editing = false;

  /**
   * The type of change that's taking place on this metadata value
   * Empty if no changes are made
   */
  change: DsoEditMetadataChangeType;

  /**
   * A flag to keep track if the value has been reordered (place has changed)
   */
  reordered = false;

  /**
   * A type or change that can be used to undo any discarding that took place
   */
  reinstatableChange: DsoEditMetadataChangeType;

  constructor(value: MetadataValue, added = false) {
    this.originalValue = value;
    this.newValue = Object.assign(new MetadataValue(), value);
    if (added) {
      this.change = DsoEditMetadataChangeType.ADD;
      this.editing = true;
    }
  }

  /**
   * Save the current changes made to the metadata value
   * This will set the type of change to UPDATE if the new metadata value's value and/or language are different from
   * the original value
   * It will also set the editing flag to false
   */
  confirmChanges(finishEditing = false) {
    this.reordered = this.originalValue.place !== this.newValue.place;
    if (hasNoValue(this.change) || this.change === DsoEditMetadataChangeType.UPDATE) {
      if ((this.originalValue.value !== this.newValue.value || this.originalValue.language !== this.newValue.language)) {
        this.change = DsoEditMetadataChangeType.UPDATE;
      } else {
        this.change = undefined;
      }
    }
    if (finishEditing) {
      this.editing = false;
    }
  }

  /**
   * Returns if the current value contains changes or not
   * If the metadata value contains changes, but they haven't been confirmed yet through confirmChanges(), this might
   * return false (which is desired)
   */
  hasChanges(): boolean {
    return hasValue(this.change) || this.reordered;
  }

  /**
   * Discard the current changes and mark the value and change type re-instatable by storing them in their relevant
   * properties
   */
  discardAndMarkReinstatable(): void {
    if (this.change === DsoEditMetadataChangeType.UPDATE || this.reordered) {
      this.reinstatableValue = this.newValue;
    }
    this.reinstatableChange = this.change;
    this.discard(false);
  }

  /**
   * Discard the current changes
   * Call discardAndMarkReinstatable() instead, if the discard should be re-instatable
   */
  discard(keepPlace = true): void {
    this.change = undefined;
    const place = this.newValue.place;
    this.newValue = Object.assign(new MetadataValue(), this.originalValue);
    if (keepPlace) {
      this.newValue.place = place;
    }
    this.confirmChanges(true);
  }

  /**
   * Re-instate (undo) the last discard by replacing the value and change type with their reinstate properties (if present)
   */
  reinstate(): void {
    if (hasValue(this.reinstatableValue)) {
      this.newValue = this.reinstatableValue;
      this.reinstatableValue = undefined;
    }
    if (hasValue(this.reinstatableChange)) {
      this.change = this.reinstatableChange;
      this.reinstatableChange = undefined;
    }
    this.confirmChanges();
  }

  /**
   * Returns if either the value or change type have a re-instatable property
   * This will be the case if a discard has taken place that undid changes to the value or type
   */
  isReinstatable(): boolean {
    return hasValue(this.reinstatableValue) || hasValue(this.reinstatableChange);
  }

  /**
   * Reset the state of the re-instatable properties
   */
  resetReinstatable() {
    this.reinstatableValue = undefined;
    this.reinstatableChange = undefined;
  }
}

/**
 * Class holding information about the metadata of a DSpaceObject and its changes within an edit form
 */
export class DsoEditMetadataForm {
  /**
   * List of original metadata field keys (before any changes took place)
   */
  originalFieldKeys: string[];

  /**
   * List of current metadata field keys (includes new fields for values added by the user)
   */
  fieldKeys: string[];

  /**
   * Current state of the form
   * Key: Metadata field
   * Value: List of {@link DsoEditMetadataValue}s for the metadata field
   */
  fields: {
    [mdField: string]: DsoEditMetadataValue[],
  };

  /**
   * A map of previously added metadata values before a discard of the form took place
   * This can be used to re-instate the entire form to before the discard taking place
   */
  reinstatableNewValues: {
    [mdField: string]: DsoEditMetadataValue[],
  };

  /**
   * A (temporary) new metadata value added by the user, not belonging to a metadata field yet
   * This value will be finalised and added to a field using setMetadataField()
   */
  newValue: DsoEditMetadataValue;

  constructor(metadata: MetadataMap) {
    this.originalFieldKeys = [];
    this.fieldKeys = [];
    this.fields = {};
    this.reinstatableNewValues = {};
    Object.entries(metadata).forEach(([mdField, values]: [string, MetadataValue[]]) => {
      this.originalFieldKeys.push(mdField);
      this.fieldKeys.push(mdField);
      this.setValuesForFieldSorted(mdField, values.map((value: MetadataValue) => new DsoEditMetadataValue(value)));
    });
    this.sortFieldKeys();
  }

  /**
   * Add a new temporary value for the user to edit
   */
  add(): void {
    if (hasNoValue(this.newValue)) {
      this.newValue = new DsoEditMetadataValue(new MetadataValue(), true);
    }
  }

  /**
   * Add the temporary value to a metadata field
   * Clear the temporary value afterwards
   * @param mdField
   */
  setMetadataField(mdField: string): void {
    this.newValue.editing = false;
    this.addValueToField(this.newValue, mdField);
    // Set the place property to match the new value's position within its field
    const place = this.fields[mdField].length - 1;
    this.fields[mdField][place].originalValue.place = place;
    this.fields[mdField][place].newValue.place = place;
    this.newValue = undefined;
  }

  /**
   * Add a value to a metadata field within the map
   * @param value
   * @param mdField
   * @private
   */
  private addValueToField(value: DsoEditMetadataValue, mdField: string): void {
    if (isEmpty(this.fields[mdField])) {
      this.fieldKeys.push(mdField);
      this.sortFieldKeys();
      this.fields[mdField] = [];
    }
    this.fields[mdField].push(value);
  }

  /**
   * Remove a value from a metadata field on a given index (this actually removes the value, not just marking it deleted)
   * @param mdField
   * @param index
   */
  remove(mdField: string, index: number): void {
    if (isNotEmpty(this.fields[mdField])) {
      this.fields[mdField].splice(index, 1);
      if (this.fields[mdField].length === 0) {
        this.fieldKeys.splice(this.fieldKeys.indexOf(mdField), 1);
        delete this.fields[mdField];
      }
    }
  }

  /**
   * Returns if at least one value within the form contains a change
   */
  hasChanges(): boolean {
    return Object.values(this.fields).some((values: DsoEditMetadataValue[]) => values.some((value: DsoEditMetadataValue) => value.hasChanges()));
  }

  /**
   * Check if a metadata field contains changes within its order (place property of values)
   * @param mdField
   */
  hasOrderChanges(mdField: string): boolean {
    return this.fields[mdField].some((value: DsoEditMetadataValue) => value.originalValue.place !== value.newValue.place);
  }

  /**
   * Discard all changes within the form and store their current values within re-instatable properties so they can be
   * undone afterwards
   */
  discard(): void {
    this.resetReinstatable();
    // Discard changes from each value from each field
    Object.entries(this.fields).forEach(([field, values]: [string, DsoEditMetadataValue[]]) => {
      let removeFromIndex = -1;
      values.forEach((value: DsoEditMetadataValue, index: number) => {
        if (value.change === DsoEditMetadataChangeType.ADD) {
          if (isEmpty(this.reinstatableNewValues[field])) {
            this.reinstatableNewValues[field] = [];
          }
          this.reinstatableNewValues[field].push(value);
          if (removeFromIndex === -1) {
            removeFromIndex = index;
          }
        } else {
          value.discardAndMarkReinstatable();
        }
      });
      if (removeFromIndex > -1) {
        this.fields[field].splice(removeFromIndex, this.fields[field].length - removeFromIndex);
      }
    });
    // Delete new metadata fields
    this.fieldKeys.forEach((field: string) => {
      if (this.originalFieldKeys.indexOf(field) < 0) {
        delete this.fields[field];
      }
    });
    this.fieldKeys = [...this.originalFieldKeys];
    this.sortFieldKeys();
    // Reset the order of values within their fields to match their place property
    this.fieldKeys.forEach((field: string) => {
      this.setValuesForFieldSorted(field, this.fields[field]);
    });
  }

  /**
   * Reset the order of values within a metadata field to their original places
   * Update the actual array to match the place properties
   * @param mdField
   */
  resetOrder(mdField: string) {
    this.fields[mdField].forEach((value: DsoEditMetadataValue) => {
      value.newValue.place = value.originalValue.place;
      value.confirmChanges();
    });
    this.setValuesForFieldSorted(mdField, this.fields[mdField]);
  }

  /**
   * Sort fieldKeys alphabetically
   * Should be called whenever a field is added to ensure the alphabetical order is kept
   */
  sortFieldKeys() {
    this.fieldKeys.sort((a: string, b: string) => a.localeCompare(b));
  }

  /**
   * Undo any previously discarded changes
   */
  reinstate(): void {
    // Reinstate each value
    Object.values(this.fields).forEach((values: DsoEditMetadataValue[]) => {
      values.forEach((value: DsoEditMetadataValue) => {
        value.reinstate();
      });
    });
    // Re-add new values
    Object.entries(this.reinstatableNewValues).forEach(([field, values]: [string, DsoEditMetadataValue[]]) => {
      values.forEach((value: DsoEditMetadataValue) => {
        this.addValueToField(value, field);
      });
    });
    // Reset the order of values within their fields to match their place property
    this.fieldKeys.forEach((field: string) => {
      this.setValuesForFieldSorted(field, this.fields[field]);
    });
    this.reinstatableNewValues = {};
  }

  /**
   * Returns if at least one value contains a re-instatable property, meaning a discard can be reversed
   */
  isReinstatable(): boolean {
    return isNotEmpty(this.reinstatableNewValues) ||
      Object.values(this.fields)
        .some((values: DsoEditMetadataValue[]) => values
          .some((value: DsoEditMetadataValue) => value.isReinstatable()));
  }

  /**
   * Reset the state of the re-instatable properties and values
   */
  resetReinstatable(): void {
    this.reinstatableNewValues = {};
    Object.values(this.fields).forEach((values: DsoEditMetadataValue[]) => {
      values.forEach((value: DsoEditMetadataValue) => {
        value.resetReinstatable();
      });
    });
  }

  /**
   * Set the values of a metadata field and sort them by their newValue's place property
   * @param mdField
   * @param values
   */
  private setValuesForFieldSorted(mdField: string, values: DsoEditMetadataValue[]) {
    this.fields[mdField] = values.sort((a: DsoEditMetadataValue, b: DsoEditMetadataValue) => a.newValue.place - b.newValue.place);
  }

  /**
   * Get the json PATCH operations for the current changes within this form
   * For each metadata field, it'll return operations in the following order: replace, remove (from last to first place), add and move
   * This order is important, as each operation is executed in succession of the previous one
   */
  getOperations(moveAnalyser: ArrayMoveChangeAnalyzer<number>): Operation[] {
    const operations: Operation[] = [];
    Object.entries(this.fields).forEach(([field, values]: [string, DsoEditMetadataValue[]]) => {
      const replaceOperations: MetadataPatchReplaceOperation[] = [];
      const removeOperations: MetadataPatchRemoveOperation[] = [];
      const addOperations: MetadataPatchAddOperation[] = [];
      [...values]
        .sort((a: DsoEditMetadataValue, b: DsoEditMetadataValue) => a.originalValue.place - b.originalValue.place)
        .forEach((value: DsoEditMetadataValue) => {
          if (hasValue(value.change)) {
            if (value.change === DsoEditMetadataChangeType.UPDATE) {
              // Only changes to value or language are considered "replace" operations. Changes to place are considered "move", which is processed below.
              if (value.originalValue.value !== value.newValue.value || value.originalValue.language !== value.newValue.language) {
                replaceOperations.push(new MetadataPatchReplaceOperation(field, value.originalValue.place, {
                  value: value.newValue.value,
                  language: value.newValue.language,
                }));
              }
            } else if (value.change === DsoEditMetadataChangeType.REMOVE) {
              removeOperations.push(new MetadataPatchRemoveOperation(field, value.originalValue.place));
            } else if (value.change === DsoEditMetadataChangeType.ADD) {
              addOperations.push(new MetadataPatchAddOperation(field, {
                value: value.newValue.value,
                language: value.newValue.language,
              }));
            } else {
              console.warn('Illegal metadata change state detected for', value);
            }
          }
        });

      operations.push(...replaceOperations
        .map((operation: MetadataPatchReplaceOperation) => operation.toOperation()));
      operations.push(...removeOperations
        // Sort remove operations backwards first, because they get executed in order. This avoids one removal affecting the next.
        .sort((a: MetadataPatchRemoveOperation, b: MetadataPatchRemoveOperation) => b.place - a.place)
        .map((operation: MetadataPatchRemoveOperation) => operation.toOperation()));
      operations.push(...addOperations
        .map((operation: MetadataPatchAddOperation) => operation.toOperation()));
    });
    // Calculate and add the move operations that need to happen in order to move value from their old place to their new within the field
    // This uses an ArrayMoveChangeAnalyzer
    Object.entries(this.fields).forEach(([field, values]: [string, DsoEditMetadataValue[]]) => {
      // Exclude values marked for removal, because operations are executed in order (remove first, then move)
      const valuesWithoutRemoved = values.filter((value: DsoEditMetadataValue) => value.change !== DsoEditMetadataChangeType.REMOVE);
      const moveOperations = moveAnalyser
        .diff(
          [...valuesWithoutRemoved]
            .sort((a: DsoEditMetadataValue, b: DsoEditMetadataValue) => a.originalValue.place - b.originalValue.place)
            .map((value: DsoEditMetadataValue) => value.originalValue.place),
          [...valuesWithoutRemoved]
            .sort((a: DsoEditMetadataValue, b: DsoEditMetadataValue) => a.newValue.place - b.newValue.place)
            .map((value: DsoEditMetadataValue) => value.originalValue.place))
        .map((operation: MoveOperation) => new MetadataPatchMoveOperation(field, +operation.from.substr(1), +operation.path.substr(1)).toOperation());
      operations.push(...moveOperations);
    });
    return operations;
  }
}
