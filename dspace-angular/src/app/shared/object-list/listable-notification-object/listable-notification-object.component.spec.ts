import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListableNotificationObjectComponent } from './listable-notification-object.component';
import { NotificationType } from '../../notifications/models/notification-type';
import { ListableNotificationObject } from './listable-notification-object.model';
import { By } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';

describe('ListableNotificationObjectComponent', () => {
  let component: ListableNotificationObjectComponent;
  let fixture: ComponentFixture<ListableNotificationObjectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot(),
      ],
      declarations: [
        ListableNotificationObjectComponent,
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ListableNotificationObjectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('ui', () => {
    it('should display the given error message', () => {
      component.object = new ListableNotificationObject(NotificationType.Error, 'test error message');
      fixture.detectChanges();

      const listableNotificationObject: Element = fixture.debugElement.query(By.css('.alert')).nativeElement;
      expect(listableNotificationObject.className).toContain(NotificationType.Error);
      expect(listableNotificationObject.innerHTML).toBe('test error message');
    });
  });

  afterEach(() => {
    fixture.debugElement.nativeElement.remove();
  });
});
