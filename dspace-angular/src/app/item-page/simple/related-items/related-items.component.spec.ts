import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ChangeDetectionStrategy, NO_ERRORS_SCHEMA } from '@angular/core';
import { RelatedItemsComponent } from './related-items-component';
import { Item } from '../../../core/shared/item.model';
import { By } from '@angular/platform-browser';
import { createRelationshipsObservable } from '../item-types/shared/item.component.spec';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { RelationshipDataService } from '../../../core/data/relationship-data.service';
import { TranslateModule } from '@ngx-translate/core';
import { VarDirective } from '../../../shared/utils/var.directive';
import { of as observableOf } from 'rxjs';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { APP_CONFIG } from '../../../../config/app-config.interface';

const parentItem: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
  metadata: [],
  relationships: createRelationshipsObservable()
});
const mockItem1: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
  metadata: [],
  relationships: createRelationshipsObservable()
});
const mockItem2: Item = Object.assign(new Item(), {
  bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
  metadata: [],
  relationships: createRelationshipsObservable()
});
const mockItems = [mockItem1, mockItem2];
const relationType = 'isItemOfItem';
let relationshipService: RelationshipDataService;

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

describe('RelatedItemsComponent', () => {
  let comp: RelatedItemsComponent;
  let fixture: ComponentFixture<RelatedItemsComponent>;

  beforeEach(waitForAsync(() => {
    relationshipService = jasmine.createSpyObj('relationshipService',
      {
        getRelatedItemsByLabel: createSuccessfulRemoteDataObject$(createPaginatedList(mockItems)),
      }
    );

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [RelatedItemsComponent, VarDirective],
      providers: [
        { provide: RelationshipDataService, useValue: relationshipService },
        { provide: APP_CONFIG, useValue: environmentUseThumbs }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(RelatedItemsComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(RelatedItemsComponent);
    comp = fixture.componentInstance;
    comp.parentItem = parentItem;
    comp.relationType = relationType;
    fixture.detectChanges();
  }));

  it(`should load ${mockItems.length} item-type-switcher components`, () => {
    const fields = fixture.debugElement.queryAll(By.css('ds-listable-object-component-loader'));
    expect(fields.length).toBe(mockItems.length);
  });

  it('should contain one page of items', () => {
    expect(comp.objects.length).toEqual(1);
  });

  describe('when increase is called', () => {
    beforeEach(() => {
      comp.increase();
    });

    it('should add a new page to the list', () => {
      expect(comp.objects.length).toEqual(2);
    });

    it('should call relationship-service\'s getRelatedItemsByLabel with the correct arguments (second page)', () => {
      expect(relationshipService.getRelatedItemsByLabel).toHaveBeenCalledWith(parentItem, relationType, Object.assign(comp.options, {
        elementsPerPage: comp.incrementBy,
        currentPage: 2,
        fetchThumbnail: true
      }));
    });

  });

  describe('when decrease is called', () => {
    beforeEach(() => {
      // Add a second page
      comp.objects.push(observableOf(undefined));
      comp.decrease();
    });

    it('should decrease the list of pages', () => {
      expect(comp.objects.length).toEqual(1);
    });
  });

});
describe('RelatedItemsComponent', () => {
  let comp: RelatedItemsComponent;
  let fixture: ComponentFixture<RelatedItemsComponent>;

  beforeEach(waitForAsync(() => {
    relationshipService = jasmine.createSpyObj('relationshipService',
      {
        getRelatedItemsByLabel: createSuccessfulRemoteDataObject$(createPaginatedList(mockItems)),
      }
    );

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [RelatedItemsComponent, VarDirective],
      providers: [
        {provide: RelationshipDataService, useValue: relationshipService},
        {provide: APP_CONFIG, useValue: enviromentNoThumbs}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(RelatedItemsComponent, {
      set: {changeDetection: ChangeDetectionStrategy.Default}
    }).compileComponents();
  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(RelatedItemsComponent);
    comp = fixture.componentInstance;
    comp.parentItem = parentItem;
    comp.relationType = relationType;
    fixture.detectChanges();
  }));
  it('should call relationship-service\'s getRelatedItemsByLabel with the correct arguments (second page)', () => {
    expect(relationshipService.getRelatedItemsByLabel).toHaveBeenCalledWith(parentItem, relationType, Object.assign(comp.options, {
      elementsPerPage: comp.incrementBy,
      currentPage: 2,
      fetchThumbnail: false
    }));
  });
});
