import { Store } from '@ngrx/store';
import { ObjectUpdatesService } from './object-updates.service';
import {
  DiscardObjectUpdatesAction,
  InitializeFieldsAction,
  ReinstateObjectUpdatesAction,
  RemoveFieldUpdateAction,
  SelectVirtualMetadataAction,
  SetEditableFieldUpdateAction
} from './object-updates.actions';
import { of as observableOf } from 'rxjs';
import { Notification } from '../../../shared/notifications/models/notification.model';
import { NotificationType } from '../../../shared/notifications/models/notification-type';
import { OBJECT_UPDATES_TRASH_PATH } from './object-updates.reducer';
import { Relationship } from '../../shared/item-relationships/relationship.model';
import { Injector } from '@angular/core';
import { FieldChangeType } from './field-change-type.model';
import { CoreState } from '../../core-state.model';

describe('ObjectUpdatesService', () => {
  let service: ObjectUpdatesService;
  let store: Store<CoreState>;
  const value = 'test value';
  const url = 'test-url.com/dspace';
  const identifiable1 = { uuid: '8222b07e-330d-417b-8d7f-3b82aeaf2320' };
  const identifiable1Updated = { uuid: '8222b07e-330d-417b-8d7f-3b82aeaf2320', value: value };
  const identifiable2 = { uuid: '26cbb5ce-5786-4e57-a394-b9fcf8eaf241' };
  const identifiable3 = { uuid: 'c5d2c2f7-d757-48bf-84cc-8c9229c8407e' };
  const identifiables = [identifiable1, identifiable2];
  const relationship: Relationship = Object.assign(new Relationship(), { uuid: 'test relationship uuid' });

  const fieldUpdates = {
    [identifiable1.uuid]: { field: identifiable1Updated, changeType: FieldChangeType.UPDATE },
    [identifiable3.uuid]: { field: identifiable3, changeType: FieldChangeType.ADD }
  };

  const modDate = new Date(2010, 2, 11);
  let patchOperationService;
  let injector: Injector;

  beforeEach(() => {
    const fieldStates = {
      [identifiable1.uuid]: { editable: false, isNew: false, isValid: true },
      [identifiable2.uuid]: { editable: true, isNew: false, isValid: false },
      [identifiable3.uuid]: { editable: true, isNew: true, isValid: true },
    };

    patchOperationService = jasmine.createSpyObj('patchOperationService', {
      fieldUpdatesToPatchOperations: []
    });
    const objectEntry = {
      fieldStates, fieldUpdates, lastModified: modDate, virtualMetadataSources: {}, patchOperationService
    };
    store = new Store<CoreState>(undefined, undefined, undefined);
    spyOn(store, 'dispatch');
    injector = jasmine.createSpyObj('injector', {
      get: patchOperationService
    });
    service = new ObjectUpdatesService(store, injector);

    spyOn(service as any, 'getObjectEntry').and.returnValue(observableOf(objectEntry));
    spyOn(service as any, 'getFieldState').and.callFake((uuid) => {
      return observableOf(fieldStates[uuid]);
    });
    spyOn(service as any, 'saveFieldUpdate');
  });

  describe('initialize', () => {
    it('should dispatch an INITIALIZE action with the correct URL, initial identifiables and the last modified date', () => {
      service.initialize(url, identifiables, modDate);
      expect(store.dispatch).toHaveBeenCalledWith(new InitializeFieldsAction(url, identifiables, modDate));
    });
  });

  describe('getFieldUpdates', () => {
    it('should return the list of all fields, including their update if there is one', () => {
      const result$ = service.getFieldUpdates(url, identifiables);
      expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

      const expectedResult = {
        [identifiable1.uuid]: { field: identifiable1Updated, changeType: FieldChangeType.UPDATE },
        [identifiable2.uuid]: { field: identifiable2, changeType: undefined },
        [identifiable3.uuid]: { field: identifiable3, changeType: FieldChangeType.ADD }
      };

      result$.subscribe((result) => {
        expect(result).toEqual(expectedResult);
      });
    });
  });

  describe('getFieldUpdatesExclusive', () => {
    it('should return the list of all fields, including their update if there is one, excluding updates that aren\'t part of the initial values provided', (done) => {
      const result$ = service.getFieldUpdatesExclusive(url, identifiables);
      expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

      const expectedResult = {
        [identifiable1.uuid]: { field: identifiable1Updated, changeType: FieldChangeType.UPDATE },
        [identifiable2.uuid]: { field: identifiable2, changeType: undefined }
      };

      result$.subscribe((result) => {
        expect(result).toEqual(expectedResult);
        done();
      });
    });
  });

  describe('isEditable', () => {
    it('should return false if this identifiable is currently not editable in the store', () => {
      const result$ = service.isEditable(url, identifiable1.uuid);
      expect((service as any).getFieldState).toHaveBeenCalledWith(url, identifiable1.uuid);
      result$.subscribe((result) => {
        expect(result).toEqual(false);
      });
    });

    it('should return true if this identifiable is currently editable in the store', () => {
      const result$ = service.isEditable(url, identifiable2.uuid);
      expect((service as any).getFieldState).toHaveBeenCalledWith(url, identifiable2.uuid);
      result$.subscribe((result) => {
        expect(result).toEqual(true);
      });
    });
  });

  describe('isValid', () => {
    it('should return false if this identifiable is currently not valid in the store', () => {
      const result$ = service.isValid(url, identifiable2.uuid);
      expect((service as any).getFieldState).toHaveBeenCalledWith(url, identifiable2.uuid);
      result$.subscribe((result) => {
        expect(result).toEqual(false);
      });
    });

    it('should return true if this identifiable is currently valid in the store', () => {
      const result$ = service.isValid(url, identifiable1.uuid);
      expect((service as any).getFieldState).toHaveBeenCalledWith(url, identifiable1.uuid);
      result$.subscribe((result) => {
        expect(result).toEqual(true);
      });
    });
  });

  describe('saveAddFieldUpdate', () => {
    it('should call saveFieldUpdate on the service with FieldChangeType.ADD', () => {
      service.saveAddFieldUpdate(url, identifiable1);
      expect((service as any).saveFieldUpdate).toHaveBeenCalledWith(url, identifiable1, FieldChangeType.ADD);
    });
  });

  describe('saveRemoveFieldUpdate', () => {
    it('should call saveFieldUpdate on the service with FieldChangeType.REMOVE', () => {
      service.saveRemoveFieldUpdate(url, identifiable1);
      expect((service as any).saveFieldUpdate).toHaveBeenCalledWith(url, identifiable1, FieldChangeType.REMOVE);
    });
  });

  describe('saveChangeFieldUpdate', () => {
    it('should call saveFieldUpdate on the service with FieldChangeType.UPDATE', () => {
      service.saveChangeFieldUpdate(url, identifiable1);
      expect((service as any).saveFieldUpdate).toHaveBeenCalledWith(url, identifiable1, FieldChangeType.UPDATE);
    });
  });

  describe('setEditableFieldUpdate', () => {
    it('should dispatch a SetEditableFieldUpdateAction action with the correct URL, uuid and true when true was set', () => {
      service.setEditableFieldUpdate(url, identifiable1.uuid, true);
      expect(store.dispatch).toHaveBeenCalledWith(new SetEditableFieldUpdateAction(url, identifiable1.uuid, true));
    });

    it('should dispatch an SetEditableFieldUpdateAction action with the correct URL, uuid and false when false was set', () => {
      service.setEditableFieldUpdate(url, identifiable1.uuid, false);
      expect(store.dispatch).toHaveBeenCalledWith(new SetEditableFieldUpdateAction(url, identifiable1.uuid, false));
    });
  });

  describe('discardFieldUpdates', () => {
    it('should dispatch a DiscardObjectUpdatesAction action with the correct URL and passed notification ', () => {
      const undoNotification = new Notification('id', NotificationType.Info, 'undo');
      service.discardFieldUpdates(url, undoNotification);
      expect(store.dispatch).toHaveBeenCalledWith(new DiscardObjectUpdatesAction(url, undoNotification));
    });
  });

  describe('reinstateFieldUpdates', () => {
    it('should dispatch a ReinstateObjectUpdatesAction action with the correct URL ', () => {
      service.reinstateFieldUpdates(url);
      expect(store.dispatch).toHaveBeenCalledWith(new ReinstateObjectUpdatesAction(url));
    });
  });

  describe('removeSingleFieldUpdate', () => {
    it('should dispatch a RemoveFieldUpdateAction action with the correct URL and uuid', () => {
      service.removeSingleFieldUpdate(url, identifiable1.uuid);
      expect(store.dispatch).toHaveBeenCalledWith(new RemoveFieldUpdateAction(url, identifiable1.uuid));
    });
  });

  describe('getUpdatedFields', () => {
    it('should return the list of all metadata fields with their new values', () => {
      const result$ = service.getUpdatedFields(url, identifiables);
      expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

      const expectedResult = [identifiable1Updated, identifiable2, identifiable3];
      result$.subscribe((result) => {
        expect(result).toEqual(expectedResult);
      });
    });
  });

  describe('hasUpdates', () => {
    it('should return true when there are updates', () => {
      const result$ = service.hasUpdates(url);
      expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

      const expectedResult = true;
      result$.subscribe((result) => {
        expect(result).toEqual(expectedResult);
      });
    });
    describe('when updates are emtpy', () => {
      beforeEach(() => {
        (service as any).getObjectEntry.and.returnValue(observableOf({}));
      });

      it('should return false when there are no updates', () => {
        const result$ = service.hasUpdates(url);
        expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

        const expectedResult = false;
        result$.subscribe((result) => {
          expect(result).toEqual(expectedResult);
        });
      });
    });
  });

  describe('isReinstatable', () => {

    describe('when updates are not emtpy', () => {
      beforeEach(() => {
        spyOn(service, 'hasUpdates').and.returnValue(observableOf(true));
      });

      it('should return true', () => {
        const result$ = service.isReinstatable(url);
        expect(service.hasUpdates).toHaveBeenCalledWith(url + OBJECT_UPDATES_TRASH_PATH);

        const expectedResult = true;
        result$.subscribe((result) => {
          expect(result).toEqual(expectedResult);
        });
      });
    });

    describe('when updates are emtpy', () => {
      beforeEach(() => {
        spyOn(service, 'hasUpdates').and.returnValue(observableOf(false));
      });

      it('should return false', () => {
        const result$ = service.isReinstatable(url);
        expect(service.hasUpdates).toHaveBeenCalledWith(url + OBJECT_UPDATES_TRASH_PATH);
        const expectedResult = false;
        result$.subscribe((result) => {
          expect(result).toEqual(expectedResult);
        });
      });
    });
  });

  describe('getLastModified', () => {
    it('should return true when hasUpdates returns true', () => {
      const result$ = service.getLastModified(url);
      expect((service as any).getObjectEntry).toHaveBeenCalledWith(url);

      const expectedResult = modDate;
      result$.subscribe((result) => {
        expect(result).toEqual(expectedResult);
      });
    });
  });

  describe('setSelectedVirtualMetadata', () => {
    it('should dispatch a SELECT_VIRTUAL_METADATA action with the correct URL, relationship, identifiable and boolean', () => {
      service.setSelectedVirtualMetadata(url, relationship.uuid, identifiable1.uuid, true);
      expect(store.dispatch).toHaveBeenCalledWith(new SelectVirtualMetadataAction(url, relationship.uuid, identifiable1.uuid, true));
    });
  });

  describe('createPatch', () => {
    let result$;

    beforeEach(() => {
      result$ = service.createPatch(url);
    });

    it('should inject the service stored in the entry', (done) => {
      result$.subscribe(() => {
        expect(injector.get).toHaveBeenCalledWith(patchOperationService);
        done();
      });
    });

    it('should create a patch from the fieldUpdates using the injected service', (done) => {
      result$.subscribe(() => {
        expect(patchOperationService.fieldUpdatesToPatchOperations).toHaveBeenCalledWith(fieldUpdates);
        done();
      });
    });
  });

});
