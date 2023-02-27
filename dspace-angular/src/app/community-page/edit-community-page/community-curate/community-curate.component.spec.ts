import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { ActivatedRoute } from '@angular/router';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { CommunityCurateComponent } from './community-curate.component';
import { Community } from '../../../core/shared/community.model';

describe('CommunityCurateComponent', () => {
  let comp: CommunityCurateComponent;
  let fixture: ComponentFixture<CommunityCurateComponent>;
  let debugEl: DebugElement;

  let routeStub;
  let dsoNameService;

  const community = Object.assign(new Community(), {
    metadata: {'dc.title': ['Community Name'], 'dc.identifier.uri': [ { value: '123456789/1'}]}
  });

  beforeEach(waitForAsync(() => {
    routeStub = {
      parent: {
        data: observableOf({
          dso: createSuccessfulRemoteDataObject(community)
        })
      }
    };

    dsoNameService = jasmine.createSpyObj('dsoNameService', {
      getName: 'Community Name'
    });

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [CommunityCurateComponent],
      providers: [
        {provide: ActivatedRoute, useValue: routeStub},
        {provide: DSONameService, useValue: dsoNameService}
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommunityCurateComponent);
    comp = fixture.componentInstance;
    debugEl = fixture.debugElement;

    fixture.detectChanges();
  });
  describe('init', () => {
    it('should initialise the comp', () => {
      expect(comp).toBeDefined();
      expect(debugEl.nativeElement.innerHTML).toContain('ds-curation-form');
    });
    it('should contain the community information provided in the route', () => {
      comp.dsoRD$.subscribe((value) => {
        expect(value.payload.handle
        ).toEqual('123456789/1');
      });
      comp.communityName$.subscribe((value) => {
        expect(value).toEqual('Community Name');
      });
    });
  });
});
