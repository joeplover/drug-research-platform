import http from '@/api/http'
import type { HealthInfo } from '@/types/platform'

export async function getHealth() {
  const { data } = await http.get<HealthInfo>('/health')
  return data
}
