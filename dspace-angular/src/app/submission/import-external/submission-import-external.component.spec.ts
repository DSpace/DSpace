import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { getTestScheduler } from 'jasmine-marbles';
import { TranslateModule } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { SubmissionImportExternalComponent } from './submission-import-external.component';
import { ExternalSourceDataService } from '../../core/data/external-source-data.service';
import { getMockExternalSourceService } from '../../shared/mocks/external-source.service.mock';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { RouteService } from '../../core/services/route.service';
import { createPaginatedList, createTestComponent } from '../../shared/testing/utils.test';
import { RouterStub } from '../../shared/testing/router.stub';
import { VarDirective } from '../../shared/utils/var.directive';
import { routeServiceStub } from '../../shared/testing/route-service.stub';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import {
  createFailedRemoteDataObject$,
  createSuccessfulRemoteDataObject,
  createSuccessfulRemoteDataObject$
} from '../../shared/remote-data.utils';
import { ExternalSourceEntry } from '../../core/shared/external-source-entry.model';
import { SubmissionImportExternalPreviewComponent } from './import-external-preview/submission-import-external-preview.component';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

describe('SubmissionImportExternalComponent test suite', () => {
  let comp: SubmissionImportExternalComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionImportExternalComponent>;
  let scheduler: TestScheduler;
  const ngbModal = jasmine.createSpyObj('modal', ['open']);
  const mockSearchOptions = observableOf(new PaginatedSearchOptions({
    pagination: Object.assign(new PaginationComponentOptions(), {
      pageSize: 10,
      currentPage: 0
    }),
    query: 'test'
  }));
  const searchConfigServiceStub = {
    paginatedSearchOptions: mockSearchOptions
  };
  const mockExternalSourceService: any = getMockExternalSourceService();

  beforeEach(waitForAsync (() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        BrowserAnimationsModule
      ],
      declarations: [
        SubmissionImportExternalComponent,
        TestComponent,
        VarDirective
      ],
      providers: [
        { provide: ExternalSourceDataService, useValue: mockExternalSourceService },
        { provide: SearchConfigurationService, useValue: searchConfigServiceStub },
        { provide: RouteService, useValue: routeServiceStub },
        { provide: Router, useValue: new RouterStub() },
        { provide: NgbModal, useValue: ngbModal },
        SubmissionImportExternalComponent
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
        <ds-submission-import-external></ds-submission-import-external>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionImportExternalComponent', inject([SubmissionImportExternalComponent], (app: SubmissionImportExternalComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionImportExternalComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      scheduler = getTestScheduler();
      mockExternalSourceService.getExternalSourceEntries.and.returnValue(createSuccessfulRemoteDataObject$(createPaginatedList([])));
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('Should init component properly (without route data)', () => {
      const expectedEntries = createSuccessfulRemoteDataObject(createPaginatedList([]));
      comp.routeData = {entity: '', sourceId: '', query: '' };
      spyOn(compAsAny.routeService, 'getQueryParameterValue').and.returnValue(observableOf(''));
      fixture.detectChanges();

      expect(comp.routeData).toEqual({entity: '', sourceId: '', query: '' });
      expect(comp.isLoading$.value).toBe(false);
      expect(comp.entriesRD$.value).toEqual(expectedEntries);
    });

    it('Should init component properly (with route data)', () => {
      comp.routeData = {entity: '', sourceId: '', query: '' };
      spyOn(compAsAny, 'retrieveExternalSources');
      spyOn(compAsAny.routeService, 'getQueryParameterValue').and.returnValues(observableOf('entity'), observableOf('source'), observableOf('dummy'));
      fixture.detectChanges();

      expect(compAsAny.retrieveExternalSources).toHaveBeenCalled();
    });

    it('Should call \'getExternalSourceEntries\' properly', () => {
      spyOn(routeServiceStub, 'getQueryParameterValue').and.callFake((param) => {
        if (param === 'sourceId') {
          return observableOf('orcidV2');
        } else if (param === 'query') {
          return observableOf('test');
        }
        return observableOf({});
      });

      fixture.detectChanges();


      expect(comp.isLoading$.value).toBe(false);
      expect(compAsAny.externalService.getExternalSourceEntries).toHaveBeenCalled();
    });

    it('Should call \'router.navigate\'', () => {
      comp.routeData = {entity: 'Person', sourceId: '', query: '' };
      spyOn(compAsAny, 'retrieveExternalSources').and.callFake(() => null);
      compAsAny.router.navigate.and.returnValue( new Promise(() => {return;}));
      const event = {entity: 'Person', sourceId: 'orcidV2', query: 'dummy' };

      scheduler.schedule(() => comp.getExternalSourceData(event));
      scheduler.flush();

      expect(compAsAny.router.navigate).toHaveBeenCalledWith([], { queryParams: { entity: event.entity, sourceId: event.sourceId, query: event.query }, replaceUrl: true });
    });

    it('Entry should be passed to the component loaded inside the modal', () => {
      const entry = Object.assign(new ExternalSourceEntry(), {
        id: '0001-0001-0001-0001',
        display: 'John Doe',
        value: 'John, Doe',
        metadata: {
          'dc.identifier.uri': [
            {
              value: 'https://orcid.org/0001-0001-0001-0001'
            }
          ]
        }
      });
      ngbModal.open.and.returnValue({componentInstance: { externalSourceEntry: null}});
      comp.import(entry);

      expect(compAsAny.modalService.open).toHaveBeenCalledWith(SubmissionImportExternalPreviewComponent, { size: 'lg' });
      expect(comp.modalRef.componentInstance.externalSourceEntry).toEqual(entry);
    });

    it('Should set the correct label', () => {
      const label = 'Person';
      compAsAny.selectLabel(label);

      expect(comp.label).toEqual(label);
    });
  });

  describe('handle backend response for search query', () => {
    const paginatedData: any = {
      'timeCompleted': 1657009282990,
      'msToLive': 900000,
      'lastUpdated': 1657009282990,
      'state': 'Success',
      'errorMessage': null,
      'payload': {
        'type': {
          'value': 'paginated-list'
        },
        'pageInfo': {
          'elementsPerPage': 10,
          'totalElements': 11971608,
          'totalPages': 1197161,
          'currentPage': 1
        },
        '_links': {
          'first': {
            'href': 'https://example.com/server/api/integration/externalsources/scopus/entries?query=test&page=0&size=10&sort=id,asc'
          },
          'self': {
            'href': 'https://example.com/server/api/integration/externalsources/scopus/entries?sort=id,ASC&page=0&size=10&query=test'
          },
          'next': {
            'href': 'https://example.com/server/api/integration/externalsources/scopus/entries?query=test&page=1&size=10&sort=id,asc'
          },
          'last': {
            'href': 'https://example.com/server/api/integration/externalsources/scopus/entries?query=test&page=1197160&size=10&sort=id,asc'
          },
          'page': [
            {
              'href': 'https://example.com/server/api/integration/externalsources/scopus/entryValues/2-s2.0-85130258665'
            }
          ]
        },
        'page': [
          {
            'id': '2-s2.0-85130258665',
            'type': 'externalSourceEntry',
            'display': 'Biological activities of endophytic fungi isolated from Annona muricata Linnaeus: a systematic review',
            'value': 'Biological activities of endophytic fungi isolated from Annona muricata Linnaeus: a systematic review',
            'externalSource': 'scopus',
            'metadata': {
              'dc.contributor.author': [
                {
                  'uuid': 'cbceba09-4c12-4968-ab02-2f77a985b422',
                  'language': null,
                  'value': 'Silva I.M.M.',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.date.issued': [
                {
                  'uuid': 'e8d3c306-ce21-43e2-8a80-5f257cc3b7ea',
                  'language': null,
                  'value': '2024-01-01',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.description.abstract': [
                {
                  'uuid': 'c9ee4076-c602-4c1d-ab1a-60bbdd0dd511',
                  'language': null,
                  'value': 'This systematic review integrates the data available in the literature regarding the biological activities of the extracts of endophytic fungi isolated from Annona muricata and their secondary metabolites. The search was performed using four electronic databases, and studies’ quality was evaluated using an adapted assessment tool. The initial database search yielded 436 results; ten studies were selected for inclusion. The leaf was the most studied part of the plant (in nine studies); Periconia sp. was the most tested fungus (n = 4); the most evaluated biological activity was anticancer (n = 6), followed by antiviral (n = 3). Antibacterial, antifungal, and antioxidant activities were also tested. Terpenoids or terpenoid hybrid compounds were the most abundant chemical metabolites. Phenolic compounds, esters, alkaloids, saturated and unsaturated fatty acids, aromatic compounds, and peptides were also reported. The selected studies highlighted the biotechnological potentiality of the endophytic fungi extracts from A. muricata. Consequently, it can be considered a promising source of biological compounds with antioxidant effects and active against different microorganisms and cancer cells. Further research is needed involving different plant tissues, other microorganisms, such as SARS-CoV-2, and different cancer cells.',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.identifier.doi': [
                {
                  'uuid': '95ec26be-c1b4-4c4a-b12d-12421a4f181d',
                  'language': null,
                  'value': '10.1590/1519-6984.259525',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.identifier.pmid': [
                {
                  'uuid': 'd6913cd6-1007-4013-b486-3f07192bc739',
                  'language': null,
                  'value': '35588520',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.identifier.scopus': [
                {
                  'uuid': '6386a1f6-84ba-431d-a583-e16d19af8db0',
                  'language': null,
                  'value': '2-s2.0-85130258665',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.relation.grantno': [
                {
                  'uuid': 'bcafd7b0-827d-4abb-8608-95dc40a8e58a',
                  'language': null,
                  'value': 'undefined',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.relation.ispartof': [
                {
                  'uuid': '680819c8-c143-405f-9d09-f84d2d5cd338',
                  'language': null,
                  'value': 'Brazilian Journal of Biology',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.relation.ispartofseries': [
                {
                  'uuid': '06634104-127b-44f6-9dcc-efae24b74bd1',
                  'language': null,
                  'value': 'Brazilian Journal of Biology',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.relation.issn': [
                {
                  'uuid': '5f6cce46-2538-49e9-8ed0-a3988dcac6c5',
                  'language': null,
                  'value': '15196984',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.subject': [
                {
                  'uuid': '0b6fbc77-de54-4f4a-b317-3d74a429f22a',
                  'language': null,
                  'value': 'biological products | biotechnology | mycology | soursop',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.title': [
                {
                  'uuid': '4c0fa3d3-1a8c-4302-a772-4a4d0408df35',
                  'language': null,
                  'value': 'Biological activities of endophytic fungi isolated from Annona muricata Linnaeus: a systematic review',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'dc.type': [
                {
                  'uuid': '5b6e0337-6f79-4574-a720-536816d1dc6e',
                  'language': null,
                  'value': 'Journal',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'oaire.citation.volume': [
                {
                  'uuid': 'b88b0246-61a9-4aca-917f-68afc8ead7d8',
                  'language': null,
                  'value': '84',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'oairecerif.affiliation.orgunit': [
                {
                  'uuid': '487c0fbc-3622-4cc7-a5fa-4edf780c6a21',
                  'language': null,
                  'value': 'Universidade Federal do Reconcavo da Bahia',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'oairecerif.citation.number': [
                {
                  'uuid': '90808bdd-f456-4ba3-91aa-b82fb3c453f6',
                  'language': null,
                  'value': 'e259525',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'person.identifier.orcid': [
                {
                  'uuid': 'e533d0d2-cf26-4c3e-b5ae-cabf497dfb6b',
                  'language': null,
                  'value': '#PLACEHOLDER_PARENT_METADATA_VALUE#',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ],
              'person.identifier.scopus-author-id': [
                {
                  'uuid': '4faf0be5-0226-4d4f-92a0-938397c4ec02',
                  'language': null,
                  'value': '42561627000',
                  'place': -1,
                  'authority': null,
                  'confidence': -1
                }
              ]
            },
            '_links': {
              'self': {
                'href': 'https://example.com/server/api/integration/externalsources/scopus/entryValues/2-s2.0-85130258665'
              }
            }
          }
        ]
      },
      'statusCode': 200
    };
    const errorObj = {
      errorMessage: 'Http failure response for ' +
        'https://example.com/server/api/integration/externalsources/pubmed/entries?sort=id,ASC&page=0&size=10&query=test: 500 OK',
      statusCode: 500,
      timeCompleted: 1656950434666,
      errors: [{
        'message': 'Internal Server Error', 'paths': ['/server/api/integration/externalsources/pubmed/entries']
      }]
    };
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionImportExternalComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      scheduler = getTestScheduler();
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('REST endpoint returns a 200 response with valid content', () => {
      mockExternalSourceService.getExternalSourceEntries.and.returnValue(createSuccessfulRemoteDataObject$(paginatedData.payload));
      const expectedEntries = createSuccessfulRemoteDataObject(paginatedData.payload);
      spyOn(routeServiceStub, 'getQueryParameterValue').and.callFake((param) => {
        if (param === 'entity') {
          return observableOf('Publication');
        } else if (param === 'sourceId') {
          return observableOf('scopus');
        } else if (param === 'query') {
          return observableOf('test');
        }
        return observableOf({});
      });
      fixture.detectChanges();

      expect(comp.isLoading$.value).toBe(false);
      expect(comp.entriesRD$.value).toEqual(expectedEntries);
      const viewableCollection = fixture.debugElement.query(By.css('ds-viewable-collection'));
      expect(viewableCollection).toBeTruthy();
    });

    it('REST endpoint returns a 200 response with no results', () => {
      mockExternalSourceService.getExternalSourceEntries.and.returnValue(createSuccessfulRemoteDataObject$(createPaginatedList([])));
      const expectedEntries = createSuccessfulRemoteDataObject(createPaginatedList([]));
      spyOn(routeServiceStub, 'getQueryParameterValue').and.callFake((param) => {
        if (param === 'entity') {
          return observableOf('Publication');
        }
        return observableOf({});
      });
      fixture.detectChanges();

      expect(comp.isLoading$.value).toBe(false);
      expect(comp.entriesRD$.value).toEqual(expectedEntries);
      const noDataAlert = fixture.debugElement.query(By.css('[data-test="empty-external-entry-list"]'));
      expect(noDataAlert).toBeTruthy();
    });

    it('REST endpoint returns a 500 error', () => {
      mockExternalSourceService.getExternalSourceEntries.and.returnValue(createFailedRemoteDataObject$(
        errorObj.errorMessage,
        errorObj.statusCode,
        errorObj.timeCompleted
      ));
      spyOn(routeServiceStub, 'getQueryParameterValue').and.callFake((param) => {
        if (param === 'entity') {
          return observableOf('Publication');
        } else if (param === 'sourceId') {
          return observableOf('pubmed');
        } else if (param === 'query') {
          return observableOf('test');
        }
        return observableOf({});
      });
      fixture.detectChanges();

      expect(comp.isLoading$.value).toBe(false);
      expect(comp.entriesRD$.value.statusCode).toEqual(500);
      const noDataAlert = fixture.debugElement.query(By.css('[data-test="empty-external-error-500"]'));
      expect(noDataAlert).toBeTruthy();
    });
  });

});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

}
