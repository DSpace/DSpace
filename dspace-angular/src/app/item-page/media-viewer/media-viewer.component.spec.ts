import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Bitstream } from '../../core/shared/bitstream.model';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { of as observableOf } from 'rxjs';
import { By } from '@angular/platform-browser';
import { MediaViewerComponent } from './media-viewer.component';
import { MockBitstreamFormat1 } from '../../shared/mocks/item.mock';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BitstreamDataService } from '../../core/data/bitstream-data.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { MediaViewerItem } from '../../core/shared/media-viewer-item.model';
import { VarDirective } from '../../shared/utils/var.directive';
import { MetadataFieldWrapperComponent } from '../../shared/metadata-field-wrapper/metadata-field-wrapper.component';
import { FileSizePipe } from '../../shared/utils/file-size-pipe';

describe('MediaViewerComponent', () => {
  let comp: MediaViewerComponent;
  let fixture: ComponentFixture<MediaViewerComponent>;

  const mockBitstream: Bitstream = Object.assign(new Bitstream(), {
    sizeBytes: 10201,
    content:
      'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713/content',
    format: observableOf(MockBitstreamFormat1),
    bundleName: 'ORIGINAL',
    _links: {
      self: {
        href:
          'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713',
      },
      content: {
        href:
          'https://dspace7.4science.it/dspace-spring-rest/api/core/bitstreams/cf9b0c8e-a1eb-4b65-afd0-567366448713/content',
      },
    },
    id: 'cf9b0c8e-a1eb-4b65-afd0-567366448713',
    uuid: 'cf9b0c8e-a1eb-4b65-afd0-567366448713',
    type: 'bitstream',
    metadata: {
      'dc.title': [
        {
          language: null,
          value: 'test_word.docx',
        },
      ],
    },
  });

  const bitstreamDataService = jasmine.createSpyObj('bitstreamDataService', {
    findAllByItemAndBundleName: createSuccessfulRemoteDataObject$(
      createPaginatedList([mockBitstream])
    ),
  });

  const mockMediaViewerItem: MediaViewerItem = Object.assign(
    new MediaViewerItem(),
    { bitstream: mockBitstream, format: 'image', thumbnail: null }
  );

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock,
          },
        }),
        BrowserAnimationsModule,
      ],
      declarations: [
        MediaViewerComponent,
        VarDirective,
        FileSizePipe,
        MetadataFieldWrapperComponent,
      ],
      providers: [
        { provide: BitstreamDataService, useValue: bitstreamDataService },
      ],

      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MediaViewerComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when the bitstreams are loading', () => {
    beforeEach(() => {
      comp.mediaList$.next([mockMediaViewerItem]);
      comp.videoOptions = true;
      comp.isLoading = true;
      fixture.detectChanges();
    });

    it('should call the createMediaViewerItem', () => {
      const mediaItem = comp.createMediaViewerItem(
        mockBitstream,
        MockBitstreamFormat1,
        undefined
      );
      expect(mediaItem).toBeTruthy();
      expect(mediaItem.thumbnail).toBe(null);
    });

    it('should display a loading component', () => {
      const loading = fixture.debugElement.query(By.css('ds-themed-loading'));
      expect(loading.nativeElement).toBeDefined();
    });
  });

  describe('when the bitstreams loading is failed', () => {
    beforeEach(() => {
      comp.mediaList$.next([]);
      comp.videoOptions = true;
      comp.isLoading = false;
      fixture.detectChanges();
    });

    it('should call the createMediaViewerItem', () => {
      const mediaItem = comp.createMediaViewerItem(
        mockBitstream,
        MockBitstreamFormat1,
        undefined
      );
      expect(mediaItem).toBeTruthy();
      expect(mediaItem.thumbnail).toBe(null);
    });

    it('should display a default, thumbnail', () => {
      const defaultThumbnail = fixture.debugElement.query(
        By.css('ds-media-viewer-image')
      );
      expect(defaultThumbnail.nativeElement).toBeDefined();
    });
  });
});
