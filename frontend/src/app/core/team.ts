import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { Role } from './auth.service';

export interface TeamMember {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
}

export interface CreateSellerRequest {
  name: string;
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class TeamApi {
  private readonly http = inject(HttpClient);

  list() {
    return this.http.get<TeamMember[]>(`${API_BASE}/users`);
  }

  createSeller(body: CreateSellerRequest) {
    return this.http.post<TeamMember>(`${API_BASE}/users`, body);
  }

  setActive(id: string, active: boolean) {
    return this.http.post<TeamMember>(
      `${API_BASE}/users/${id}/${active ? 'activate' : 'deactivate'}`,
      {},
    );
  }
}

/** Quem pode receber uma negociação: usuários ativos, menos o responsável atual. */
export function transferTargets(members: TeamMember[], currentOwnerId: string): TeamMember[] {
  return members.filter((m) => m.active && m.id !== currentOwnerId);
}
