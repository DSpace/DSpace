import { ObjectCollectionComponent } from './object-collection.component';
import { By } from '@angular/platform-browser';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { RouterStub } from '../testing/router.stub';
import { ViewMode } from '../../core/shared/view-mode.model';

describe('ObjectCollectionComponent', () => {
  let fixture: ComponentFixture<ObjectCollectionComponent>;
  let objectCollectionComponent: ObjectCollectionComponent;

  const queryParam = 'test query';
  const scopeParam = '7669c72a-3f2a-451f-a3b9-9210e7a4c02f';
  const activatedRouteStub = {
    queryParams: observableOf({
      query: queryParam,
      scope: scopeParam
    })
  };
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ObjectCollectionComponent],
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useClass: RouterStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();  // compile template and css
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ObjectCollectionComponent);
    objectCollectionComponent = fixture.componentInstance;
  }));

  it('should only show the grid component when the viewmode is set to grid', () => {
    objectCollectionComponent.currentMode$ = observableOf(ViewMode.GridElement);

    expect(fixture.debugElement.query(By.css('ds-object-grid'))).toBeDefined();
    expect(fixture.debugElement.query(By.css('ds-object-list'))).toBeNull();
  });

  it('should only show the list component when the viewmode is set to list', () => {
    objectCollectionComponent.currentMode$ = observableOf(ViewMode.ListElement);

    expect(fixture.debugElement.query(By.css('ds-object-list'))).toBeDefined();
    expect(fixture.debugElement.query(By.css('ds-object-grid'))).toBeNull();
  });
  it('should set fallback placeholder font size during test', () => {
    objectCollectionComponent.currentMode$ = observableOf(ViewMode.ListElement);
    expect(fixture.debugElement.query(By.css('thumb-font-3'))).toBeDefined();

  });
});
