import { MetadataPatchOperationService } from './metadata-patch-operation.service';
import { Operation } from 'fast-json-patch';
import { MetadatumViewModel } from '../../../shared/metadata.models';
import { FieldUpdates } from '../field-updates.model';
import { FieldChangeType } from '../field-change-type.model';

describe('MetadataPatchOperationService', () => {
  let service: MetadataPatchOperationService;

  beforeEach(() => {
    service = new MetadataPatchOperationService();
  });

  describe('fieldUpdatesToPatchOperations', () => {
    let fieldUpdates: FieldUpdates;
    let expected: Operation[];
    let result: Operation[];

    describe('when fieldUpdates contains a single remove', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Deleted title',
              place: 0
            }),
            changeType: FieldChangeType.REMOVE
          }
        });
        expected = [
          { op: 'remove', path: '/metadata/dc.title/0' }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain a single remove operation with the correct path', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains a single add', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Added title',
              place: 0
            }),
            changeType: FieldChangeType.ADD
          }
        });
        expected = [
          { op: 'add', path: '/metadata/dc.title/-', value: [{ value: 'Added title', language: undefined }] }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain a single add operation with the correct path', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains a single update', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Changed title',
              place: 0
            }),
            changeType: FieldChangeType.UPDATE
          }
        });
        expected = [
          { op: 'replace', path: '/metadata/dc.title/0', value: { value: 'Changed title', language: undefined } }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain a single replace operation with the correct path', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains multiple removes with incrementing indexes', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'First deleted title',
              place: 0
            }),
            changeType: FieldChangeType.REMOVE
          },
          update2: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Second deleted title',
              place: 1
            }),
            changeType: FieldChangeType.REMOVE
          },
          update3: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Third deleted title',
              place: 2
            }),
            changeType: FieldChangeType.REMOVE
          }
        });
        expected = [
          { op: 'remove', path: '/metadata/dc.title/0' },
          { op: 'remove', path: '/metadata/dc.title/0' },
          { op: 'remove', path: '/metadata/dc.title/0' }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain all the remove operations on the same index', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains multiple removes with decreasing indexes', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Third deleted title',
              place: 2
            }),
            changeType: FieldChangeType.REMOVE
          },
          update2: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Second deleted title',
              place: 1
            }),
            changeType: FieldChangeType.REMOVE
          },
          update3: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'First deleted title',
              place: 0
            }),
            changeType: FieldChangeType.REMOVE
          }
        });
        expected = [
          { op: 'remove', path: '/metadata/dc.title/2' },
          { op: 'remove', path: '/metadata/dc.title/1' },
          { op: 'remove', path: '/metadata/dc.title/0' }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain all the remove operations with their corresponding indexes', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains multiple removes with random indexes', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Second deleted title',
              place: 1
            }),
            changeType: FieldChangeType.REMOVE
          },
          update2: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Third deleted title',
              place: 2
            }),
            changeType: FieldChangeType.REMOVE
          },
          update3: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'First deleted title',
              place: 0
            }),
            changeType: FieldChangeType.REMOVE
          }
        });
        expected = [
          { op: 'remove', path: '/metadata/dc.title/1' },
          { op: 'remove', path: '/metadata/dc.title/1' },
          { op: 'remove', path: '/metadata/dc.title/0' }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain all the remove operations with the correct indexes taking previous operations into account', () => {
        expect(result).toEqual(expected);
      });
    });

    describe('when fieldUpdates contains multiple removes and updates with random indexes', () => {
      beforeEach(() => {
        fieldUpdates = Object.assign({
          update1: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Second deleted title',
              place: 1
            }),
            changeType: FieldChangeType.REMOVE
          },
          update2: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'Third changed title',
              place: 2
            }),
            changeType: FieldChangeType.UPDATE
          },
          update3: {
            field: Object.assign(new MetadatumViewModel(), {
              key: 'dc.title',
              value: 'First deleted title',
              place: 0
            }),
            changeType: FieldChangeType.REMOVE
          }
        });
        expected = [
          { op: 'remove', path: '/metadata/dc.title/1' },
          { op: 'replace', path: '/metadata/dc.title/1', value: { value: 'Third changed title', language: undefined } },
          { op: 'remove', path: '/metadata/dc.title/0' }
        ] as any[];
        result = service.fieldUpdatesToPatchOperations(fieldUpdates);
      });

      it('should contain all the remove and replace operations with the correct indexes taking previous remove operations into account', () => {
        expect(result).toEqual(expected);
      });
    });
  });
});
