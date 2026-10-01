export function tokenFromFragment(fragment: string | null | undefined): string | null {
  if (!fragment) {
    return null;
  }
  const token = new URLSearchParams(fragment).get('token')?.trim();
  return token ? token : null;
}
