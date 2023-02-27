import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { NameVariantModalComponent } from './name-variant-modal.component';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';

describe('NameVariantModalComponent', () => {
  let component: NameVariantModalComponent;
  let fixture: ComponentFixture<NameVariantModalComponent>;
  let debugElement;
  let modal;

  function init() {
    modal = jasmine.createSpyObj('modal', ['close', 'dismiss']);
  }
  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [NameVariantModalComponent],
      imports: [NgbModule, TranslateModule.forRoot()],
      providers: [{ provide: NgbActiveModal, useValue: modal }]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NameVariantModalComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();

  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('when close button is clicked, dismiss should be called on the modal', () => {
    debugElement.query(By.css('button.close')).triggerEventHandler('click', {});
    expect(modal.dismiss).toHaveBeenCalled();
  });

  it('when confirm button is clicked, close should be called on the modal', () => {
    debugElement.query(By.css('button.confirm-button')).triggerEventHandler('click', {});
    expect(modal.close).toHaveBeenCalled();
  });

  it('when decline button is clicked, dismiss should be called on the modal', () => {
    debugElement.query(By.css('button.decline-button')).triggerEventHandler('click', {});
    expect(modal.dismiss).toHaveBeenCalled();
  });
});
