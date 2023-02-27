import { TestBed } from '@angular/core/testing';

import { ShortNumberPipe } from './short-number.pipe';

describe('ShortNumber Pipe', () => {

  let shortNumberPipe: ShortNumberPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ShortNumberPipe
      ],
    }).compileComponents();

    shortNumberPipe = TestBed.inject(ShortNumberPipe);
  });

  it('should not transform with an invalid number', async () => {
    await testTransform(
      'tre',
      'tre'
    );
  });

  it('should not transform with an empty string', async () => {
    await testTransform(
      '',
      ''
    );
  });

  it('should not transform with zero', async () => {
    await testTransform(
      0,
      '0'
    );
  });

  it('should render 1K', async () => {
    await testTransform(
      '1000',
      '1K'
    );
  });

  it('should render 1K', async () => {
    await testTransform(
      1000,
      '1K'
    );
  });

  it('should render 19.3K', async () => {
    await testTransform(
      19300,
      '19.3K'
    );
  });

  it('should render 1M', async () => {
    await testTransform(
      1000000,
      '1M'
    );
  });

  it('should render 1B', async () => {
    await testTransform(
      1000000000,
      '1B'
    );
  });

  it('should render 1T', async () => {
    await testTransform(
      1000000000000,
      '1T'
    );
  });

  it('should render 1Q', async () => {
    await testTransform(
      1000000000000000,
      '1Q'
    );
  });

  async function testTransform(input: any, output: string) {
    expect(
      await shortNumberPipe.transform(input)
    ).toMatch(
      output
    );
  }
});
