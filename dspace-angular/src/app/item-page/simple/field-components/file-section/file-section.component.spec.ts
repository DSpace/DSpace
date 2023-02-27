import { FileSectionComponent } from './file-section.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../../../shared/mocks/translate-loader.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { VarDirective } from '../../../../shared/utils/var.directive';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BitstreamDataService } from '../../../../core/data/bitstream-data.service';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { By } from '@angular/platform-browser';
import { Bitstream } from '../../../../core/shared/bitstream.model';
import { of as observableOf } from 'rxjs';
import { MockBitstreamFormat1 } from '../../../../shared/mocks/item.mock';
import { FileSizePipe } from '../../../../shared/utils/file-size-pipe';
import { PageInfo } from '../../../../core/shared/page-info.model';
import { MetadataFieldWrapperComponent } from '../../../../shared/metadata-field-wrapper/metadata-field-wrapper.component';
import { createPaginatedList } from '../../../../shared/testing/utils.test';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../../shared/testing/notifications-service.stub';
import { APP_CONFIG } from 'src/config/app-config.interface';
import { environment } from 'src/environments/environment';

describe('FileSectionComponent', () => {
  let comp: FileSectionComponent;
  let fixture: ComponentFixture<FileSectionComponent>;

  const bitstreamDataService = jasmine.createSpyObj('bitstreamDataService', {
    findAllByItemAndBundleName: createSuccessfulRemoteDataObject$(createPaginatedList([]))
  });

  const mockBitstream: Bitstream = Object.assign(new Bitstream(),
    {
      sizeBytes: 10201,
      content: 'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713/content',
      format: observableOf(MockBitstreamFormat1),
      bundleName: 'ORIGINAL',
      _links: {
        self: {
          href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713'
        },
        content: {
          href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713/content'
        }
      },
      id: 'cf9b0c8e-a1eb-4b65-afd0-567366448713',
      uuid: 'cf9b0c8e-a1eb-4b65-afd0-567366448713',
      type: 'bitstream',
      metadata: {
        'dc.title': [
          {
            language: null,
            value: 'test_word.docx'
          }
        ]
      }
    });

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderMock
        }
      }), BrowserAnimationsModule],
      declarations: [FileSectionComponent, VarDirective, FileSizePipe, MetadataFieldWrapperComponent],
      providers: [
        { provide: BitstreamDataService, useValue: bitstreamDataService },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: APP_CONFIG, useValue: environment }
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(FileSectionComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  }));

  describe('when the bitstreams are loading', () => {
    beforeEach(() => {
      comp.bitstreams$.next([mockBitstream]);
      comp.isLoading = true;
      fixture.detectChanges();
    });

    it('should display a loading component', () => {
      const loading = fixture.debugElement.query(By.css('ds-themed-loading'));
      expect(loading.nativeElement).toBeDefined();
    });
  });

  describe('when the "Show more" button is clicked', () => {

    beforeEach(() => {
      comp.bitstreams$.next([mockBitstream]);
      comp.currentPage = 1;
      comp.isLastPage = false;
      fixture.detectChanges();
    });

    it('should call the service to retrieve more bitstreams', () => {
      const viewMore = fixture.debugElement.query(By.css('.bitstream-view-more'));
      viewMore.triggerEventHandler('click', null);
      expect(bitstreamDataService.findAllByItemAndBundleName).toHaveBeenCalled();
    });

    it('one bitstream should be on the page', () => {
      const viewMore = fixture.debugElement.query(By.css('.bitstream-view-more'));
      viewMore.triggerEventHandler('click', null);
      const fileDownloadLink = fixture.debugElement.queryAll(By.css('ds-themed-file-download-link'));
      expect(fileDownloadLink.length).toEqual(1);
    });

    describe('when it is then clicked again', () => {
      beforeEach(() => {
        bitstreamDataService.findAllByItemAndBundleName.and.returnValue(createSuccessfulRemoteDataObject$(createPaginatedList([mockBitstream])));
        const viewMore = fixture.debugElement.query(By.css('.bitstream-view-more'));
        viewMore.triggerEventHandler('click', null);
        fixture.detectChanges();

      });
      it('should contain another bitstream', () => {
        const fileDownloadLink = fixture.debugElement.queryAll(By.css('ds-themed-file-download-link'));
        expect(fileDownloadLink.length).toEqual(2);
      });
    });
  });

  describe('when its the last page of bitstreams', () => {
    beforeEach(() => {
      comp.bitstreams$.next([mockBitstream]);
      comp.isLastPage = true;
      comp.currentPage = 2;
      fixture.detectChanges();
    });

    it('should not contain a view more link', () => {
      const viewMore = fixture.debugElement.query(By.css('.bitstream-view-more'));
      expect(viewMore).toBeNull();
    });

    it('should contain a view less link', () => {
      const viewLess = fixture.debugElement.query(By.css('.bitstream-collapse'));
      expect(viewLess).toBeDefined();
    });

    it('clicking on the view less link should reset the pages and call getNextPage()', () => {
      const pageInfo = Object.assign(new PageInfo(), {
        elementsPerPage: 3,
        totalElements: 5,
        totalPages: 2,
        currentPage: 1,
        _links: {
          self: { href: 'https://rest.api/core/bitstreams/' },
          next: { href: 'https://rest.api/core/bitstreams?page=2' }
        }
      });
      const PaginatedList = Object.assign(createPaginatedList([mockBitstream]), {
        pageInfo: pageInfo
      });
      bitstreamDataService.findAllByItemAndBundleName.and.returnValue(createSuccessfulRemoteDataObject$(PaginatedList));
      const viewLess = fixture.debugElement.query(By.css('.bitstream-collapse'));
      viewLess.triggerEventHandler('click', null);
      expect(bitstreamDataService.findAllByItemAndBundleName).toHaveBeenCalled();
      expect(comp.currentPage).toBe(1);
      expect(comp.isLastPage).toBeFalse();
    });

  });
});
