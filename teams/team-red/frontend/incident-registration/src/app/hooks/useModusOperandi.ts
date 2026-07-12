import { useState, useEffect, useCallback } from 'react'
import { obtenerPropuestaActual, obtenerHistorialMO } from '../services/modusOperandiService'
import type { PropuestaModusOperandiResponseDTO } from '../types/api.types'

export function useModusOperandi(expedienteId: number | null) {
    const [propuesta, setPropuesta] = useState<PropuestaModusOperandiResponseDTO | null>(null)
    const [historial, setHistorial] = useState<PropuestaModusOperandiResponseDTO[]>([])
    const [loading, setLoading] = useState(false)
    const [sinPropuesta, setSinPropuesta] = useState(false)

    const fetch = useCallback(async () => {
        if (expedienteId == null) return
        setLoading(true)
        setSinPropuesta(false)
        try {
            const [actual, hist] = await Promise.all([
                obtenerPropuestaActual(expedienteId),
                obtenerHistorialMO(expedienteId),
            ])
            setPropuesta(actual)
            setHistorial(hist)
        } catch (err) {
            console.warn('[useModusOperandi] Aún no hay propuesta de MO para este expediente.', err)
            setPropuesta(null)
            setSinPropuesta(true)
        } finally {
            setLoading(false)
        }
    }, [expedienteId])

    useEffect(() => { fetch() }, [fetch])

    return { propuesta, historial, loading, sinPropuesta, refetch: fetch }
}