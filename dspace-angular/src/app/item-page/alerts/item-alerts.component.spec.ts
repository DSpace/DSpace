import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ItemAlertsComponent } from './item-alerts.component';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Item } from '../../core/shared/item.model';
import { By } from '@angular/platform-browser';

describe('ItemAlertsComponent', () => {
  let component: ItemAlertsComponent;
  let fixture: ComponentFixture<ItemAlertsComponent>;
  let item: Item;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ItemAlertsComponent],
      imports: [TranslateModule.forRoot()],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemAlertsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when the item is discoverable', () => {
    beforeEach(() => {
      item = Object.assign(new Item(), {
        isDiscoverable: true
      });
      component.item = item;
      fixture.detectChanges();
    });

    it('should not display the private alert', () => {
      const privateWarning = fixture.debugElement.query(By.css('.private-warning'));
      expect(privateWarning).toBeNull();
    });
  });

  describe('when the item is not discoverable', () => {
    beforeEach(() => {
      item = Object.assign(new Item(), {
        isDiscoverable: false
      });
      component.item = item;
      fixture.detectChanges();
    });

    it('should display the private alert', () => {
      const privateWarning = fixture.debugElement.query(By.css('.private-warning'));
      expect(privateWarning).not.toBeNull();
    });
  });

  describe('when the item is withdrawn', () => {
    beforeEach(() => {
      item = Object.assign(new Item(), {
        isWithdrawn: true
      });
      component.item = item;
      fixture.detectChanges();
    });

    it('should display the withdrawn alert', () => {
      const privateWarning = fixture.debugElement.query(By.css('.withdrawn-warning'));
      expect(privateWarning).not.toBeNull();
    });
  });

  describe('when the item is not withdrawn', () => {
    beforeEach(() => {
      item = Object.assign(new Item(), {
        isWithdrawn: false
      });
      component.item = item;
      fixture.detectChanges();
    });

    it('should not display the withdrawn alert', () => {
      const privateWarning = fixture.debugElement.query(By.css('.withdrawn-warning'));
      expect(privateWarning).toBeNull();
    });
  });
});
