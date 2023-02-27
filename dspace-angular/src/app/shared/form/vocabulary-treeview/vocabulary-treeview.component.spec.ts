import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { CdkTreeModule } from '@angular/cdk/tree';

import { of as observableOf } from 'rxjs';
import { StoreModule } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { provideMockStore } from '@ngrx/store/testing';

import { createTestComponent } from '../../testing/utils.test';
import { VocabularyTreeviewComponent } from './vocabulary-treeview.component';
import { VocabularyTreeviewService } from './vocabulary-treeview.service';
import { VocabularyEntryDetail } from '../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';
import { TreeviewFlatNode } from './vocabulary-treeview-node.model';
import { FormFieldMetadataValueObject } from '../builder/models/form-field-metadata-value.model';
import { VocabularyOptions } from '../../../core/submission/vocabularies/models/vocabulary-options.model';
import { PageInfo } from '../../../core/shared/page-info.model';
import { VocabularyEntry } from '../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { AuthTokenInfo } from '../../../core/auth/models/auth-token-info.model';
import { authReducer } from '../../../core/auth/auth.reducer';
import { storeModuleConfig } from '../../../app.reducer';

describe('VocabularyTreeviewComponent test suite', () => {

  let comp: VocabularyTreeviewComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<VocabularyTreeviewComponent>;
  let initialState;

  const item = new VocabularyEntryDetail();
  item.id = 'node1';
  const item2 = new VocabularyEntryDetail();
  item2.id = 'node2';
  const emptyNodeMap = new Map<string, TreeviewFlatNode>();
  const storedNodeMap = new Map<string, TreeviewFlatNode>().set('test', new TreeviewFlatNode(item2));
  const nodeMap = new Map<string, TreeviewFlatNode>().set('test', new TreeviewFlatNode(item));
  const vocabularyOptions = new VocabularyOptions('vocabularyTest', false);
  const modalStub = jasmine.createSpyObj('modalStub', ['close']);
  const vocabularyTreeviewServiceStub = jasmine.createSpyObj('VocabularyTreeviewService', {
    initialize: jasmine.createSpy('initialize'),
    getData: jasmine.createSpy('getData'),
    loadMore: jasmine.createSpy('loadMore'),
    loadMoreRoot: jasmine.createSpy('loadMoreRoot'),
    isLoading: jasmine.createSpy('isLoading'),
    searchByQuery: jasmine.createSpy('searchByQuery'),
    restoreNodes: jasmine.createSpy('restoreNodes'),
    cleanTree: jasmine.createSpy('cleanTree'),
  });

  initialState = {
    core: {
      auth: {
        authenticated: true,
        loaded: true,
        blocking: false,
        loading: false,
        authToken: new AuthTokenInfo('test_token'),
        userId: 'testid',
        authMethods: []
      }
    }
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        CdkTreeModule,
        StoreModule.forRoot({ auth: authReducer }, storeModuleConfig),
        TranslateModule.forRoot()
      ],
      declarations: [
        VocabularyTreeviewComponent,
        TestComponent
      ],
      providers: [
        { provide: VocabularyTreeviewService, useValue: vocabularyTreeviewServiceStub },
        { provide: NgbActiveModal, useValue: modalStub },
        provideMockStore({ initialState }),
        ChangeDetectorRef,
        VocabularyTreeviewComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then(() => {
      vocabularyTreeviewServiceStub.getData.and.returnValue(observableOf([]));
      vocabularyTreeviewServiceStub.isLoading.and.returnValue(observableOf(false));
    });
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {

      const html = `
        <ds-vocabulary-treeview [vocabularyOptions]="vocabularyOptions" [preloadLevel]="preloadLevel"></ds-vocabulary-treeview>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
      vocabularyTreeviewServiceStub.getData.and.returnValue(observableOf([]));
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create VocabularyTreeviewComponent', inject([VocabularyTreeviewComponent], (app: VocabularyTreeviewComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(VocabularyTreeviewComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      comp.vocabularyOptions = vocabularyOptions;
      comp.selectedItem = null;
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('should should init component properly', () => {
      fixture.detectChanges();
      expect(comp.dataSource.data).toEqual([]);
      expect(vocabularyTreeviewServiceStub.initialize).toHaveBeenCalled();
    });

    it('should should init component properly with init value as FormFieldMetadataValueObject', () => {
      const currentValue = new FormFieldMetadataValueObject();
      currentValue.value = 'testValue';
      currentValue.otherInformation = {
        id: 'entryID'
      };
      comp.selectedItem = currentValue;
      fixture.detectChanges();
      expect(comp.dataSource.data).toEqual([]);
      expect(vocabularyTreeviewServiceStub.initialize).toHaveBeenCalledWith(comp.vocabularyOptions, new PageInfo(), null);
    });

    it('should should init component properly with init value as VocabularyEntry', () => {
      const currentValue = new VocabularyEntry();
      currentValue.value = 'testValue';
      currentValue.otherInformation = {
        id: 'entryID'
      };
      comp.selectedItem = currentValue;
      fixture.detectChanges();
      expect(comp.dataSource.data).toEqual([]);
      expect(vocabularyTreeviewServiceStub.initialize).toHaveBeenCalledWith(comp.vocabularyOptions, new PageInfo(), null);
    });

    it('should call loadMore function', () => {
      comp.loadMore(item);
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.loadMore).toHaveBeenCalledWith(item);
    });

    it('should call loadMoreRoot function', () => {
      const node = new TreeviewFlatNode(item);
      comp.loadMoreRoot(node);
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.loadMoreRoot).toHaveBeenCalledWith(node);
    });

    it('should call loadChildren function', () => {
      const node = new TreeviewFlatNode(item);
      comp.loadChildren(node);
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.loadMore).toHaveBeenCalledWith(node.item, true);
    });

    it('should emit select event', () => {
      spyOn(comp, 'onSelect');
      comp.onSelect(item);

      expect(comp.onSelect).toHaveBeenCalledWith(item);
    });

    it('should call searchByQuery function and set storedNodeMap properly', () => {
      comp.searchText = 'test search';
      comp.nodeMap.set('test', new TreeviewFlatNode(item));
      comp.search();
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.searchByQuery).toHaveBeenCalledWith('test search');
      expect(comp.storedNodeMap).toEqual(nodeMap);
      expect(comp.nodeMap).toEqual(emptyNodeMap);
    });

    it('should call searchByQuery function and not set storedNodeMap', () => {
      comp.searchText = 'test search';
      comp.nodeMap.set('test', new TreeviewFlatNode(item));
      comp.storedNodeMap.set('test', new TreeviewFlatNode(item2));
      comp.search();
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.searchByQuery).toHaveBeenCalledWith('test search');
      expect(comp.storedNodeMap).toEqual(storedNodeMap);
      expect(comp.nodeMap).toEqual(emptyNodeMap);
    });

    it('should call restoreNodes function and restore nodeMap properly', () => {
      comp.nodeMap.set('test', new TreeviewFlatNode(item));
      comp.storedNodeMap.set('test', new TreeviewFlatNode(item2));
      comp.reset();
      fixture.detectChanges();
      expect(vocabularyTreeviewServiceStub.restoreNodes).toHaveBeenCalled();
      expect(comp.storedNodeMap).toEqual(emptyNodeMap);
      expect(comp.nodeMap).toEqual(storedNodeMap);
      expect(comp.searchText).toEqual('');
    });

    it('should clear search string', () => {
      comp.nodeMap.set('test', new TreeviewFlatNode(item));
      comp.reset();
      fixture.detectChanges();
      expect(comp.storedNodeMap).toEqual(emptyNodeMap);
      expect(comp.nodeMap).toEqual(nodeMap);
      expect(comp.searchText).toEqual('');
    });

    it('should call cleanTree method on destroy', () => {
      compAsAny.ngOnDestroy();
      expect(vocabularyTreeviewServiceStub.cleanTree).toHaveBeenCalled();
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  vocabularyOptions: VocabularyOptions = new VocabularyOptions('vocabularyTest', false);
  preloadLevel = 2;

}
