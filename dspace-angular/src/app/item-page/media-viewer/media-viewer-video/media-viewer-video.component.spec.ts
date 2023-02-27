import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { of as observableOf } from 'rxjs';
import { Bitstream } from '../../../core/shared/bitstream.model';
import { MediaViewerItem } from '../../../core/shared/media-viewer-item.model';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { FileSizePipe } from '../../../shared/utils/file-size-pipe';
import { VarDirective } from '../../../shared/utils/var.directive';
import { MetadataFieldWrapperComponent } from '../../../shared/metadata-field-wrapper/metadata-field-wrapper.component';
import { MockBitstreamFormat1 } from '../../../shared/mocks/item.mock';
import { MediaViewerVideoComponent } from './media-viewer-video.component';
import { By } from '@angular/platform-browser';

describe('MediaViewerVideoComponent', () => {
  let component: MediaViewerVideoComponent;
  let fixture: ComponentFixture<MediaViewerVideoComponent>;

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
        MediaViewerVideoComponent,
        VarDirective,
        FileSizePipe,
        MetadataFieldWrapperComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  }));

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

  const mockMediaViewerItems: MediaViewerItem[] = Object.assign(
    new Array<MediaViewerItem>(),
    [
      { bitstream: mockBitstream, format: 'video', thumbnail: null },
      { bitstream: mockBitstream, format: 'video', thumbnail: null },
    ]
  );
  const mockMediaViewerItem: MediaViewerItem[] = Object.assign(
    new Array<MediaViewerItem>(),
    [{ bitstream: mockBitstream, format: 'video', thumbnail: null }]
  );

  beforeEach(() => {
    fixture = TestBed.createComponent(MediaViewerVideoComponent);
    component = fixture.componentInstance;
    component.medias = mockMediaViewerItem;
    component.filteredMedias = mockMediaViewerItem;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('should show controller buttons when the having mode then one video', () => {
    beforeEach(() => {
      component.medias = mockMediaViewerItems;
      component.filteredMedias = mockMediaViewerItems;
      fixture.detectChanges();
    });

    it('should show buttons', () => {
      const controllerButtons = fixture.debugElement.query(By.css('.buttons'));
      expect(controllerButtons).toBeTruthy();
    });

    describe('when the "Next" button is clicked', () => {
      beforeEach(() => {
        component.currentIndex = 0;
        fixture.detectChanges();
      });

      it('should increase the index', () => {
        const viewMore = fixture.debugElement.query(By.css('.next'));
        viewMore.triggerEventHandler('click', null);
        expect(component.currentIndex).toBe(1);
      });
    });

    describe('when the "Previous" button is clicked', () => {
      beforeEach(() => {
        component.currentIndex = 1;
        fixture.detectChanges();
      });

      it('should decrease the index', () => {
        const viewMore = fixture.debugElement.query(By.css('.previous'));
        viewMore.triggerEventHandler('click', null);
        expect(component.currentIndex).toBe(0);
      });
    });

    describe('when the "Playlist element" button is clicked', () => {
      beforeEach(() => {
        component.isCollapsed = true;
        fixture.detectChanges();
      });

      it('should set the the index with the selected one', () => {
        const viewMore = fixture.debugElement.query(By.css('.list-element'));
        viewMore.triggerEventHandler('click', null);
        expect(component.currentIndex).toBe(0);
      });
    });
  });
});
