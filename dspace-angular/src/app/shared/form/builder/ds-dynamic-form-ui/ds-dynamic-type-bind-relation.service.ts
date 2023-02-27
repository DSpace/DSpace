import { Inject, Injectable, Injector, Optional } from '@angular/core';
import { FormControl } from '@angular/forms';

import { Subscription } from 'rxjs';
import { startWith } from 'rxjs/operators';

import {
  AND_OPERATOR,
  DYNAMIC_MATCHERS,
  DynamicFormControlCondition,
  DynamicFormControlMatcher,
  DynamicFormControlModel,
  DynamicFormControlRelation,
  DynamicFormRelationService, MATCH_VISIBLE,
  OR_OPERATOR
} from '@ng-dynamic-forms/core';

import {hasNoValue, hasValue} from '../../../empty.util';
import { FormBuilderService } from '../form-builder.service';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';
import { DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP } from './ds-dynamic-form-constants';

/**
 * Service to manage type binding for submission input fields
 * Any form component with the typeBindRelations DynamicFormControlRelation property can be controlled this way
 */
@Injectable()
export class DsDynamicTypeBindRelationService {

  constructor(@Optional() @Inject(DYNAMIC_MATCHERS) private dynamicMatchers: DynamicFormControlMatcher[],
              protected dynamicFormRelationService: DynamicFormRelationService,
              protected formBuilderService: FormBuilderService,
              protected injector: Injector) {
  }

  /**
   * Return the string value of the type bind model
   * @param bindModelValue
   * @private
   */
  public getTypeBindValue(bindModelValue: string | FormFieldMetadataValueObject): string {
    let value;
    if (hasNoValue(bindModelValue) || typeof bindModelValue === 'string') {
      value = bindModelValue;
    } else if (bindModelValue instanceof FormFieldMetadataValueObject
      && bindModelValue.hasAuthority()) {
      value = bindModelValue.authority;
    } else {
      value = bindModelValue.value;
    }

    return value;
  }


  /**
   * Get models for this bind type
   * @param model
   */
  public getRelatedFormModel(model: DynamicFormControlModel): DynamicFormControlModel[] {

    const models: DynamicFormControlModel[] = [];

    (model as any).typeBindRelations.forEach((relGroup) => relGroup.when.forEach((rel) => {

      if (model.id === rel.id) {
        throw new Error(`FormControl ${model.id} cannot depend on itself`);
      }

      const bindModel: DynamicFormControlModel = this.formBuilderService.getTypeBindModel();

      if (model && !models.some((modelElement) => modelElement === bindModel)) {
        models.push(bindModel);
      }
    }));

    return models;
  }

