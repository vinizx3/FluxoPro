import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { PageResponse, STATUS_LABEL } from './opportunity';
import { PROJECT_STATUS_LABEL } from './project';

export type AuditEntityType = 'CUSTOMER' | 'OPPORTUNITY' | 'PROJECT';
export type AuditAction = 'CREATED' | 'UPDATED' | 'STATUS_CHANGED' | 'ARCHIVED';

export interface AuditChange {
  field: string;
  oldValue: string | null;
  newValue: string | null;
}

export interface AuditEntry {
  id: string;
  actorId: string;
  /** Nome de quem fez a ação; se faltar, a tela mostra "Usuário". */
  actorName?: string | null;
  entityType: AuditEntityType;
  entityId: string;
  action: AuditAction;
  occurredAt: string;
  changes: AuditChange[];
}

export const ENTITY_LABEL: Record<AuditEntityType, string> = {
  CUSTOMER: 'cliente',
  OPPORTUNITY: 'negociação',
  PROJECT: 'projeto',
};

export const ACTION_LABEL: Record<AuditAction, string> = {
  CREATED: 'Criou',
  UPDATED: 'Editou',
  STATUS_CHANGED: 'Mudou a etapa de',
  ARCHIVED: 'Arquivou',
};

const FIELD_LABEL: Record<string, string> = {
  title: 'Título',
  name: 'Nome',
  amount: 'Valor',
  status: 'Etapa',
  lostReason: 'Motivo da perda',
  description: 'Descrição',
  expectedDeliveryDate: 'Entrega prevista',
  email: 'E-mail',
  phone: 'Telefone',
  customerId: 'Cliente',
  owner: 'Responsável',
  sourceOpportunityId: 'Negociação de origem',
};

export function fieldLabel(field: string): string {
  return FIELD_LABEL[field] ?? field;
}

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** Deixa um valor do histórico legível: etapas em português, dinheiro em reais, datas em dd/MM/yyyy. */
export function formatValue(field: string, value: string | null): string {
  if (value === null || value === '') {
    return '—';
  }
  if (field.endsWith('Id')) {
    return 'vinculado';
  }
  if (field === 'status') {
    const labels: Record<string, string> = { ...STATUS_LABEL, ...PROJECT_STATUS_LABEL };
    return labels[value] ?? value;
  }
  if (field === 'amount' && !Number.isNaN(Number(value))) {
    return currency.format(Number(value)).replace(/ /g, ' ');
  }
  const date = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (date) {
    return `${date[3]}/${date[2]}/${date[1]}`;
  }
  return value;
}

@Injectable({ providedIn: 'root' })
export class AuditApi {
  private readonly http = inject(HttpClient);

  list(entityType: AuditEntityType | null, entityId: string | null, page: number, size = 20) {
    const params: Record<string, string | number> = { page, size };
    if (entityType) {
      params['entityType'] = entityType;
    }
    if (entityId) {
      params['entityId'] = entityId;
    }
    return this.http.get<PageResponse<AuditEntry>>(`${API_BASE}/audit-log`, { params });
  }
}
