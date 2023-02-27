// Load the implementations that should be tested
import { CommonModule } from '@angular/common';

import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';

import { ComponentFixture, fakeAsync, inject, TestBed, tick, waitForAsync } from '@angular/core/testing';

import { RouterTestingModule } from '@angular/router/testing';

import { ActivatedRoute, Router } from '@angular/router';
import { By } from '@angular/platform-browser';

import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { StoreModule } from '@ngrx/store';

import { NgxPaginationModule } from 'ngx-pagination';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { PaginationComponent } from './pagination.component';
import { PaginationComponentOptions } from './pagination-component-options.model';

import { TranslateLoaderMock } from '../mocks/translate-loader.mock';
import { HostWindowServiceMock } from '../mocks/host-window-service.mock';
import { MockActivatedRoute } from '../mocks/active-router.mock';
import { RouterMock } from '../mocks/router.mock';

import { HostWindowService } from '../host-window.service';
import { EnumKeysPipe } from '../utils/enum-keys-pipe';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';

import { createTestComponent } from '../testing/utils.test';
import { storeModuleConfig } from '../../app.reducer';
import { PaginationService } from '../../core/pagination/pagination.service';
import { BehaviorSubject } from 'rxjs';
import { FindListOptions } from '../../core/data/find-list-options.model';

function expectPages(fixture: ComponentFixture<any>, pagesDef: string[]): void {
  const de = fixture.debugElement.query(By.css('.pagination'));
  const pages = de.nativeElement.querySelectorAll('li');

  expect(pages.length).toEqual(pagesDef.length);

  for (let i = 0; i < pagesDef.length; i++) {
    const pageDef = pagesDef[i];
    const classIndicator = pageDef.charAt(0);

    if (classIndicator === '+') {
      expect(pages[i].classList.contains('active')).toBeTruthy();
      expect(pages[i].classList.contains('disabled')).toBeFalsy();
      expect(normalizeText(pages[i].textContent)).toEqual(normalizeText(pageDef));
    } else if (classIndicator === '-') {
      expect(pages[i].classList.contains('active')).toBeFalsy();
      expect(pages[i].classList.contains('disabled')).toBeTruthy();
      expect(normalizeText(pages[i].textContent)).toEqual(normalizeText(pageDef));
      if (normalizeText(pages[i].textContent) !== '...') {
        expect(pages[i].querySelector('a').getAttribute('tabindex')).toEqual('-1');
      }
    } else {
      expect(pages[i].classList.contains('active')).toBeFalsy();
      expect(pages[i].classList.contains('disabled')).toBeFalsy();
      expect(normalizeText(pages[i].textContent)).toEqual(normalizeText(pageDef));
      if (normalizeText(pages[i].textContent) !== '...') {
        expect(pages[i].querySelector('a').hasAttribute('tabindex')).toBeFalsy();
      }
    }
  }
}

function changePageSize(fixture: ComponentFixture<any>, pageSize: string): void {
  const buttonEl = fixture.nativeElement.querySelector('#paginationControls');

  buttonEl.click();

  const dropdownMenu = fixture.debugElement.query(By.css('#paginationControlsDropdownMenu'));
  const buttons = dropdownMenu.nativeElement.querySelectorAll('button');

  for (const button of buttons) {
    if (button.textContent.trim() === pageSize) {
      button.click();
      fixture.detectChanges();
      break;
    }
  }
}

function changePage(fixture: ComponentFixture<any>, idx: number): void {
  const de = fixture.debugElement.query(By.css('.pagination'));
  const buttons = de.nativeElement.querySelectorAll('li');

  buttons[idx].querySelector('a').click();
  fixture.detectChanges();
}

function normalizeText(txt: string): string {
  const matches = txt.match(/([0-9«»]|\.{3})/);
  return matches ? matches[0] : '';
}

