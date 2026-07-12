import { apiClient } from './api'
import type { PropuestaModusOperandiResponseDTO } from '../types/api.types'

export async function obtenerPropuestaActual(expedienteId: number): Promise<PropuestaModusOperandiResponseDTO> {
    const res = await apiClient.get<{ data: PropuestaModusOperandiResponseDTO }>(
        `/analisis-mo/expedientes/${expedienteId}`)
    return res.data
}

export async function obtenerHistorialMO(expedienteId: number): Promise<PropuestaModusOperandiResponseDTO[]> {
    const res = await apiClient.get<{ data: PropuestaModusOperandiResponseDTO[] }>(
        `/analisis-mo/expedientes/${expedienteId}/historial`)
    return res.data
}

export async function reanalizarExpediente(expedienteId: number): Promise<void> {
    await apiClient.post<{ data: null }>(`/analisis-mo/expedientes/${expedienteId}/analizar`, {})
}

export async function compararExpedientes(
    expedienteAId: number, expedienteBId: number,
): Promise<{ similitudPorcentual: number }> {
    const res = await apiClient.get<{ data: { similitudPorcentual: number } }>(
        `/analisis-mo/comparar?expedienteAId=${expedienteAId}&expedienteBId=${expedienteBId}`)
    return res.data
}