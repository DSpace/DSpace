import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NgxGalleryOptions } from '@kolkov/ngx-gallery';
import { Bitstream } from '../../../core/shared/bitstream.model';
import { MediaViewerItem } from '../../../core/shared/media-viewer-item.model';
import { MockBitstreamFormat1 } from '../../../shared/mocks/item.mock';

import { MediaViewerImageComponent } from './media-viewer-image.component';

import { of as observableOf } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

describe('MediaViewerImageComponent', () => {
  let component: MediaViewerImageComponent;
  let fixture: ComponentFixture<MediaViewerImageComponent>;

  const authService = jasmine.createSpyObj('authService', {
    isAuthenticated: observableOf(false)
  });

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
      { bitstream: mockBitstream, format: 'image', thumbnail: null },
      { bitstream: mockBitstream, format: 'image', thumbnail: null },
    ]
  );

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports:[],
      declarations: [MediaViewerImageComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MediaViewerImageComponent);
    component = fixture.componentInstance;
    component.galleryOptions = [new NgxGalleryOptions({})];
    component.galleryImages = component.convertToGalleryImage(
      mockMediaViewerItems
    );
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should contain a gallery options', () => {
    expect(component.galleryOptions.length).toBeGreaterThan(0);
  });

  it('should contain an image array', () => {
    expect(component.galleryImages.length).toBeGreaterThan(0);
  });
});
