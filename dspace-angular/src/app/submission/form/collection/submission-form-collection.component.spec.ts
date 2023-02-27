import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, inject, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngrx/store';
import { cold } from 'jasmine-marbles';
import { of } from 'rxjs';

import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import { mockSubmissionId, mockSubmissionRestResponse } from '../../../shared/mocks/submission.mock';
import { SubmissionService } from '../../submission.service';
import { SubmissionFormCollectionComponent } from './submission-form-collection.component';
import { CommunityDataService } from '../../../core/data/community-data.service';
import { SubmissionJsonPatchOperationsService } from '../../../core/submission/submission-json-patch-operations.service';
import { SubmissionJsonPatchOperationsServiceStub } from '../../../shared/testing/submission-json-patch-operations-service.stub';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { createTestComponent } from '../../../shared/testing/utils.test';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { SectionsService } from '../../sections/sections.service';
import { Collection } from '../../../core/shared/collection.model';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';

describe('SubmissionFormCollectionComponent Component', () => {

  let comp: SubmissionFormCollectionComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionFormCollectionComponent>;
  let submissionServiceStub: SubmissionServiceStub;
  let jsonPatchOpServiceStub: SubmissionJsonPatchOperationsServiceStub;

  const submissionId = mockSubmissionId;
  const collectionId = '1234567890-1';
  const definition = 'traditional';
  const submissionRestResponse = mockSubmissionRestResponse;

  const mockCollection = Object.assign(new Collection(), {
    name: 'Community 1-Collection 1',
    id: collectionId,
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 1'
      }],
    _links: {
      defaultAccessConditions: collectionId + '/defaultAccessConditions'
    }
  });

  const mockCollectionList = [
    {
      communities: [
        {
          id: '123456789-1',
          name: 'Community 1'
        }
      ],
      collection: {
        id: '1234567890-1',
        name: 'Community 1-Collection 1'
      }
    },
    {
      communities: [
        {
          id: '123456789-1',
          name: 'Community 1'
        }
      ],
      collection: {
        id: '1234567890-2',
        name: 'Community 1-Collection 2'
      }
    },
    {
      communities: [
        {
          id: '123456789-2',
          name: 'Community 2'
        }
      ],
      collection: {
        id: '1234567890-3',
        name: 'Community 2-Collection 1'
      }
    },
    {
      communities: [
        {
          id: '123456789-2',
          name: 'Community 2'
        }
      ],
      collection: {
        id: '1234567890-4',
        name: 'Community 2-Collection 2'
      }
    }
  ];

  const communityDataService: any = jasmine.createSpyObj('communityDataService', {
    findAll: jasmine.createSpy('findAll')
  });

  const collectionDataService: any = jasmine.createSpyObj('collectionDataService', {
    findById: jasmine.createSpy('findById'),
    getAuthorizedCollectionByCommunity: jasmine.createSpy('getAuthorizedCollectionByCommunity')
  });

  const store: any = jasmine.createSpyObj('store', {
    dispatch: jasmine.createSpy('dispatch'),
    select: jasmine.createSpy('select')
  });
  const jsonPatchOpBuilder: any = jasmine.createSpyObj('jsonPatchOpBuilder', {
    replace: jasmine.createSpy('replace')
  });

  const sectionsService: any = jasmine.createSpyObj('sectionsService', {
    isSectionTypeAvailable: of(true)
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        NgbModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        SubmissionFormCollectionComponent,
        TestComponent
      ],
      providers: [
        { provide: CollectionDataService, useValue: collectionDataService },
        { provide: SubmissionJsonPatchOperationsService, useClass: SubmissionJsonPatchOperationsServiceStub },
        { provide: SubmissionService, useClass: SubmissionServiceStub },
        { provide: CommunityDataService, useValue: communityDataService },
        { provide: JsonPatchOperationsBuilder, useValue: jsonPatchOpBuilder },
        { provide: Store, useValue: store },
        { provide: SectionsService, useValue: sectionsService },
        ChangeDetectorRef,
        SubmissionFormCollectionComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));
      const html = `
        <ds-submission-form-collection [currentCollectionId]="collectionId"
                                       [currentDefinition]="definitionId"
                                       [submissionId]="submissionId"
                                       (collectionChange)="onCollectionChange($event)">
        </ds-submission-form-collection>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionFormCollectionComponent', inject([SubmissionFormCollectionComponent], (app: SubmissionFormCollectionComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionFormCollectionComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      submissionServiceStub = TestBed.inject(SubmissionService as any);
      jsonPatchOpServiceStub = TestBed.inject(SubmissionJsonPatchOperationsService as any);
      comp.currentCollectionId = collectionId;
      comp.currentDefinition = definition;
      comp.submissionId = submissionId;
    });

    afterEach(() => {
      comp = null;
      compAsAny = null;
      fixture = null;
      submissionServiceStub = null;
      jsonPatchOpServiceStub = null;
    });

    it('should init JsonPatchOperationPathCombiner', () => {
      const expected = new JsonPatchOperationPathCombiner('sections', 'collection');

      fixture.detectChanges();

      expect(compAsAny.pathCombiner).toEqual(expected);
    });

    describe('', () => {
      let dropdowBtn: DebugElement;
      let dropdownMenu: DebugElement;

      beforeEach(() => {
        fixture.detectChanges();
        dropdowBtn = fixture.debugElement.query(By.css('#collectionControlsMenuButton'));
        dropdownMenu = fixture.debugElement.query(By.css('#collectionControlsDropdownMenu'));
      });

      it('should have dropdown menu closed', () => {

        expect(dropdowBtn).not.toBeUndefined();
        expect(dropdownMenu.nativeElement.classList).not.toContain('show');

      });

      it('should display dropdown menu when click on dropdown button', fakeAsync(() => {

        spyOn(comp, 'onClose');
        dropdowBtn.triggerEventHandler('click', null);
        tick();
        fixture.detectChanges();

        fixture.whenStable().then(() => {
          expect(comp.onClose).toHaveBeenCalled();
          expect(dropdownMenu.nativeElement.classList).toContain('show');
        });
      }));

      it('the dropdown menu should be enable', () => {
        const dropDown = fixture.debugElement.query(By.css('#collectionControlsDropdownMenu'));
        expect(dropDown).toBeTruthy();
      });

      it('the dropdown menu should be disabled', () => {
        comp.available$ = of(false);
        fixture.detectChanges();
        const dropDown = fixture.debugElement.query(By.css('#collectionControlsDropdownMenu'));
        expect(dropDown).toBeFalsy();
      });

      it('should be simulated when the drop-down menu is closed', () => {
        spyOn(comp, 'onClose');
        comp.onClose();
        expect(comp.onClose).toHaveBeenCalled();
      });

      it('should be simulated when the drop-down menu is toggled', () => {
        spyOn(comp, 'toggled');
        comp.toggled(false);
        expect(comp.toggled).toHaveBeenCalled();
      });

      it('should change collection properly', () => {
        spyOn(comp.collectionChange, 'emit').and.callThrough();
        jsonPatchOpServiceStub.jsonPatchByResourceID.and.returnValue(of(submissionRestResponse));
        submissionServiceStub.retrieveSubmission.and.returnValue(createSuccessfulRemoteDataObject$(submissionRestResponse[0]));
        comp.ngOnInit();
        comp.onSelect(mockCollectionList[1]);
        fixture.detectChanges();

        expect(submissionServiceStub.changeSubmissionCollection).toHaveBeenCalled();
        expect(comp.selectedCollectionId).toBe(mockCollectionList[1].collection.id);
        expect(comp.selectedCollectionName$).toBeObservable(cold('(a|)', {
          a: mockCollectionList[1].collection.name
        }));
      });
    });

  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  collectionId = '1234567890-1';
  definitionId = 'traditional';
  submissionId = mockSubmissionId;

  onCollectionChange = () => { return; };

}
