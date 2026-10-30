import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { PageResponse } from './opportunity';

export interface Customer {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  createdAt: string;
  archivedAt: string | null;
  version: number;
}

export interface CustomerRequest {
  name: string;
  email: string | null;
  phone: string | null;
}

export interface CustomerUpdateRequest extends CustomerRequest {
  version: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerApi {
  private readonly http = inject(HttpClient);

  list(search: string, page: number, size: number) {
    const params: Record<string, string | number> = { page, size };
    if (search.trim()) {
      params['search'] = search.trim();
    }
    return this.http.get<PageResponse<Customer>>(`${API_BASE}/customers`, { params });
  }

  create(body: CustomerRequest) {
    return this.http.post<Customer>(`${API_BASE}/customers`, body);
  }

  update(id: string, body: CustomerUpdateRequest) {
    return this.http.put<Customer>(`${API_BASE}/customers/${id}`, body);
  }

  archive(id: string) {
    return this.http.post<void>(`${API_BASE}/customers/${id}/archive`, {});
  }
}
