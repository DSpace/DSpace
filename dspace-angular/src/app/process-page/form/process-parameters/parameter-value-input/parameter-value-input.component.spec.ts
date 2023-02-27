import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { ParameterValueInputComponent } from './parameter-value-input.component';
import { ScriptParameter } from '../../../scripts/script-parameter.model';
import { ScriptParameterType } from '../../../scripts/script-parameter-type.model';
import { By } from '@angular/platform-browser';
import { BooleanValueInputComponent } from './boolean-value-input/boolean-value-input.component';
import { StringValueInputComponent } from './string-value-input/string-value-input.component';
import { FileValueInputComponent } from './file-value-input/file-value-input.component';
import { DateValueInputComponent } from './date-value-input/date-value-input.component';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { FileValueAccessorDirective } from '../../../../shared/utils/file-value-accessor.directive';
import { FileValidator } from '../../../../shared/utils/require-file.validator';
import { TranslateLoaderMock } from '../../../../shared/mocks/translate-loader.mock';

describe('ParameterValueInputComponent', () => {
  let component: ParameterValueInputComponent;
  let fixture: ComponentFixture<ParameterValueInputComponent>;
  let booleanParameter;
  let stringParameter;
  let fileParameter;
  let dateParameter;
  let outputParameter;

  function init() {
    booleanParameter = Object.assign(new ScriptParameter(), { type: ScriptParameterType.BOOLEAN });
    stringParameter = Object.assign(new ScriptParameter(), { type: ScriptParameterType.STRING });
    fileParameter = Object.assign(new ScriptParameter(), { type: ScriptParameterType.FILE });
    dateParameter = Object.assign(new ScriptParameter(), { type: ScriptParameterType.DATE });
    outputParameter = Object.assign(new ScriptParameter(), { type: ScriptParameterType.OUTPUT });
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })],
      declarations: [
        ParameterValueInputComponent,
        BooleanValueInputComponent,
        StringValueInputComponent,
        FileValueInputComponent,
        DateValueInputComponent,
        FileValueAccessorDirective,
        FileValidator
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ParameterValueInputComponent);
    component = fixture.componentInstance;
    component.parameter = stringParameter;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show a BooleanValueInputComponent when the parameter type is boolean', () => {
    component.parameter = booleanParameter;
    fixture.detectChanges();
    const valueInput = fixture.debugElement.query(By.directive(BooleanValueInputComponent));
    expect(valueInput).toBeTruthy();
  });

  it('should show a StringValueInputComponent when the parameter type is string', () => {
    component.parameter = stringParameter;
    fixture.detectChanges();
    const valueInput = fixture.debugElement.query(By.directive(StringValueInputComponent));
    expect(valueInput).toBeTruthy();
  });

  it('should show a FileValueInputComponent when the parameter type is file', () => {
    component.parameter = fileParameter;
    fixture.detectChanges();
    const valueInput = fixture.debugElement.query(By.directive(FileValueInputComponent));
    expect(valueInput).toBeTruthy();
  });

  it('should show a DateValueInputComponent when the parameter type is date', () => {
    component.parameter = dateParameter;
    fixture.detectChanges();
    const valueInput = fixture.debugElement.query(By.directive(DateValueInputComponent));
    expect(valueInput).toBeTruthy();
  });

  it('should show a StringValueInputComponent when the parameter type is output', () => {
    component.parameter = outputParameter;
    fixture.detectChanges();
    const valueInput = fixture.debugElement.query(By.directive(StringValueInputComponent));
    expect(valueInput).toBeTruthy();
  });
});
