import { hasNoValue, hasValue, isEmpty } from '../../shared/empty.util';
import { GenericConstructor } from '../shared/generic-constructor';

const excludedFromEquals = new Map();
const fieldsForEqualsMap = new Map();

/**
 * Method to compare fields of two objects against each other
 * @param object1 The first object for the comparison
 * @param object2 The second object for the comparison
 * @param fieldList The list of property/field names to compare
 */
function equalsByFields(object1, object2, fieldList): boolean {
  const unequalProperty = fieldList.find((key) => {
    if (object1[key] === object2[key]) {
      return false;
    }
    if (hasNoValue(object1[key]) && hasNoValue(object2[key])) {
      return false;
    }
    if (hasNoValue(object1[key]) || hasNoValue(object2[key])) {
      return true;
    }
    const mapping = getFieldsForEquals(object1.constructor, key);
    if (hasValue(mapping)) {
      return !equalsByFields(object1[key], object2[key], mapping);
    }
    if (object1[key] instanceof EquatableObject) {
      return !object1[key].equals(object2[key]);
    }
    if (typeof object1[key] === 'object') {
      return !equalsByFields(object1[key], object2[key], Object.keys(object1));
    }
    return object1[key] !== object2[key];
  });
  return hasNoValue(unequalProperty);
}

/**
 * Abstract class to represent objects that can be compared to each other
 * It provides a default way of comparing
 */
export abstract class EquatableObject<T> {
  equals(other: T): boolean {
    if (hasNoValue(other)) {
      return false;
    }
    if (this as any === other) {
      return true;
    }
    const excludedKeys = getExcludedFromEqualsFor(this.constructor);
    const keys = Object.keys(this).filter((key) => !excludedKeys.includes(key));
    return equalsByFields(this, other, keys);
  }
}

/**
 * Decorator function that adds the equatable settings from the given (parent) object
 * @param parentCo The constructor of the parent object
 */
export function inheritEquatable(parentCo: GenericConstructor<EquatableObject<any>>) {
  return function decorator(childCo: GenericConstructor<EquatableObject<any>>) {
    const parentExcludedFields = getExcludedFromEqualsFor(parentCo) || [];
    const excludedFields = getExcludedFromEqualsFor(childCo) || [];
    excludedFromEquals.set(childCo, [...excludedFields, ...parentExcludedFields]);

    const mappedFields = fieldsForEqualsMap.get(childCo) || new Map();
    const parentMappedFields = fieldsForEqualsMap.get(parentCo) || new Map();
    Array.from(parentMappedFields.keys())
      .filter((key) => !Array.from(mappedFields.keys()).includes(key))
      .forEach((key) => {
        fieldsForEquals(...parentMappedFields.get(key))(new childCo(), key);
      });
  };
}

/**
 * Function to mark properties as excluded from the equals method
 * @param object The object to exclude the property for
 * @param propertyName The name of the property to exclude
 */
export function excludeFromEquals(object: any, propertyName: string): any {
  if (!object) {
    return;
  }
  let list = excludedFromEquals.get(object.constructor);
  if (isEmpty(list)) {
    list = [];
  }
  excludedFromEquals.set(object.constructor, [...list, propertyName]);
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function getExcludedFromEqualsFor(constructor: Function): string[] {
  return excludedFromEquals.get(constructor) || [];
}

/**
 * Function to save the fields that are to be used for a certain property in the equals method for the given object
 * @param fields The fields to use to equate the property of the object
 */
export function fieldsForEquals(...fields: string[]): any {
  return function i(object: any, propertyName: string): any {
    if (!object) {
      return;
    }
    let fieldMap = fieldsForEqualsMap.get(object.constructor);
    if (isEmpty(fieldMap)) {
      fieldMap = new Map();
    }
    fieldMap.set(propertyName, fields);
    fieldsForEqualsMap.set(object.constructor, fieldMap);
  };
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function getFieldsForEquals(constructor: Function, field: string): string[] {
  const fieldMap = fieldsForEqualsMap.get(constructor) || new Map();
  return fieldMap.get(field);
}
