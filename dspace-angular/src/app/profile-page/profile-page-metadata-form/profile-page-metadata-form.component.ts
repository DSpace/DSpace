import { Component, Input, OnInit } from '@angular/core';
import {
  DynamicFormControlModel,
  DynamicFormValueControlModel,
  DynamicInputModel,
  DynamicSelectModel
} from '@ng-dynamic-forms/core';
import { FormGroup } from '@angular/forms';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { TranslateService } from '@ngx-translate/core';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { LangConfig } from '../../../config/lang-config.interface';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import cloneDeep from 'lodash/cloneDeep';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../../core/shared/operators';
import { FormBuilderService } from '../../shared/form/builder/form-builder.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'ds-profile-page-metadata-form',
  templateUrl: './profile-page-metadata-form.component.html'
})
/**
 * Component for a user to edit their metadata
 * Displays a form containing:
 * - readonly email field,
 * - required first name text field
 * - required last name text field
 * - phone text field
 * - language dropdown
 */
export class ProfilePageMetadataFormComponent implements OnInit {
  /**
   * The user to display the form for
   */
  @Input() user: EPerson;

  /**
   * The form's input models
   */
  formModel: DynamicFormControlModel[] = [
    new DynamicInputModel({
      id: 'email',
      name: 'email',
      readOnly: true
    }),
    new DynamicInputModel({
      id: 'firstname',
      name: 'eperson.firstname',
      required: true,
      validators: {
        required: null
      },
      errorMessages: {
        required: 'This field is required'
      },
    }),
    new DynamicInputModel({
      id: 'lastname',
      name: 'eperson.lastname',
      required: true,
      validators: {
        required: null
      },
      errorMessages: {
        required: 'This field is required'
      },
    }),
    new DynamicInputModel({
      id: 'phone',
      name: 'eperson.phone'
    }),
    new DynamicSelectModel<string>({
      id: 'language',
      name: 'eperson.language'
    })
  ];

  /**
   * The form group of this form
   */
  formGroup: FormGroup;

  /**
   * Prefix for the form's label messages of this component
   */
  LABEL_PREFIX = 'profile.metadata.form.label.';

  /**
   * Prefix for the form's error messages of this component
   */
  ERROR_PREFIX = 'profile.metadata.form.error.';

  /**
   * Prefix for the notification messages of this component
   */
  NOTIFICATION_PREFIX = 'profile.metadata.form.notifications.';

  /**
   * All of the configured active languages
   * Used to populate the language dropdown
   */
  activeLangs: LangConfig[];

  constructor(protected formBuilderService: FormBuilderService,
              protected translate: TranslateService,
              protected epersonService: EPersonDataService,
              protected notificationsService: NotificationsService) {
  }

  ngOnInit(): void {
    this.activeLangs = environment.languages.filter((MyLangConfig) => MyLangConfig.active === true);
    this.setFormValues();
    this.updateFieldTranslations();
    this.translate.onLangChange
      .subscribe(() => {
        this.updateFieldTranslations();
      });
  }

  /**
   * Loop over all the form's input models and set their values depending on the user's metadata
   * Create the FormGroup
   */
  setFormValues() {
    this.formModel.forEach(
      (fieldModel: any) => {
        if (fieldModel.name === 'email') {
          fieldModel.value = this.user.email;
        } else {
          fieldModel.value = this.user.firstMetadataValue(fieldModel.name);
        }
        if (fieldModel.id === 'language') {
          (fieldModel as DynamicSelectModel<string>).options =
            this.activeLangs.map((langConfig) => Object.assign({ value: langConfig.code, label: langConfig.label }));
        }
      }
    );
    this.formGroup = this.formBuilderService.createFormGroup(this.formModel);
  }

  /**
   * Update the translations of the field labels and error messages
   */
  updateFieldTranslations() {
    this.formModel.forEach(
      (fieldModel: DynamicInputModel) => {
        fieldModel.label = this.translate.instant(this.LABEL_PREFIX + fieldModel.id);
        if (isNotEmpty(fieldModel.validators)) {
          fieldModel.errorMessages = {};
          Object.keys(fieldModel.validators).forEach((key) => {
            fieldModel.errorMessages[key] = this.translate.instant(this.ERROR_PREFIX + fieldModel.id + '.' + key);
          });
        }
      }
    );
  }

  /**
   * Update the user's metadata
   *
   * Sends a patch request for updating the user's metadata when at least one value changed or got added/removed and the
   * form is valid.
   * Nothing happens when the form is invalid or no metadata changed.
   *
   * Returns false when nothing happened.
   */
  updateProfile(): boolean {
    if (!this.formGroup.valid) {
      return false;
    }

    const newMetadata = cloneDeep(this.user.metadata);
    let changed = false;
    this.formModel.filter((fieldModel) => fieldModel.id !== 'email').forEach((fieldModel: DynamicFormValueControlModel<string>) => {
      if (newMetadata.hasOwnProperty(fieldModel.name) && newMetadata[fieldModel.name].length > 0) {
        if (hasValue(fieldModel.value)) {
          if (newMetadata[fieldModel.name][0].value !== fieldModel.value) {
            newMetadata[fieldModel.name][0].value = fieldModel.value;
            changed = true;
          }
        } else {
          newMetadata[fieldModel.name] = [];
          changed = true;
        }
      } else if (hasValue(fieldModel.value)) {
        newMetadata[fieldModel.name] = [{
          value: fieldModel.value,
          language: null
        } as any];
        changed = true;
      }
    });

    if (changed) {
      this.epersonService.update(Object.assign(cloneDeep(this.user), {metadata: newMetadata})).pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload()
      ).subscribe((user) => {
        this.user = user;
        this.setFormValues();
        this.notificationsService.success(
          this.translate.instant(this.NOTIFICATION_PREFIX + 'success.title'),
          this.translate.instant(this.NOTIFICATION_PREFIX + 'success.content')
        );
      });
    }

    return changed;
  }
}
