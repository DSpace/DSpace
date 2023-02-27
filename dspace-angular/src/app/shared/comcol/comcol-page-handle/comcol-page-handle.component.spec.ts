import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { ComcolPageHandleComponent } from './comcol-page-handle.component';

const handle = 'http://localhost:4000/handle/123456789/2';

describe('ComcolPageHandleComponent', () => {
  let component: ComcolPageHandleComponent;
  let fixture: ComponentFixture<ComcolPageHandleComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [ComcolPageHandleComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ComcolPageHandleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should be empty if no content is passed', () => {
    component.content = undefined;
    fixture.detectChanges();
    const div = fixture.debugElement.query(By.css('div'));
    expect(div).toBeNull();
  });

  it('should create a link pointing the handle when present', () => {

    component.content = handle;
    fixture.detectChanges();

    const link = fixture.debugElement.query(By.css('a'));
    expect(link.nativeElement.getAttribute('href')).toBe(handle);
    expect(link.nativeElement.innerHTML).toBe(handle);

  });

});
