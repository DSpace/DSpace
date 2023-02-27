import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { OnClickMenuItemComponent } from './onclick-menu-item.component';
import { OnClickMenuItemModel } from './models/onclick.model';

describe('OnClickMenuItemComponent', () => {
  let component: OnClickMenuItemComponent;
  let fixture: ComponentFixture<OnClickMenuItemComponent>;
  let debugElement: DebugElement;
  const text = 'HELLO';
  const func = () => {
    /* comment */
  };
  const item = Object.assign(new OnClickMenuItemModel(), { text, function: func });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [OnClickMenuItemComponent],
      providers: [
        { provide: 'itemModelProvider', useValue: item },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    spyOn(item, 'function');
    fixture = TestBed.createComponent(OnClickMenuItemComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should contain the correct text', () => {
    expect(component).toBeTruthy();
  });

  it('should contain the text element', () => {
    const textContent = debugElement.query(By.css('a')).nativeElement.textContent;
    expect(textContent).toEqual(text);
  });

  it('should call the function on the item when clicked', () => {
    debugElement.query(By.css('a.nav-link')).triggerEventHandler('click', new Event(('click')));
    expect(item.function).toHaveBeenCalled();
  });
});
