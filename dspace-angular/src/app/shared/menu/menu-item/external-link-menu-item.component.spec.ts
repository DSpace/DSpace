import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ExternalLinkMenuItemComponent } from './external-link-menu-item.component';

describe('ExternalLinkMenuItemComponent', () => {
  let component: ExternalLinkMenuItemComponent;
  let fixture: ComponentFixture<ExternalLinkMenuItemComponent>;
  let debugElement: DebugElement;
  let text;
  let link;

  function init() {
    text = 'HELLO';
    link = 'https://google.com/';
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [ExternalLinkMenuItemComponent],
      providers: [
        { provide: 'itemModelProvider', useValue: { text: text, href: link } },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExternalLinkMenuItemComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should contain the correct text', () => {
    const textContent = debugElement.query(By.css('a')).nativeElement.textContent;
    expect(textContent).toEqual(text);
  });

  it('should have the right href attribute', () => {
    const links = fixture.debugElement.queryAll(By.css('a'));
    expect(links.length).toBe(1);
    expect(links[0].nativeElement.href).toBe(link);
  });
});
