import { useParams } from 'react-router-dom';

const DEFAULT_TENANT = '_';

export function useTenant() {
  const { tenant: tenantParam } = useParams<{ tenant?: string }>();
  const tenant = tenantParam || DEFAULT_TENANT;

  return {
    tenant,
    isDefaultTenant: tenant === DEFAULT_TENANT,
  };
}
