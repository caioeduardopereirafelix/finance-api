import { tokenFromFragment } from './token';

describe('tokenFromFragment', () => {
  it('lê o token do fragmento da URL', () => {
    expect(tokenFromFragment('token=abc-DEF_123')).toBe('abc-DEF_123');
  });

  it('sem fragmento ou sem token devolve null', () => {
    expect(tokenFromFragment(null)).toBeNull();
    expect(tokenFromFragment(undefined)).toBeNull();
    expect(tokenFromFragment('')).toBeNull();
    expect(tokenFromFragment('outro=1')).toBeNull();
    expect(tokenFromFragment('token=')).toBeNull();
    expect(tokenFromFragment('token=%20')).toBeNull();
  });
});
