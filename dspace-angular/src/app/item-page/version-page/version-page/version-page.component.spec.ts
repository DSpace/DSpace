import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VersionPageComponent } from './version-page.component';
import { ActivatedRoute } from '@angular/router';
import { ActivatedRouteStub } from '../../../shared/testing/active-router.stub';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { Item } from '../../../core/shared/item.model';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { createRelationshipsObservable } from '../../simple/item-types/shared/item.component.spec';
import { VersionDataService } from '../../../core/data/version-data.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Version } from '../../../core/shared/version.model';
import { RouterTestingModule } from '@angular/router/testing';
import { Component } from '@angular/core';

const mockItem: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
  metadata: [],
  relationships: createRelationshipsObservable(),
  uuid: 'item-uuid',
});

const mockVersion: Version = Object.assign(new Version(), {
  item: createSuccessfulRemoteDataObject$(mockItem),
  version: 1,
});

@Component({ template: '' })
class DummyComponent {
}

describe('VersionPageComponent', () => {
  let component: VersionPageComponent;
  let fixture: ComponentFixture<VersionPageComponent>;
  let authService: AuthService;

  const mockRoute = Object.assign(new ActivatedRouteStub(), {
    data: observableOf({dso: createSuccessfulRemoteDataObject(mockVersion)})
  });

  beforeEach(waitForAsync(() => {
    authService = jasmine.createSpyObj('authService', {
      isAuthenticated: observableOf(true),
      setRedirectUrl: {}
    });
    TestBed.configureTestingModule({
      declarations: [VersionPageComponent, DummyComponent],
      imports: [RouterTestingModule.withRoutes([{ path: 'items/item-uuid', component: DummyComponent, pathMatch: 'full' }])],
      providers: [
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: VersionDataService, useValue: {} },
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
  }));



  beforeEach(() => {
    fixture = TestBed.createComponent(VersionPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
