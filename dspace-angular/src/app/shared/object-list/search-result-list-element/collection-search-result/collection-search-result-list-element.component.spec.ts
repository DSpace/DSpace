import { CollectionSearchResultListElementComponent } from './collection-search-result-list-element.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { of as observableOf } from 'rxjs';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { TruncatePipe } from '../../../utils/truncate.pipe';
import { Collection } from '../../../../core/shared/collection.model';
import { TruncatableService } from '../../../truncatable/truncatable.service';
import { CollectionSearchResult } from '../../../object-collection/shared/collection-search-result.model';
import { DSONameService } from '../../../../core/breadcrumbs/dso-name.service';
import { DSONameServiceMock } from '../../../mocks/dso-name.service.mock';
import { APP_CONFIG } from '../../../../../config/app-config.interface';

let collectionSearchResultListElementComponent: CollectionSearchResultListElementComponent;
let fixture: ComponentFixture<CollectionSearchResultListElementComponent>;

const truncatableServiceStub: any = {
  isCollapsed: (id: number) => observableOf(true),
};

const mockCollectionWithAbstract: CollectionSearchResult = new CollectionSearchResult();
mockCollectionWithAbstract.hitHighlights = {};
mockCollectionWithAbstract.indexableObject = Object.assign(new Collection(), {
  metadata: {
    'dc.description.abstract': [
      {
        language: 'en_US',
        value: 'Short description'
      }
    ]
  }
});

const mockCollectionWithoutAbstract: CollectionSearchResult = new CollectionSearchResult();
mockCollectionWithoutAbstract.hitHighlights = {};
mockCollectionWithoutAbstract.indexableObject = Object.assign(new Collection(), {
  metadata: {
    'dc.title': [
      {
        language: 'en_US',
        value: 'Test title'
      }
    ]
  }
});

const environmentUseThumbs = {
  browseBy: {
    showThumbnails: true
  }
};

describe('CollectionSearchResultListElementComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [CollectionSearchResultListElementComponent, TruncatePipe],
      providers: [
        { provide: TruncatableService, useValue: truncatableServiceStub },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: environmentUseThumbs }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(CollectionSearchResultListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(CollectionSearchResultListElementComponent);
    collectionSearchResultListElementComponent = fixture.componentInstance;
    collectionSearchResultListElementComponent.object = mockCollectionWithAbstract;
    fixture.detectChanges();
  }));

  describe('When the collection has an abstract', () => {
    beforeEach(() => {
      collectionSearchResultListElementComponent.dso = mockCollectionWithAbstract.indexableObject;
      fixture.detectChanges();
    });

    it('should show the description paragraph', () => {
      const collectionAbstractField = fixture.debugElement.query(By.css('div.abstract-text'));
      expect(collectionAbstractField).not.toBeNull();
    });
  });

  describe('When the collection has no abstract', () => {
    beforeEach(() => {
      collectionSearchResultListElementComponent.dso = mockCollectionWithoutAbstract.indexableObject;
      fixture.detectChanges();
    });

    it('should not show the description paragraph', () => {
      const collectionAbstractField = fixture.debugElement.query(By.css('div.abstract-text'));
      expect(collectionAbstractField).toBeNull();
    });
  });

  describe('when environment is set to show thumbnail images', () => {
    it('should offset content', () => {
      const offset = fixture.debugElement.query(By.css('offset-md-2'));
    });
  });

});
