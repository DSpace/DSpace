import { ArrayMoveChangeAnalyzer } from './array-move-change-analyzer.service';
import { moveItemInArray } from '@angular/cdk/drag-drop';
import { Operation } from 'fast-json-patch';

/**
 * Helper class for creating move tests
 * Define a "from" and "to" index to move objects within the array before comparing
 */
class MoveTest {
  from: number;
  to: number;

  constructor(from: number, to: number) {
    this.from = from;
    this.to = to;
  }
}

describe('ArrayMoveChangeAnalyzer', () => {
  const comparator = new ArrayMoveChangeAnalyzer<string>();

  let originalArray = [];

  describe('when all values are defined', () => {
    beforeEach(() => {
      originalArray = [
        '98700118-d65d-4636-b1d0-dba83fc932e1',
        '4d7d0798-a8fa-45b8-b4fc-deb2819606c8',
        'e56eb99e-2f7c-4bee-9b3f-d3dcc83386b1',
        '0f608168-cdfc-46b0-92ce-889f7d3ac684',
        '546f9f5c-15dc-4eec-86fe-648007ac9e1c'
      ];
    });

    testMove([
      { op: 'move', from: '/2', path: '/4' },
    ], new MoveTest(2, 4));

    testMove([
      { op: 'move', from: '/0', path: '/3' },
    ], new MoveTest(0, 3));

    testMove([
      { op: 'move', from: '/2', path: '/3' },
      { op: 'move', from: '/0', path: '/3' },
    ], new MoveTest(0, 3), new MoveTest(1, 2));

    testMove([
      { op: 'move', from: '/3', path: '/4' },
      { op: 'move', from: '/0', path: '/1' },
    ], new MoveTest(0, 1), new MoveTest(3, 4));

    testMove([], new MoveTest(0, 4), new MoveTest(4, 0));

    testMove([
      { op: 'move', from: '/2', path: '/3' },
      { op: 'move', from: '/0', path: '/3' },
    ], new MoveTest(0, 4), new MoveTest(1, 3), new MoveTest(2, 4));

    testMove([
      { op: 'move', from: '/3', path: '/4' },
      { op: 'move', from: '/2', path: '/4' },
      { op: 'move', from: '/1', path: '/3' },
      { op: 'move', from: '/0', path: '/3' },
    ], new MoveTest(4, 1), new MoveTest(4, 2), new MoveTest(0, 3));
  });

  describe('when some values are undefined (index 2 and 3)', () => {
    beforeEach(() => {
      originalArray = [
        '98700118-d65d-4636-b1d0-dba83fc932e1',
        '4d7d0798-a8fa-45b8-b4fc-deb2819606c8',
        undefined,
        undefined,
        '546f9f5c-15dc-4eec-86fe-648007ac9e1c'
      ];
    });

    // It can't create a move operation for undefined values, so it should create move operations for the defined values instead
    testMove([
      { op: 'move', from: '/4', path: '/3' },
    ], new MoveTest(2, 4));

    // Moving a defined value should result in the same operations
    testMove([
      { op: 'move', from: '/0', path: '/3' },
    ], new MoveTest(0, 3));
  });

  /**
   * Helper function for creating a move test
   *
   * @param expectedOperations  An array of expected operations after comparing the original array with the array
   *                            created using the provided MoveTests
   * @param moves               An array of MoveTest objects telling the test where to move objects before comparing
   */
  function testMove(expectedOperations: Operation[], ...moves: MoveTest[]) {
    describe(`move ${moves.map((move) => `${move.from} to ${move.to}`).join(' and ')}`, () => {
      let result;

      beforeEach(() => {
        const movedArray = [...originalArray];
        moves.forEach((move) => {
          moveItemInArray(movedArray, move.from, move.to);
        });
        result = comparator.diff(originalArray, movedArray);
      });

      it('should create the expected move operations', () => {
        expect(result).toEqual(expectedOperations);
      });
    });
  }
});
