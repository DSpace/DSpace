import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { IdleModalComponent } from './idle-modal.component';
import { AuthService } from '../../core/auth/auth.service';
import { By } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { LogOutAction } from '../../core/auth/auth.actions';

describe('IdleModalComponent', () => {
  let component: IdleModalComponent;
  let fixture: ComponentFixture<IdleModalComponent>;
  let debugElement: DebugElement;

  let modalStub;
  let authServiceStub;
  let storeStub;

  beforeEach(waitForAsync(() => {
    modalStub = jasmine.createSpyObj('modalStub', ['close']);
    authServiceStub = jasmine.createSpyObj('authService', ['setIdle']);
    storeStub = jasmine.createSpyObj('store', ['dispatch']);
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [IdleModalComponent],
      providers: [
        { provide: NgbActiveModal, useValue: modalStub },
        { provide: AuthService, useValue: authServiceStub },
        { provide: Store, useValue: storeStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(IdleModalComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('extendSessionPressed', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component.response, 'emit');
      component.extendSessionPressed();
    }));
    it('should set idle to false', () => {
      expect(authServiceStub.setIdle).toHaveBeenCalledWith(false);
    });
    it('should close the modal', () => {
      expect(modalStub.close).toHaveBeenCalled();
    });
    it('response \'closed\' should emit true', () => {
      expect(component.response.emit).toHaveBeenCalledWith(true);
    });
  });

  describe('logOutPressed', () => {
    beforeEach(() => {
      component.logOutPressed();
    });
    it('should close the modal', () => {
      expect(modalStub.close).toHaveBeenCalled();
    });
    it('should send logout action', () => {
      expect(storeStub.dispatch).toHaveBeenCalledWith(new LogOutAction());
    });
  });

  describe('closePressed', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component.response, 'emit');
      component.closePressed();
    }));
    it('should set idle to false', () => {
      expect(authServiceStub.setIdle).toHaveBeenCalledWith(false);
    });
    it('should close the modal', () => {
      expect(modalStub.close).toHaveBeenCalled();
    });
    it('response \'closed\' should emit true', () => {
      expect(component.response.emit).toHaveBeenCalledWith(true);
    });
  });

  describe('when the click method emits on extend session button', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component, 'extendSessionPressed');
      debugElement.query(By.css('button.confirm')).triggerEventHandler('click', {
        preventDefault: () => {/**/
        }
      });
      tick();
      fixture.detectChanges();
    }));
    it('should call the extendSessionPressed method on the component', () => {
      expect(component.extendSessionPressed).toHaveBeenCalled();
    });
  });

  describe('when the click method emits on log out button', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component, 'logOutPressed');
      debugElement.query(By.css('button.cancel')).triggerEventHandler('click', {
        preventDefault: () => {/**/
        }
      });
      tick();
      fixture.detectChanges();
    }));
    it('should call the logOutPressed method on the component', () => {
      expect(component.logOutPressed).toHaveBeenCalled();
    });
  });

  describe('when the click method emits on close button', () => {
    beforeEach(fakeAsync(() => {
      spyOn(component, 'closePressed');
      debugElement.query(By.css('.close')).triggerEventHandler('click', {
        preventDefault: () => {/**/
        }
      });
      tick();
      fixture.detectChanges();
    }));
    it('should call the closePressed method on the component', () => {
      expect(component.closePressed).toHaveBeenCalled();
    });
  });
});
