import { PatchOperationService } from './patch-operation.service';
import { MetadatumViewModel } from '../../../shared/metadata.models';
import { Operation } from 'fast-json-patch';
import { Injectable } from '@angular/core';
import { MetadataPatchOperation } from './operations/metadata/metadata-patch-operation.model';
import { hasValue } from '../../../../shared/empty.util';
import { MetadataPatchAddOperation } from './operations/metadata/metadata-patch-add-operation.model';
import { MetadataPatchRemoveOperation } from './operations/metadata/metadata-patch-remove-operation.model';
import { MetadataPatchReplaceOperation } from './operations/metadata/metadata-patch-replace-operation.model';
import { FieldUpdates } from '../field-updates.model';
import { FieldChangeType } from '../field-change-type.model';

/**
 * Service transforming {@link FieldUpdates} into {@link Operation}s for metadata values
 * This expects the fields within every {@link FieldUpdate} to be {@link MetadatumViewModel}s
 */
@Injectable({
  providedIn: 'root'
})
export class MetadataPatchOperationService implements PatchOperationService {

  /**
   * Transform a {@link FieldUpdates} object into an array of fast-json-patch Operations for metadata values
   * This method first creates an array of {@link MetadataPatchOperation} wrapper operations, which are then
   * iterated over to create the actual patch operations. While iterating, it has the ability to check for previous
   * operations that would modify the operation's position and act accordingly.
   * @param fieldUpdates
   */
  fieldUpdatesToPatchOperations(fieldUpdates: FieldUpdates): Operation[] {
    const metadataPatch = this.fieldUpdatesToMetadataPatchOperations(fieldUpdates);

    // This map stores what metadata fields had a value deleted at which places
    // This is used to modify the place of operations to match previous operations
    const metadataRemoveMap = new Map<string, number[]>();
    const patch = [];
    metadataPatch.forEach((operation) => {
      // If this operation is removing or editing an existing value, first check the map for previous operations
      // If the map contains remove operations before this operation's place, lower the place by 1 for each
      if ((operation.op === MetadataPatchRemoveOperation.operationType || operation.op === MetadataPatchReplaceOperation.operationType) && hasValue((operation as any).place)) {
        if (metadataRemoveMap.has(operation.field)) {
          metadataRemoveMap.get(operation.field).forEach((index) => {
            if (index < (operation as any).place) {
              (operation as any).place--;
            }
          });
        }
      }

      // If this is a remove operation, add its (updated) place to the map, so we can adjust following operations accordingly
      if (operation.op === MetadataPatchRemoveOperation.operationType && hasValue((operation as any).place)) {
        if (!metadataRemoveMap.has(operation.field)) {
          metadataRemoveMap.set(operation.field, []);
        }
        metadataRemoveMap.get(operation.field).push((operation as any).place);
      }

      // Transform the updated operation into a fast-json-patch Operation and add it to the patch
      patch.push(operation.toOperation());
    });

    return patch;
  }

  /**
   * Transform a {@link FieldUpdates} object into an array of {@link MetadataPatchOperation} wrapper objects
   * These wrapper objects contain detailed information about the patch operation that needs to be creates for each update
   * This information can then be modified before creating the actual patch
   * @param fieldUpdates
   */
  fieldUpdatesToMetadataPatchOperations(fieldUpdates: FieldUpdates): MetadataPatchOperation[] {
    const metadataPatch = [];

    Object.keys(fieldUpdates).forEach((uuid) => {
      const update = fieldUpdates[uuid];
      const metadatum = update.field as MetadatumViewModel;
      const val = {
        value: metadatum.value,
        language: metadatum.language
      };

      let operation: MetadataPatchOperation;
      switch (update.changeType) {
        case FieldChangeType.ADD:
          operation = new MetadataPatchAddOperation(metadatum.key, [ val ]);
          break;
        case FieldChangeType.REMOVE:
          operation = new MetadataPatchRemoveOperation(metadatum.key, metadatum.place);
          break;
        case FieldChangeType.UPDATE:
          operation = new MetadataPatchReplaceOperation(metadatum.key, metadatum.place, val);
          break;
      }

      metadataPatch.push(operation);
    });

    return metadataPatch;
  }

}
