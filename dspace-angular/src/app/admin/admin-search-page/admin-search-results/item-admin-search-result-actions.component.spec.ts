import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { ItemAdminSearchResultActionsComponent } from './item-admin-search-result-actions.component';
import { Item } from '../../../core/shared/item.model';
import { URLCombiner } from '../../../core/url-combiner/url-combiner';
import { getItemEditRoute } from '../../../item-page/item-page-routing-paths';
import {
  ITEM_EDIT_DELETE_PATH,
  ITEM_EDIT_MOVE_PATH,
  ITEM_EDIT_PRIVATE_PATH,
  ITEM_EDIT_PUBLIC_PATH,
  ITEM_EDIT_REINSTATE_PATH,
  ITEM_EDIT_WITHDRAW_PATH
} from '../../../item-page/edit-item-page/edit-item-page.routing-paths';

describe('ItemAdminSearchResultActionsComponent', () => {
  let component: ItemAdminSearchResultActionsComponent;
  let fixture: ComponentFixture<ItemAdminSearchResultActionsComponent>;
  let id;
  let item;

  function init() {
    id = '780b2588-bda5-4112-a1cd-0b15000a5339';
    item = new Item();
    item.uuid = id;
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule.withRoutes([])
      ],
      declarations: [ItemAdminSearchResultActionsComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemAdminSearchResultActionsComponent);
    component = fixture.componentInstance;
    component.item = item;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render an edit button with the correct link', () => {
    const button = fixture.debugElement.query(By.css('a.edit-link'));
    const link = button.nativeElement.href;
    expect(link).toContain(getItemEditRoute(item));
  });

  it('should render a delete button with the correct link', () => {
    const button = fixture.debugElement.query(By.css('a.delete-link'));
    const link = button.nativeElement.href;
    expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_DELETE_PATH).toString());
  });

  it('should render a move button with the correct link', () => {
    const a = fixture.debugElement.query(By.css('a.move-link'));
    const link = a.nativeElement.href;
    expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_MOVE_PATH).toString());
  });

  describe('when the item is not withdrawn', () => {
    beforeEach(() => {
      component.item.isWithdrawn = false;
      fixture.detectChanges();
    });

    it('should render a withdraw button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.withdraw-link'));
      const link = a.nativeElement.href;
      expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_WITHDRAW_PATH).toString());
    });

    it('should not render a reinstate button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.reinstate-link'));
      expect(a).toBeNull();
    });
  });

  describe('when the item is withdrawn', () => {
    beforeEach(() => {
      component.item.isWithdrawn = true;
      fixture.detectChanges();
    });

    it('should not render a withdraw button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.withdraw-link'));
      expect(a).toBeNull();
    });

    it('should render a reinstate button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.reinstate-link'));
      const link = a.nativeElement.href;
      expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_REINSTATE_PATH).toString());
    });
  });

  describe('when the item is not private', () => {
    beforeEach(() => {
      component.item.isDiscoverable = true;
      fixture.detectChanges();
    });

    it('should render a make private button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.private-link'));
      const link = a.nativeElement.href;
      expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_PRIVATE_PATH).toString());
    });

    it('should not render a make public button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.public-link'));
      expect(a).toBeNull();
    });
  });

  describe('when the item is private', () => {
    beforeEach(() => {
      component.item.isDiscoverable = false;
      fixture.detectChanges();
    });

    it('should not render a make private button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.private-link'));
      expect(a).toBeNull();
    });

    it('should render a make private button with the correct link', () => {
      const a = fixture.debugElement.query(By.css('a.public-link'));
      const link = a.nativeElement.href;
      expect(link).toContain(new URLCombiner(getItemEditRoute(item), ITEM_EDIT_PUBLIC_PATH).toString());
    });
  });
});
