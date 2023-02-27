import { Component, Optional } from '@angular/core';
import { ValueInputComponent } from '../value-input.component';
import { ControlContainer, NgForm } from '@angular/forms';
import { controlContainerFactory } from '../../../process-form.component';

/**
 * Represents the user inputted value of a file parameter
 */
@Component({
  selector: 'ds-file-value-input',
  templateUrl: './file-value-input.component.html',
  styleUrls: ['./file-value-input.component.scss'],
  viewProviders: [ { provide: ControlContainer,
    useFactory: controlContainerFactory,
    deps: [[new Optional(), NgForm]] } ]
})
export class FileValueInputComponent extends ValueInputComponent<File> {
  /**
   * The current value of the file
   */
  fileObject: File;
  setFile(files) {
    this.fileObject = files.length > 0 ? files[0] : undefined;
    this.updateValue.emit(this.fileObject);
  }
}
