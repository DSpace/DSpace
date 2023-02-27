import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { of as observableOf } from 'rxjs';
import { ItemSearchResultListElementComponent } from './item-search-result-list-element.component';
import { Item } from '../../../../../../core/shared/item.model';
import { TruncatePipe } from '../../../../../utils/truncate.pipe';
import { TruncatableService } from '../../../../../truncatable/truncatable.service';
import { ItemSearchResult } from '../../../../../object-collection/shared/item-search-result.model';
import { DSONameService } from '../../../../../../core/breadcrumbs/dso-name.service';
import { DSONameServiceMock, UNDEFINED_NAME } from '../../../../../mocks/dso-name.service.mock';
import { APP_CONFIG } from '../../../../../../../config/app-config.interface';

let publicationListElementComponent: ItemSearchResultListElementComponent;
let fixture: ComponentFixture<ItemSearchResultListElementComponent>;
const dcTitle = 'This is just another <em>title</em>';
const mockItemWithMetadata: ItemSearchResult = Object.assign(new ItemSearchResult(), {
  hitHighlights: {
    'dc.title': [{
      value: dcTitle
    }],
  },
  indexableObject:
    Object.assign(new Item(), {
      bundles: observableOf({}),
      metadata: {
        'dc.title': [
          {
            language: 'en_US',
            value: dcTitle
          }
        ],
        'dc.contributor.author': [
          {
            language: 'en_US',
            value: 'Smith, Donald'
          }
        ],
        'dc.publisher': [
          {
            language: 'en_US',
            value: 'a publisher'
          }
        ],
        'dc.date.issued': [
          {
            language: 'en_US',
            value: '2015-06-26'
          }
        ],
        'dc.description.abstract': [
          {
            language: 'en_US',
            value: 'This is the abstract'
          }
        ]
      }
    })
});
const mockItemWithoutMetadata: ItemSearchResult = Object.assign(new ItemSearchResult(), {
  indexableObject:
    Object.assign(new Item(), {
      bundles: observableOf({}),
      metadata: {}
    })
});
const mockPerson: ItemSearchResult = Object.assign(new ItemSearchResult(), {
  hitHighlights: {
    'person.familyName': [{
      value: '<em>Michel</em>'
    }],
  },
  indexableObject:
    Object.assign(new Item(), {
      bundles: observableOf({}),
      entityType: 'Person',
      metadata: {
        'dc.title': [
          {
            language: 'en_US',
            value: 'This is just another title'
          }
        ],
        'dc.contributor.author': [
          {
            language: 'en_US',
            value: 'Smith, Donald'
          }
        ],
        'dc.publisher': [
          {
            language: 'en_US',
            value: 'a publisher'
          }
        ],
        'dc.date.issued': [
          {
            language: 'en_US',
            value: '2015-06-26'
          }
        ],
        'dc.description.abstract': [
          {
            language: 'en_US',
            value: 'This is the abstract'
          }
        ],
        'person.familyName': [
          {
            value: 'Michel'
          }
        ],
        'dspace.entity.type': [
          {
            value: 'Person'
          }
        ]
      }
    })
});
const mockOrgUnit: ItemSearchResult = Object.assign(new ItemSearchResult(), {
  hitHighlights: {
    'organization.legalName': [{
      value: '<em>Science</em>'
    }],
  },
  indexableObject:
    Object.assign(new Item(), {
      bundles: observableOf({}),
      entityType: 'OrgUnit',
      metadata: {
        'dc.title': [
          {
            language: 'en_US',
            value: 'This is just another title'
          }
        ],
        'dc.contributor.author': [
          {
            language: 'en_US',
            value: 'Smith, Donald'
          }
        ],
        'dc.publisher': [
          {
            language: 'en_US',
            value: 'a publisher'
          }
        ],
        'dc.date.issued': [
          {
            language: 'en_US',
            value: '2015-06-26'
          }
        ],
        'dc.description.abstract': [
          {
            language: 'en_US',
            value: 'This is the abstract'
          }
        ],
        'organization.legalName': [
          {
            value: 'Science'
          }
        ],
        'dspace.entity.type': [
          {
            value: 'OrgUnit'
          }
        ]
      }
    })
});
const environmentUseThumbs = {
  browseBy: {
    showThumbnails: true
  }
};

const enviromentNoThumbs = {
  browseBy: {
    showThumbnails: false
  }
};

