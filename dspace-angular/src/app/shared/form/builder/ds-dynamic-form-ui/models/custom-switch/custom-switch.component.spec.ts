import { DynamicFormsCoreModule, DynamicFormService } from '@ng-dynamic-forms/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DynamicCustomSwitchModel } from './custom-switch.model';
import { CustomSwitchComponent } from './custom-switch.component';

describe('CustomSwitchComponent', () => {

  const testModel = new DynamicCustomSwitchModel({ id: 'switch' });
  const formModel = [testModel];
  let formGroup: FormGroup;
  let fixture: ComponentFixture<CustomSwitchComponent>;
  let component: CustomSwitchComponent;
  let debugElement: DebugElement;
  let testElement: DebugElement;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        NoopAnimationsModule,
        DynamicFormsCoreModule.forRoot()
      ],
      declarations: [CustomSwitchComponent]

    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(CustomSwitchComponent);

      component = fixture.componentInstance;
      debugElement = fixture.debugElement;
    });
  }));

  beforeEach(inject([DynamicFormService], (service: DynamicFormService) => {
    formGroup = service.createFormGroup(formModel);

    component.group = formGroup;
    component.model = testModel;

    fixture.detectChanges();

    testElement = debugElement.query(By.css(`input[id='${testModel.id}']`));
  }));

  it('should initialize correctly', () => {
    expect(component.bindId).toBe(true);
    expect(component.group instanceof FormGroup).toBe(true);
    expect(component.model instanceof DynamicCustomSwitchModel).toBe(true);

    expect(component.blur).toBeDefined();
    expect(component.change).toBeDefined();
    expect(component.focus).toBeDefined();

    expect(component.onBlur).toBeDefined();
    expect(component.onChange).toBeDefined();
    expect(component.onFocus).toBeDefined();

    expect(component.hasFocus).toBe(false);
    expect(component.isValid).toBe(true);
    expect(component.isInvalid).toBe(false);
  });

  it('should have an input element', () => {
    expect(testElement instanceof DebugElement).toBe(true);
  });

  it('should have an input element of type checkbox', () => {
    expect(testElement.nativeElement.getAttribute('type')).toEqual('checkbox');
  });

  it('should emit blur event', () => {
    spyOn(component.blur, 'emit');

    component.onBlur(null);

    expect(component.blur.emit).toHaveBeenCalled();
  });

  it('should emit change event', () => {
    spyOn(component.change, 'emit');

    component.onChange(null);

    expect(component.change.emit).toHaveBeenCalled();
  });

  it('should emit focus event', () => {
    spyOn(component.focus, 'emit');

    component.onFocus(null);

    expect(component.focus.emit).toHaveBeenCalled();
  });
});
