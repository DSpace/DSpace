import { ActivatedRoute, Router } from '@angular/router';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

import { of } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ProfileClaimItemModalComponent } from './profile-claim-item-modal.component';
import { ProfileClaimService } from '../profile-claim/profile-claim.service';
import { Item } from '../../core/shared/item.model';
import { ItemSearchResult } from '../../shared/object-collection/shared/item-search-result.model';
import { SearchObjects } from '../../shared/search/models/search-objects.model';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { getItemPageRoute } from '../../item-page/item-page-routing-paths';
import { RouterStub } from '../../shared/testing/router.stub';

describe('ProfileClaimItemModalComponent', () => {
  let component: ProfileClaimItemModalComponent;
  let fixture: ComponentFixture<ProfileClaimItemModalComponent>;

  const item1: Item = Object.assign(new Item(), {
    uuid: 'e1c51c69-896d-42dc-8221-1d5f2ad5516e',
    metadata: {
      'person.email': [
        {
          value: 'fake@email.com'
        }
      ],
      'person.familyName': [
        {
          value: 'Doe'
        }
      ],
      'person.givenName': [
        {
          value: 'John'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });
  const item2: Item = Object.assign(new Item(), {
    uuid: 'c8279647-1acc-41ae-b036-951d5f65649b',
    metadata: {
      'person.email': [
        {
          value: 'fake2@email.com'
        }
      ],
      'dc.title': [
        {
          value: 'John, Doe'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });
  const item3: Item = Object.assign(new Item(), {
    uuid: 'c8279647-1acc-41ae-b036-951d5f65649b',
    metadata: {
      'person.email': [
        {
          value: 'fake3@email.com'
        }
      ],
      'dc.title': [
        {
          value: 'John, Doe'
        }
      ]
    },
    _links: {
      self: {
        href: 'item-href'
      }
    }
  });

  const searchResult1 = Object.assign(new ItemSearchResult(), { indexableObject: item1 });
  const searchResult2 = Object.assign(new ItemSearchResult(), { indexableObject: item2 });
  const searchResult3 = Object.assign(new ItemSearchResult(), { indexableObject: item3 });

  const searchResult = Object.assign(new SearchObjects(), {
    page: [searchResult1, searchResult2, searchResult3]
  });
  const emptySearchResult = Object.assign(new SearchObjects(), {
    page: []
  });
  const searchResultRD = createSuccessfulRemoteDataObject(searchResult);
  const emptySearchResultRD = createSuccessfulRemoteDataObject(emptySearchResult);

  const profileClaimService = jasmine.createSpyObj('profileClaimService', {
    searchForSuggestions: jasmine.createSpy('searchForSuggestions')
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [ProfileClaimItemModalComponent],
      providers: [
        { provide: NgbActiveModal, useValue: {} },
        { provide: ActivatedRoute, useValue: {} },
        { provide: Router, useValue: new RouterStub() },
        { provide: ProfileClaimService, useValue: profileClaimService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfileClaimItemModalComponent);
    component = fixture.componentInstance;
  });

  describe('when there are suggestions', () => {

    beforeEach(() => {
      profileClaimService.searchForSuggestions.and.returnValue(of(searchResultRD));
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should init the list of suggestions', () => {
      const entries = fixture.debugElement.queryAll(By.css('.list-group-item'));
      expect(component.listEntries$.value).toEqual(searchResultRD);
      expect(entries.length).toBe(3);
    });

    it('should close modal and call navigate method', () => {
      spyOn(component, 'close');
      spyOn(component, 'navigate');
      component.selectItem(item1);

      expect(component.close).toHaveBeenCalled();
      expect(component.navigate).toHaveBeenCalledWith(item1);
    });

    it('should call router navigate method', () => {
      const route = [getItemPageRoute(item1)];
      component.navigate(item1);

      expect((component as any).router.navigate).toHaveBeenCalledWith(route);
    });

    it('should toggle checkbox', () => {
      component.toggleCheckbox();

      expect((component as any).checked).toBe(true);
    });

    it('should emit create event', () => {
      spyOn(component, 'close');
      spyOn(component.create, 'emit');
      component.createFromScratch();

      expect(component.create.emit).toHaveBeenCalled();
      expect(component.close).toHaveBeenCalled();
    });
  });

  describe('when there are not suggestions', () => {

    beforeEach(() => {
      profileClaimService.searchForSuggestions.and.returnValue(of(emptySearchResultRD));
      fixture.detectChanges();
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should init the list of suggestions', () => {
      const entries = fixture.debugElement.queryAll(By.css('.list-group-item'));
      expect(component.listEntries$.value).toEqual(emptySearchResultRD);
      expect(entries.length).toBe(0);
    });

    it('should close modal and call navigate method', () => {
      spyOn(component, 'close');
      spyOn(component, 'navigate');
      component.selectItem(item1);

      expect(component.close).toHaveBeenCalled();
      expect(component.navigate).toHaveBeenCalledWith(item1);
    });

    it('should call router navigate method', () => {
      const route = [getItemPageRoute(item1)];
      component.navigate(item1);

      expect((component as any).router.navigate).toHaveBeenCalledWith(route);
    });

    it('should toggle checkbox', () => {
      component.toggleCheckbox();

      expect((component as any).checked).toBe(true);
    });

    it('should emit create event', () => {
      spyOn(component, 'close');
      spyOn(component.create, 'emit');
      component.createFromScratch();

      expect(component.create.emit).toHaveBeenCalled();
      expect(component.close).toHaveBeenCalled();
    });
  });
});
