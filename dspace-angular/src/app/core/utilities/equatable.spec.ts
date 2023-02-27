/* eslint-disable max-classes-per-file */
import { EquatableObject, excludeFromEquals, fieldsForEquals } from './equals.decorators';
import cloneDeep from 'lodash/cloneDeep';

class Dog extends EquatableObject<Dog> {
  public name: string;

  @excludeFromEquals
  public ballsCaught: number;

  public owner: Owner;

  @fieldsForEquals('name')
  public favouriteToy: { name: string, colour: string };
}

class Owner extends EquatableObject<Owner> {
  @excludeFromEquals
  favouriteFood: string;

  constructor(
    public name: string,
    public age: number,
    favouriteFood: string
  ) {
    super();
    this.favouriteFood = favouriteFood;
  }

}

describe('equatable', () => {
  let dogRoger: Dog;
  let dogMissy: Dog;

  beforeEach(() => {
    dogRoger = new Dog();
    dogRoger.name = 'Roger';
    dogRoger.ballsCaught = 6;
    dogRoger.owner = new Owner('Tommy', 16, 'spaghetti');
    dogRoger.favouriteToy = { name: 'Twinky', colour: 'red' };

    dogMissy = new Dog();
    dogMissy.name = 'Missy';
    dogMissy.ballsCaught = 9;
    dogMissy.owner = new Owner('Jenny', 29, 'pizza');
    dogRoger.favouriteToy = { name: 'McSqueak', colour: 'grey' };
  });

  it('should return false when the other object is undefined', () => {
    const isEqual = dogRoger.equals(undefined);
    expect(isEqual).toBe(false);
  });

  it('should return true when the other object is the exact same object', () => {
    const isEqual = dogRoger.equals(dogRoger);
    expect(isEqual).toBe(true);
  });

  it('should return true when the other object is an exact copy of the first one', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(true);
  });

  it('should return false when the other object differs in all fields', () => {
    const isEqual = dogRoger.equals(dogMissy);
    expect(isEqual).toBe(false);
  });

  it('should return true when the other object only differs in fields that are marked as excludeFromEquals', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.ballsCaught = 4;
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(true);
  });

  it('should return false when the other object differs in fields that are not marked as excludeFromEquals', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.name = 'Elliot';
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(false);
  });

  it('should return true when the other object\'s nested object only differs in fields that are marked as excludeFromEquals, when the nested object is not marked decorated with @fieldsForEquals', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.owner.favouriteFood = 'Sushi';
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(true);
  });

  it('should return false when the other object\'s nested object differs in fields that are not marked as excludeFromEquals, when the nested object is not marked decorated with @fieldsForEquals', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.owner.age = 36;
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(false);
  });

  it('should return true when the other object\'s nested object does not differ in fields that are listed inside the nested @fieldsForEquals decorator', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.favouriteToy.colour = 'green';
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(true);
  });

  it('should return false when the other object\'s nested object differs in fields that are listed inside the nested @fieldsForEquals decorator', () => {
    const copyOfDogRoger = cloneDeep(dogRoger);
    copyOfDogRoger.favouriteToy.name = 'Mister Bone';
    const isEqual = dogRoger.equals(copyOfDogRoger);
    expect(isEqual).toBe(false);
  });
});
