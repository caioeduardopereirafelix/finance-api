import { EMAIL_PATTERN } from './email';

describe('EMAIL_PATTERN', () => {
  it('aceita endereços com domínio e TLD', () => {
    expect(EMAIL_PATTERN.test('a@b.co')).toBe(true);
    expect(EMAIL_PATTERN.test('caio.felix+teste@sub.exemplo.com.br')).toBe(true);
  });

  it('recusa endereços sem TLD ou malformados', () => {
    for (const email of ['afafasf@gfsgsg', 'a@b.c', 'sem-arroba.com', 'com espaco@exemplo.com', 'dois@@exemplo.com', '@exemplo.com']) {
      expect(EMAIL_PATTERN.test(email)).toBe(false);
    }
  });
});
