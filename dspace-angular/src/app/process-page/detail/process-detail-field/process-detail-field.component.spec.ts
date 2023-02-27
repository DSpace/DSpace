import { ProcessDetailFieldComponent } from './process-detail-field.component';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { VarDirective } from '../../../shared/utils/var.directive';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('ProcessDetailFieldComponent', () => {
  let component: ProcessDetailFieldComponent;
  let fixture: ComponentFixture<ProcessDetailFieldComponent>;

  let title;

  beforeEach(waitForAsync(() => {
    title = 'fake.title.message';

    TestBed.configureTestingModule({
      declarations: [ProcessDetailFieldComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProcessDetailFieldComponent);
    component = fixture.componentInstance;
    component.title = title;
    fixture.detectChanges();
  });

  it('should display the given title', () => {
    const header = fixture.debugElement.query(By.css('h4')).nativeElement;
    expect(header.textContent).toContain(title);
  });
});
