import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { Router } from '@angular/router';

import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TestScheduler } from 'rxjs/testing';
import { of as observableOf } from 'rxjs';
import { getTestScheduler } from 'jasmine-marbles';

import { SubmissionImportExternalPreviewComponent } from './submission-import-external-preview.component';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { RouterStub } from '../../../shared/testing/router.stub';
import { SubmissionService } from '../../submission.service';
import { createTestComponent } from '../../../shared/testing/utils.test';
import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { ExternalSourceEntry } from '../../../core/shared/external-source-entry.model';
import { Metadata } from '../../../core/shared/metadata.utils';
import { SubmissionImportExternalCollectionComponent } from '../import-external-collection/submission-import-external-collection.component';
import { CollectionListEntry } from '../../../shared/collection-dropdown/collection-dropdown.component';

const externalEntry = Object.assign(new ExternalSourceEntry(), {
  id: '0001-0001-0001-0001',
  display: 'John Doe',
  value: 'John, Doe',
  metadata: {
    'dc.identifier.uri': [
      {
        value: 'https://orcid.org/0001-0001-0001-0001'
      }
    ]
  },
  _links: { self: { href: 'http://test-rest.com/server/api/integration/externalSources/orcidV2/entryValues/0000-0003-4851-8004' } }
});

describe('SubmissionImportExternalPreviewComponent test suite', () => {
  let comp: SubmissionImportExternalPreviewComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionImportExternalPreviewComponent>;
  let submissionServiceStub: SubmissionServiceStub;
  let scheduler: TestScheduler;
  const ngbActiveModal = jasmine.createSpyObj('modal', ['close', 'dismiss']);
  const ngbModal = jasmine.createSpyObj('modal', ['open']);


  beforeEach(waitForAsync(() => {
    scheduler = getTestScheduler();
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot()
      ],
      declarations: [
        SubmissionImportExternalPreviewComponent,
        TestComponent
      ],
      providers: [
        { provide: Router, useValue: new RouterStub() },
        { provide: SubmissionService, useValue: new SubmissionServiceStub() },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: NgbModal, useValue: ngbModal },
        { provide: NgbActiveModal, useValue: ngbActiveModal },
        SubmissionImportExternalPreviewComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then();
  }));

  // First test to check the correct component creation
  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      const html = `
        <ds-submission-import-external-preview [externalSourceEntry]="externalSourceEntry"></ds-submission-import-external-preview>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionImportExternalPreviewComponent', inject([SubmissionImportExternalPreviewComponent], (app: SubmissionImportExternalPreviewComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionImportExternalPreviewComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      submissionServiceStub = TestBed.inject(SubmissionService as any);
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('Should init component properly', () => {
      comp.externalSourceEntry = externalEntry;
      const expected = [
        { key: 'dc.identifier.uri', value: Metadata.first(comp.externalSourceEntry.metadata, 'dc.identifier.uri') }
      ];
      fixture.detectChanges();

      expect(comp.metadataList).toEqual(expected);
    });

    it('Should close the modal calling \'activeModal.dismiss\'', () => {
      comp.modalRef = jasmine.createSpyObj('modal', ['close', 'dismiss']);
      comp.closeMetadataModal();

      expect(compAsAny.activeModal.dismiss).toHaveBeenCalled();
    });

    it('Should start the import process opening a modal', (done) => {
      const emittedEvent: CollectionListEntry = {
        communities: [
          {
            id: 'dummy',
            uuid: 'dummy',
            name: 'dummy',
          }
        ],
        collection: {
          id: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
          uuid: 'ce64f48e-2c9b-411a-ac36-ee429c0e6a88',
          name: 'Collection 1',
        }
      };
      const submissionObjects = [
        { id: 'jk11k13o-9v4z-632i-sr88-wq071n0h1d47' }
      ];
      comp.externalSourceEntry = externalEntry;
      ngbModal.open.and.returnValue({
        componentInstance: { selectedEvent: observableOf(emittedEvent) },
        close: () => {
          return;
        }
      });
      spyOn(comp, 'closeMetadataModal');
      submissionServiceStub.createSubmissionFromExternalSource.and.returnValue(observableOf(submissionObjects));
      spyOn(compAsAny.router, 'navigateByUrl');
      scheduler.schedule(() => comp.import());
      scheduler.flush();

      expect(compAsAny.modalService.open).toHaveBeenCalledWith(SubmissionImportExternalCollectionComponent, { size: 'lg' });
      expect(comp.closeMetadataModal).toHaveBeenCalled();
      expect(compAsAny.submissionService.createSubmissionFromExternalSource).toHaveBeenCalledWith(externalEntry._links.self.href, emittedEvent.collection.id);
      expect(compAsAny.router.navigateByUrl).toHaveBeenCalledWith('/workspaceitems/' + submissionObjects[0].id + '/edit');
      done();
    });
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  externalSourceEntry = externalEntry;
}
