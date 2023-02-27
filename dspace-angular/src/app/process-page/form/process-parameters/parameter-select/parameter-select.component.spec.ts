import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { ParameterSelectComponent } from './parameter-select.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ScriptParameter } from '../../../scripts/script-parameter.model';
import { ScriptParameterType } from '../../../scripts/script-parameter-type.model';
import { By } from '@angular/platform-browser';

describe('ParameterSelectComponent', () => {
  let component: ParameterSelectComponent;
  let fixture: ComponentFixture<ParameterSelectComponent>;
  let scriptParams: ScriptParameter[];

  function init() {
    scriptParams = [
      Object.assign(
        new ScriptParameter(),
        {
          name: '-a',
          type: ScriptParameterType.BOOLEAN
        }
      ),
      Object.assign(
        new ScriptParameter(),
        {
          name: '-f',
          type: ScriptParameterType.FILE
        }
      ),
    ];
  }
  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ParameterSelectComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParameterSelectComponent);
    component = fixture.componentInstance;

    component.parameters = scriptParams;
    component.removable = false;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the remove button when removable', () => {
    component.removable = true;
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button.remove-button'));
    expect(button).not.toBeNull();
  });

  it('should hide the remove button when not removable', () => {
    component.removable = false;
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button.remove-button'));
    expect(button).toBeNull();
  });
});
