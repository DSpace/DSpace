import { EmailRequestCopyComponent } from './email-request-copy.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Location } from '@angular/common';
import { RequestCopyEmail } from './request-copy-email.model';

describe('EmailRequestCopyComponent', () => {
  let component: EmailRequestCopyComponent;
  let fixture: ComponentFixture<EmailRequestCopyComponent>;

  let location: Location;

  beforeEach(waitForAsync(() => {
    location = jasmine.createSpyObj('location', ['back']);

    TestBed.configureTestingModule({
      declarations: [EmailRequestCopyComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: Location, useValue: location },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EmailRequestCopyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('return should navigate to the previous page', () => {
    component.return();
    expect(location.back).toHaveBeenCalled();
  });

  it('submit should emit an email object', () => {
    spyOn(component.send, 'emit').and.stub();
    component.subject = 'test-subject';
    component.message = 'test-message';
    component.submit();
    expect(component.send.emit).toHaveBeenCalledWith(new RequestCopyEmail('test-subject', 'test-message'));
  });
});
