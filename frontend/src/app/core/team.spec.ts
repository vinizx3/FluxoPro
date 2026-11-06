import { TeamMember, transferTargets } from './team';

function member(id: string, active: boolean): TeamMember {
  return {
    id,
    name: `Pessoa ${id}`,
    email: `${id}@empresa.com.br`,
    role: 'SELLER',
    active,
    createdAt: '2026-10-01T10:00:00Z',
  };
}

describe('transferTargets', () => {
  const members = [member('a', true), member('b', true), member('c', false)];

  it('leaves out the current owner and inactive people', () => {
    expect(transferTargets(members, 'a').map((m) => m.id)).toEqual(['b']);
  });

  it('is empty when nobody else is active', () => {
    expect(transferTargets([member('a', true)], 'a')).toEqual([]);
  });
});
