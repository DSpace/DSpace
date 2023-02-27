import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';
import { StartsWithDateComponent } from './starts-with-date.component';
import { ActivatedRouteStub } from '../../testing/active-router.stub';
import { EnumKeysPipe } from '../../utils/enum-keys-pipe';
import { RouterStub } from '../../testing/router.stub';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../testing/pagination-service.stub';

describe('StartsWithDateComponent', () => {
  let comp: StartsWithDateComponent;
  let fixture: ComponentFixture<StartsWithDateComponent>;
  let route: ActivatedRoute;
  let router: Router;
  let paginationService;

  const options = [2019, 2018, 2017, 2016, 2015];

  const activatedRouteStub = Object.assign(new ActivatedRouteStub(), {
    params: observableOf({}),
    queryParams: observableOf({})
  });

  paginationService = new PaginationServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [StartsWithDateComponent, EnumKeysPipe],
      providers: [
        { provide: 'startsWithOptions', useValue: options },
        { provide: 'paginationId', useValue: 'page-id' },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: PaginationService, useValue: paginationService },
        { provide: Router, useValue: new RouterStub() }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StartsWithDateComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
    route = (comp as any).route;
    router = (comp as any).router;
  });

  it('should create a FormGroup containing a startsWith FormControl', () => {
    expect(comp.formData.value.startsWith).toBeDefined();
  });

  describe('when selecting the first option in the year dropdown', () => {
    let select;

    beforeEach(() => {
      select = fixture.debugElement.query(By.css('select#year-select')).nativeElement;
      select.value = select.options[0].value;
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    });

    it('should set startsWith to undefined', () => {
      expect(comp.startsWith).toBeUndefined();
    });

    it('should not add a startsWith query parameter', () => {
      route.queryParams.subscribe((params) => {
        expect(params.startsWith).toBeUndefined();
      });
    });
  });

  describe('when selecting the second option in the year dropdown', () => {
    let select;
    let input;
    let expectedValue;

    beforeEach(() => {
      expectedValue = '' + options[0];

      select = fixture.debugElement.query(By.css('select#year-select')).nativeElement;
      input = fixture.debugElement.query(By.css('input')).nativeElement;
      select.value = select.options[1].value;
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    });

    it('should set startsWith to the correct value', () => {
      expect(comp.startsWith).toEqual(expectedValue);
    });

    it('should add a startsWith query parameter', () => {
      expect(paginationService.updateRoute).toHaveBeenCalledWith('page-id', {page: 1}, {startsWith: expectedValue});
    });

    it('should automatically fill in the input field', () => {
      expect(input.value).toEqual(expectedValue);
    });

    describe('and selecting the first option in the month dropdown', () => {
      let monthSelect;

      beforeEach(() => {
        monthSelect = fixture.debugElement.query(By.css('select#month-select')).nativeElement;
        monthSelect.value = monthSelect.options[0].value;
        monthSelect.dispatchEvent(new Event('change'));
        fixture.detectChanges();
      });

      it('should set startsWith to the correct value', () => {
        expect(comp.startsWith).toEqual(expectedValue);
      });

      it('should add a startsWith query parameter', () => {
        expect(paginationService.updateRoute).toHaveBeenCalledWith('page-id', {page: 1}, {startsWith: expectedValue});
      });

      it('should automatically fill in the input field', () => {
        expect(input.value).toEqual(expectedValue);
      });
    });

    describe('and selecting the second option in the month dropdown', () => {
      let monthSelect;

      beforeEach(() => {
        expectedValue = `${options[0]}-01`;
        monthSelect = fixture.debugElement.query(By.css('select#month-select')).nativeElement;
        monthSelect.value = monthSelect.options[1].value;
        monthSelect.dispatchEvent(new Event('change'));
        fixture.detectChanges();
      });

      it('should set startsWith to the correct value', () => {
        expect(comp.startsWith).toEqual(expectedValue);
      });

      it('should add a startsWith query parameter', () => {
        expect(paginationService.updateRoute).toHaveBeenCalledWith('page-id', {page: 1}, {startsWith: expectedValue});
      });

      it('should automatically fill in the input field', () => {
        expect(input.value).toEqual(expectedValue);
      });
    });
  });

  describe('when filling in the input form', () => {
    let form;
    const expectedValue = '2015';
    const extras: NavigationExtras = {
      queryParams: Object.assign({ startsWith: expectedValue }),
      queryParamsHandling: 'merge'
    };

    beforeEach(() => {
      form = fixture.debugElement.query(By.css('form'));
      comp.formData.value.startsWith = expectedValue;
      form.triggerEventHandler('ngSubmit', null);
      fixture.detectChanges();
    });

    it('should set startsWith to the correct value', () => {
      expect(comp.startsWith).toEqual(expectedValue);
    });

    it('should add a startsWith query parameter', () => {
      expect(paginationService.updateRoute).toHaveBeenCalledWith('page-id', {page: 1}, {startsWith: expectedValue});
    });
  });

});