describe('ItemSearchResultListElementComponent', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ItemSearchResultListElementComponent, TruncatePipe],
      providers: [
        { provide: TruncatableService, useValue: {} },
        { provide: DSONameService, useClass: DSONameServiceMock },
        { provide: APP_CONFIG, useValue: environmentUseThumbs }
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ItemSearchResultListElementComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ItemSearchResultListElementComponent);
    publicationListElementComponent = fixture.componentInstance;

  }));

  describe('with environment.browseBy.showThumbnails set to true', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });
    it('should set showThumbnails to true', () => {
      expect(publicationListElementComponent.showThumbnails).toBeTrue();
    });

    it('should add ds-thumbnail element', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('ds-thumbnail'));
      expect(thumbnailElement).toBeTruthy();
    });
  });

  describe('When the item has an author', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the author paragraph', () => {
      const itemAuthorField = fixture.debugElement.query(By.css('span.item-list-authors'));
      expect(itemAuthorField).not.toBeNull();
    });
  });

  describe('When the item has no author', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the author paragraph', () => {
      const itemAuthorField = fixture.debugElement.query(By.css('span.item-list-authors'));
      expect(itemAuthorField).toBeNull();
    });
  });

  describe('When the item has a publisher', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the publisher span', () => {
      const publisherField = fixture.debugElement.query(By.css('span.item-list-publisher'));
      expect(publisherField).not.toBeNull();
    });
  });

  describe('When the item has no publisher', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the publisher span', () => {
      const publisherField = fixture.debugElement.query(By.css('span.item-list-publisher'));
      expect(publisherField).toBeNull();
    });
  });

  describe('When the item has an issuedate', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the issuedate span', () => {
      const dateField = fixture.debugElement.query(By.css('span.item-list-date'));
      expect(dateField).not.toBeNull();
    });
  });

  describe('When the item has no issuedate', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the issuedate span', () => {
      const dateField = fixture.debugElement.query(By.css('span.item-list-date'));
      expect(dateField).toBeNull();
    });
  });

  describe('When the item has an abstract', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show the abstract span', () => {
      const abstractField = fixture.debugElement.query(By.css('div.item-list-abstract'));
      expect(abstractField).not.toBeNull();
    });
  });

  describe('When the item has no abstract', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should not show the abstract span', () => {
      const abstractField = fixture.debugElement.query(By.css('div.item-list-abstract'));
      expect(abstractField).toBeNull();
    });
  });

  describe('When the item has title', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should show highlighted title', () => {
      const titleField = fixture.debugElement.query(By.css('.item-list-title'));
      expect(titleField.nativeNode.innerHTML).toEqual(dcTitle);
    });
  });

  describe('When the item is Person and has title', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockPerson;
      fixture.detectChanges();
    });

    it('should show highlighted title', () => {
      const titleField = fixture.debugElement.query(By.css('.item-list-title'));
      expect(titleField.nativeNode.innerHTML).toEqual('<em>Michel</em>');
    });
  });

  describe('When the item is orgUnit and has title', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockOrgUnit;
      fixture.detectChanges();
    });

    it('should show highlighted title', () => {
      const titleField = fixture.debugElement.query(By.css('.item-list-title'));
      expect(titleField.nativeNode.innerHTML).toEqual('<em>Science</em>');
    });
  });

  describe('When the item has no title', () => {
    beforeEach(() => {
      publicationListElementComponent.object = mockItemWithoutMetadata;
      fixture.detectChanges();
    });

    it('should show the fallback untitled translation', () => {
      const titleField = fixture.debugElement.query(By.css('.item-list-title'));
      expect(titleField.nativeElement.textContent.trim()).toEqual(UNDEFINED_NAME);
    });
  });
});

describe('ItemSearchResultListElementComponent', () => {

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ItemSearchResultListElementComponent, TruncatePipe],
      providers: [
        {provide: TruncatableService, useValue: {}},
        {provide: DSONameService, useClass: DSONameServiceMock},
        { provide: APP_CONFIG, useValue: enviromentNoThumbs }
      ],

      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ItemSearchResultListElementComponent, {
      set: {changeDetection: ChangeDetectionStrategy.Default}
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(ItemSearchResultListElementComponent);
    publicationListElementComponent = fixture.componentInstance;
  }));

  describe('with environment.browseBy.showThumbnails set to false', () => {
    beforeEach(() => {

      publicationListElementComponent.object = mockItemWithMetadata;
      fixture.detectChanges();
    });

    it('should not add ds-thumbnail element', () => {
      const thumbnailElement = fixture.debugElement.query(By.css('ds-thumbnail'));
      expect(thumbnailElement).toBeFalsy();
    });
  });
});