describe('Pagination component', () => {

  let testComp: TestComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let de: DebugElement;
  let html;
  let hostWindowServiceStub: HostWindowServiceMock;

  let activatedRouteStub: MockActivatedRoute;
  let routerStub: RouterMock;

  let paginationService;

  // Define initial state and test state
  const _initialState = { width: 1600, height: 770 };

  const pagination = new PaginationComponentOptions();
  pagination.currentPage = 1;
  pagination.pageSize = 10;

  const sort = new SortOptions('score', SortDirection.DESC);
  const findlistOptions = Object.assign(new FindListOptions(), { currentPage: 1, elementsPerPage: 10 });
  let currentPagination;
  let currentSort;
  let currentFindListOptions;

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    activatedRouteStub = new MockActivatedRoute();
    routerStub = new RouterMock();
    hostWindowServiceStub = new HostWindowServiceMock(_initialState.width);

    currentPagination = new BehaviorSubject<PaginationComponentOptions>(pagination);
    currentSort = new BehaviorSubject<SortOptions>(sort);
    currentFindListOptions = new BehaviorSubject<FindListOptions>(findlistOptions);


    paginationService = jasmine.createSpyObj('PaginationService', {
      getCurrentPagination: currentPagination,
      getCurrentSort: currentSort,
      getFindListOptions: currentFindListOptions,
      resetPage: {},
      updateRoute: {},
      updateRouteWithUrl: {}
    });

    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        StoreModule.forRoot({}, storeModuleConfig),
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        }),
        NgxPaginationModule,
        NgbModule,
        RouterTestingModule.withRoutes([
          { path: 'home', component: TestComponent }
        ])],
      declarations: [
        PaginationComponent,
        TestComponent,
        EnumKeysPipe
      ], // declare the test component
      providers: [
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: routerStub },
        { provide: HostWindowService, useValue: hostWindowServiceStub },
        { provide: PaginationService, useValue: paginationService },
        ChangeDetectorRef,
        PaginationComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

  }));

  describe('when showPaginator is false', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
      <ds-pagination #p='paginationComponent'
                     [paginationOptions]='paginationOptions'
                     [sortOptions]='sortOptions'
                     [collectionSize]='collectionSize'
                     (pageChange)='pageChanged($event)'
                     (pageSizeChange)='pageSizeChanged($event)'
                      >
        <ul>
          <li *ngFor='let item of collection | paginate: { itemsPerPage: paginationOptions.pageSize,
                      currentPage: paginationOptions.currentPage, totalItems: collectionSize }'> {{item}} </li>
        </ul>
      </ds-pagination>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    it('should create Pagination Component', inject([PaginationComponent], (app: PaginationComponent) => {
      expect(app).toBeDefined();
    }));

    it('should render', () => {
      expect(testComp.paginationOptions.id).toEqual('test');
      expect(testComp.paginationOptions.currentPage).toEqual(1);
      expect(testComp.paginationOptions.pageSize).toEqual(10);
      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '» Next']);
    });

    it('should render and respond to page change', () => {
      testComp.collectionSize = 30;
      testFixture.detectChanges();


      currentPagination.next(Object.assign(new PaginationComponentOptions(), pagination, {currentPage: 3}));
      testFixture.detectChanges();

      expectPages(testFixture, ['« Previous', '1', '2', '+3', '-» Next']);

      currentPagination.next(Object.assign(new PaginationComponentOptions(), pagination, {currentPage: 2}));
      testFixture.detectChanges();

      expectPages(testFixture, ['« Previous', '1', '+2', '3', '» Next']);
    });

    it('should render and respond to collectionSize change', () => {

      testComp.collectionSize = 30;
      testFixture.detectChanges();
      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '» Next']);

      testComp.collectionSize = 40;
      testFixture.detectChanges();
      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '4', '» Next']);
    });

    it('should render and respond to pageSize change', () => {
      const paginationComponent: PaginationComponent = testFixture.debugElement.query(By.css('ds-pagination')).references.p;

      testComp.collectionSize = 30;
      testFixture.detectChanges();
      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '» Next']);

      currentPagination.next(Object.assign(new PaginationComponentOptions(), pagination, {pageSize: 5}));
      testFixture.detectChanges();

      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '4', '5', '6', '» Next']);

      currentPagination.next(Object.assign(new PaginationComponentOptions(), pagination, {pageSize: 10}));
      testFixture.detectChanges();
      expectPages(testFixture, ['-« Previous', '+1', '2', '3', '» Next']);

      currentPagination.next(Object.assign(new PaginationComponentOptions(), pagination, {pageSize: 20}));
      testFixture.detectChanges();
      expectPages(testFixture, ['-« Previous', '+1', '2', '» Next']);
    });

    it('should emit pageSizeChange event with correct value', fakeAsync(() => {
      const paginationComponent: PaginationComponent = testFixture.debugElement.query(By.css('ds-pagination')).references.p;

      spyOn(testComp, 'pageSizeChanged');

      testComp.pageSizeChanged(5);
      tick();

      expect(testComp.pageSizeChanged).toHaveBeenCalledWith(5);
    }));

    it('should call the updateRoute method on the paginationService with the correct params', fakeAsync(() => {
      testComp.collectionSize = 60;

      changePage(testFixture, 3);
      tick();
      expect(paginationService.updateRoute).toHaveBeenCalledWith('test', Object.assign({ page: '3'}), {},  false);

      changePage(testFixture, 0);
      tick();
      expect(paginationService.updateRoute).toHaveBeenCalledWith('test', Object.assign({ page: '2'}), {},  false);
    }));

    it('should set correct pageSize route parameters', fakeAsync(() => {
      routerStub = testFixture.debugElement.injector.get(Router) as any;

      testComp.collectionSize = 60;

      changePageSize(testFixture, '20');
      tick();
      expect(paginationService.updateRoute).toHaveBeenCalledWith('test', Object.assign({ pageId: 'test', page: 1, pageSize: 20}), {},  false);
    }));

    it('should respond to windows resize', () => {
      const paginationComponent: PaginationComponent = testFixture.debugElement.query(By.css('ds-pagination')).references.p;
      hostWindowServiceStub = testFixture.debugElement.injector.get(HostWindowService) as any;

      hostWindowServiceStub.setWidth(400);

      hostWindowServiceStub.isXs().subscribe((status) => {
        paginationComponent.isXs = status;
        testFixture.detectChanges();
        expectPages(testFixture, ['-« Previous', '+1', '2', '3', '4', '5', '-...', '10', '» Next']);
        de = testFixture.debugElement.query(By.css('ul.pagination'));
        expect(de.nativeElement.classList.contains('pagination-sm')).toBeTruthy();
      });
    });
  });

  describe('when showPaginator is true', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
      <ds-pagination #p='paginationComponent'
                     [paginationOptions]='paginationOptions'
                     [sortOptions]='sortOptions'
                     [collectionSize]='collectionSize'
                     (pageChange)='pageChanged($event)'
                     (pageSizeChange)='pageSizeChanged($event)'
                     [showPaginator]='false'
                     [objects]='objects'
                     (prev)="goPrev()"
                     (next)="goNext()"
                     >
        <ul>
          <li *ngFor='let item of collection | paginate: { itemsPerPage: paginationOptions.pageSize,
                      currentPage: paginationOptions.currentPage, totalItems: collectionSize }'> {{item}} </li>
        </ul>
      </ds-pagination>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    beforeEach(() => {
      testComp.showPaginator = false;
      testFixture.detectChanges();
    });

    describe('when clicking the previous arrow button', () => {
      beforeEach(() => {
        spyOn(testComp, 'goPrev');
        testFixture.detectChanges();
      });

      it('should call goPrev method', () => {
        const prev = testFixture.debugElement.query(By.css('#nav-prev'));
        testFixture.detectChanges();
        prev.triggerEventHandler('click', null);
        expect(testComp.goPrev).toHaveBeenCalled();
      });
    });

    describe('when clicking the next arrow button', () => {
      beforeEach(() => {
        spyOn(testComp, 'goNext');
        testFixture.detectChanges();
      });

      it('should call goNext method', () => {
        const next = testFixture.debugElement.query(By.css('#nav-next'));
        testFixture.detectChanges();
        next.triggerEventHandler('click', null);
        expect(testComp.goNext).toHaveBeenCalled();
      });
    });

    describe('check for prev and next button', () => {
      it('shoud have a previous button', () => {
        const prev = testFixture.debugElement.query(By.css('#nav-prev'));
        testFixture.detectChanges();
        expect(prev).toBeTruthy();
      });

      it('shoud have a next button', () => {
        const next = testFixture.debugElement.query(By.css('#nav-next'));
        testFixture.detectChanges();
        expect(next).toBeTruthy();
      });
    });
  });

});

// declare a test component
@Component({ selector: 'ds-test-cmp', template: '' })
class TestComponent {

  collection: string[] = [];
  collectionSize: number;
  paginationOptions = new PaginationComponentOptions();
  sortOptions = new SortOptions('dc.title', SortDirection.ASC);
  showPaginator: boolean;
  objects = {
    payload: {
      currentPage: 2,
      totalPages: 100
    }
  };

  constructor() {
    this.collection = Array.from(new Array(100), (x, i) => `item ${i + 1}`);
    this.collectionSize = 100;
    this.paginationOptions.id = 'test';
    this.showPaginator = false;
  }

  pageChanged(page) {
    this.paginationOptions.currentPage = page;
  }

  pageSizeChanged(pageSize) {
    this.paginationOptions.pageSize = pageSize;
  }

  goPrev() {
    this.objects.payload.currentPage --;
  }

  goNext() {
    this.objects.payload.currentPage ++;
  }
}