  /**
   * Return false if the type bind relation (eg. {MATCH_VISIBLE, OR, ['book', 'book part']}) matches the value in
   * matcher.match or true if the opposite match. Since this is called with regard to actively *hiding* a form
   * component, the negation of the comparison is returned.
   * @param relation type bind relation (eg. {MATCH_VISIBLE, OR, ['book', 'book part']})
   * @param matcher contains 'match' value and an onChange() event listener
   */
  public matchesCondition(relation: DynamicFormControlRelation, matcher: DynamicFormControlMatcher): boolean {

    // Default to OR for operator (OR is explicitly set in field-parser.ts anyway)
    const operator = relation.operator || OR_OPERATOR;


    return relation.when.reduce((hasAlreadyMatched: boolean, condition: DynamicFormControlCondition, index: number) => {
      // Get the DynamicFormControlModel (typeBindModel) from the form builder service, set in the form builder
      // in the form model at init time in formBuilderService.modelFromConfiguration (called by other form components
      // like relation group component and submission section form component).
      // This model (DynamicRelationGroupModel) contains eg. mandatory field, formConfiguration, relationFields,
      // submission scope, form/section type and other high level properties
      const bindModel: any = this.formBuilderService.getTypeBindModel();

      let values: string[];
      let bindModelValue = bindModel.value;

      // If the form type is RELATION, set bindModelValue to the mandatory field for this model, otherwise leave
      // as plain value
      if (bindModel.type === DYNAMIC_FORM_CONTROL_TYPE_RELATION_GROUP) {
        bindModelValue = bindModel.value.map((entry) => entry[bindModel.mandatoryField]);
      }
      // Support multiple bind models
      if (Array.isArray(bindModelValue)) {
        values = [...bindModelValue.map((entry) => this.getTypeBindValue(entry))];
      } else {
        values = [this.getTypeBindValue(bindModelValue)];
      }

      // If bind model evaluates to 'true' (is not undefined, is not null, is not false etc,
      // AND the relation match (type bind) is equal to the matcher match (item publication type), then the return
      // value is initialised as false.
      let returnValue = (!(bindModel && relation.match === matcher.match));

      // Iterate the type bind values parsed and mapped from our form/relation group model
      for (const value of values) {
        if (bindModel && relation.match === matcher.match) {
          // If we're not at the first array element, and we're using the AND operator, and we have not
          // yet matched anything, return false.
          if (index > 0 && operator === AND_OPERATOR && !hasAlreadyMatched) {
            return false;
          }
          // If we're not at the first array element, and we're using the OR operator (almost always the case)
          // and we've already matched then there is no need to continue, just return true.
          if (index > 0 && operator === OR_OPERATOR && hasAlreadyMatched) {
            return true;
          }

          // Do the actual match. Does condition.value (the item publication type) match the field model
          // type bind currently being inspected?
          returnValue = condition.value === value;

          // If return value is already true, break.
          if (returnValue) {
            break;
          }
        }

        // Test opposingMatch (eg. if match is VISIBLE, opposingMatch will be HIDDEN)
        if (bindModel && relation.match === matcher.opposingMatch) {
          // If we're not at the first element, using AND, and already matched, just return true here
          if (index > 0 && operator === AND_OPERATOR && hasAlreadyMatched) {
            return true;
          }

          // If we're not at the first element, using OR, and we have NOT already matched, return false
          if (index > 0 && operator === OR_OPERATOR && !hasAlreadyMatched) {
            return false;
          }

          // Negated comparison for return value since this is expected to be in the context of a HIDDEN_MATCHER
          returnValue = !(condition.value === value);

          // Break if already false
          if (!returnValue) {
            break;
          }
        }
      }
      return returnValue;
    }, false);
  }

  /**
   * Return an array of subscriptions to a calling component
   * @param model
   * @param control
   */
  subscribeRelations(model: DynamicFormControlModel, control: FormControl): Subscription[] {

    const relatedModels = this.getRelatedFormModel(model);
    const subscriptions: Subscription[] = [];

    Object.values(relatedModels).forEach((relatedModel: any) => {

      if (hasValue(relatedModel)) {
        const initValue = (hasNoValue(relatedModel.value) || typeof relatedModel.value === 'string') ? relatedModel.value :
          (Array.isArray(relatedModel.value) ? relatedModel.value : relatedModel.value.value);

        const valueChanges = relatedModel.valueChanges.pipe(
          startWith(initValue)
        );

        // Build up the subscriptions to watch for changes;
        subscriptions.push(valueChanges.subscribe(() => {
          // Iterate each matcher
          if (hasValue(this.dynamicMatchers)) {
            this.dynamicMatchers.forEach((matcher) => {
              // Find the relation
              const relation = this.dynamicFormRelationService.findRelationByMatcher((model as any).typeBindRelations, matcher);
              // If the relation is defined, get matchesCondition result and pass it to the onChange event listener
              if (relation !== undefined) {
                const hasMatch = this.matchesCondition(relation, matcher);
                matcher.onChange(hasMatch, model, control, this.injector);
              }
            });
          }
        }));
      }
    });

    return subscriptions;
  }

  /**
   * Helper function to construct a typeBindRelations array
   * @param configuredTypeBindValues
   */
  public getTypeBindRelations(configuredTypeBindValues: string[]): DynamicFormControlRelation[] {
    const bindValues = [];
    configuredTypeBindValues.forEach((value) => {
      bindValues.push({
        id: 'dc.type',
        value: value
      });
    });
    return [{
      match: MATCH_VISIBLE,
      operator: OR_OPERATOR,
      when: bindValues
    }];
  }

}
