import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationExtras, Router } from '@angular/router';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { SearchService } from '../core/shared/search/search.service';
import { TranslateLoaderMock } from '../shared/mocks/translate-loader.mock';

import { SearchNavbarComponent } from './search-navbar.component';
import { PaginationServiceStub } from '../shared/testing/pagination-service.stub';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserOnlyMockPipe } from '../shared/testing/browser-only-mock.pipe';

describe('SearchNavbarComponent', () => {
  let component: SearchNavbarComponent;
  let fixture: ComponentFixture<SearchNavbarComponent>;
  let mockSearchService: any;
  let router: Router;
  let routerStub;
  let paginationService;

  beforeEach(waitForAsync(() => {
    mockSearchService = {
      getSearchLink() {
        return '/search';
      }
    };

    routerStub = {
      navigate: (commands) => commands
    };

    paginationService = new PaginationServiceStub();

    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ReactiveFormsModule,
        BrowserAnimationsModule,
        RouterTestingModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })],
      declarations: [
        SearchNavbarComponent,
        BrowserOnlyMockPipe,
      ],
      providers: [
        { provide: SearchService, useValue: mockSearchService }
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchNavbarComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('when you click on search icon', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component, 'expand').and.callThrough();
      spyOn(component, 'onSubmit').and.callThrough();
      spyOn(router, 'navigate');
      const searchIcon = fixture.debugElement.query(By.css('#search-navbar-container form .submit-icon'));
      searchIcon.triggerEventHandler('click', {
        preventDefault: () => {/**/
        }
      });
      tick();
      fixture.detectChanges();
    }));

    it('input expands', () => {
      expect(component.expand).toHaveBeenCalled();
    });

    describe('empty query', () => {
      describe('press submit button', () => {
        beforeEach(fakeAsync(() => {
          const searchIcon = fixture.debugElement.query(By.css('#search-navbar-container form .submit-icon'));
          searchIcon.triggerEventHandler('click', {
            preventDefault: () => {/**/
            }
          });
          tick();
          fixture.detectChanges();
        }));
        it('to search page with empty query', () => {
          const extras: NavigationExtras = {queryParams: { query: '' }, queryParamsHandling: 'merge'};
          expect(component.onSubmit).toHaveBeenCalledWith({ query: '' });
          expect(router.navigate).toHaveBeenCalledWith(['search'], extras);
        });
      });
    });

    describe('fill in some query', () => {
      let searchInput;
      beforeEach(async () => {
        await fixture.whenStable();
        fixture.detectChanges();
        searchInput = fixture.debugElement.query(By.css('#search-navbar-container form input'));
        searchInput.nativeElement.value = 'test';
        searchInput.nativeElement.dispatchEvent(new Event('input'));
        fixture.detectChanges();
      });
      describe('press submit button', () => {
        beforeEach(fakeAsync(() => {
          const searchIcon = fixture.debugElement.query(By.css('#search-navbar-container form .submit-icon'));
          searchIcon.triggerEventHandler('click', null);
          tick();
          fixture.detectChanges();
        }));
        it('to search page with query', async () => {
          const extras: NavigationExtras = { queryParams: { query: 'test' }, queryParamsHandling: 'merge'};
          expect(component.onSubmit).toHaveBeenCalledWith({ query: 'test' });

          expect(router.navigate).toHaveBeenCalledWith(['search'], extras);
        });
      });
    });

  });
});
