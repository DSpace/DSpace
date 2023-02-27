import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { CollectionCurateComponent } from './collection-curate.component';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { Collection } from '../../../core/shared/collection.model';
import { ActivatedRoute } from '@angular/router';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';

describe('CollectionCurateComponent', () => {
  let comp: CollectionCurateComponent;
  let fixture: ComponentFixture<CollectionCurateComponent>;
  let debugEl: DebugElement;

  let routeStub;
  let dsoNameService;

  const collection = Object.assign(new Collection(), {
    metadata: {'dc.title': ['Collection Name'], 'dc.identifier.uri': [ { value: '123456789/1'}]}
  });

  beforeEach(waitForAsync(() => {
    routeStub = {
      parent: {
        data: observableOf({
          dso: createSuccessfulRemoteDataObject(collection)
        })
      }
    };

    dsoNameService = jasmine.createSpyObj('dsoNameService', {
      getName: 'Collection Name'
    });

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [CollectionCurateComponent],
      providers: [
        {provide: ActivatedRoute, useValue: routeStub},
        {provide: DSONameService, useValue: dsoNameService}
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionCurateComponent);
    comp = fixture.componentInstance;
    debugEl = fixture.debugElement;

    fixture.detectChanges();
  });
  describe('init', () => {
    it('should initialise the comp', () => {
      expect(comp).toBeDefined();
      expect(debugEl.nativeElement.innerHTML).toContain('ds-curation-form');
    });
    it('should contain the collection information provided in the route', () => {
      comp.dsoRD$.subscribe((value) => {
        expect(value.payload.handle
        ).toEqual('123456789/1');
      });
      comp.collectionName$.subscribe((value) => {
        expect(value).toEqual('Collection Name');
      });
    });
  });
});
