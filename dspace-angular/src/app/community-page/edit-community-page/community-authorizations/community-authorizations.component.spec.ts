import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { cold } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';

import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { CommunityAuthorizationsComponent } from './community-authorizations.component';
import { Collection } from '../../../core/shared/collection.model';

describe('CommunityAuthorizationsComponent', () => {
  let comp: CommunityAuthorizationsComponent<DSpaceObject>;
  let fixture: ComponentFixture<CommunityAuthorizationsComponent<any>>;

  const community = Object.assign(new Collection(), {
    uuid: 'community',
    id: 'community',
    _links: {
      self: { href: 'community-selflink' }
    }
  });

  const communityRD = createSuccessfulRemoteDataObject(community);

  const routeStub = {
    parent: {
      parent: {
        data: observableOf({
          dso: communityRD
        })
      }
    }
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule
      ],
      declarations: [CommunityAuthorizationsComponent],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        ChangeDetectorRef,
        CommunityAuthorizationsComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommunityAuthorizationsComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    comp = null;
    fixture.destroy();
  });

  it('should create', () => {
    expect(comp).toBeTruthy();
  });

  it('should init dso remote data properly', (done) => {
    const expected = cold('(a|)', { a: communityRD });
    expect(comp.dsoRD$).toBeObservable(expected);
    done();
  });
});
