// @ts-ignore
import { useState, useEffect } from 'react'

// --- TIPOS ---

export interface Evidencia {
    id: string
    numeroSecuencial: string  // EV-001, EV-002, etc.
    tipo: string
    descripcion: string
    ubicacion: string
    responsable: string  // Nombre del investigador
    embalaje: string
    horaRecoleccion: string
    hashIntegridad?: string
    timestamp?: string
}

export interface EscenaNegativaItem {
    id: string
    elemento: string
    lugar: string
    resultado: string
    observacion: string  // Campo nuevo
}

export interface EscenaCrimenState {
    paso_actual: 1 | 2 | 3 | 4
    paso1_completado: boolean
    paso2_completado: boolean
    paso3_completado: boolean
    paso4_completado: boolean
    tipoEscena: 'escena_completa' | 'solo_evidencia'
    folioExpediente: string | null  // Nuevo: folio del expediente vinculado
    perimetro: {
        sellado: boolean
        agentes: number
        horaCierre: string
    }
    evidencias: Evidencia[]
    liberacion: {
        hora: string
    }
    escenaNegativa: EscenaNegativaItem[]
    noHayEscenaNegativa: boolean
    alertasIntegridad: Array<{  // Nuevo: alertas de integridad
        evidenciaId: string
        mensaje: string
        integro: boolean
    }>
}

const STORAGE_KEY = 'escena_crimen_local'

// Generador de número secuencial
let contadorEvidencias = 0

function generarNumeroSecuencial(): string {
    contadorEvidencias++
    return `EV-${String(contadorEvidencias).padStart(3, '0')}`
}

function makeInitialState(): EscenaCrimenState {
    contadorEvidencias = 0
    return {
        paso_actual: 1,
        paso1_completado: false,
        paso2_completado: false,
        paso3_completado: false,
        paso4_completado: false,
        tipoEscena: 'escena_completa',
        folioExpediente: null,
        perimetro: {
            sellado: false,
            agentes: 0,
            horaCierre: '',
        },
        evidencias: [],
        liberacion: {
            hora: '',
        },
        escenaNegativa: [],
        noHayEscenaNegativa: false,
        alertasIntegridad: [],
    }
}

// --- HOOK ---

export function useEscenaCrimen() {
    const [state, setState] = useState<EscenaCrimenState>(() => {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved) {
            try {
                const parsed = JSON.parse(saved)
                if (!parsed.evidencias) parsed.evidencias = []
                if (!parsed.escenaNegativa) parsed.escenaNegativa = []
                if (parsed.tipoEscena === undefined) parsed.tipoEscena = 'escena_completa'
                if (parsed.noHayEscenaNegativa === undefined) parsed.noHayEscenaNegativa = false
                if (!parsed.liberacion?.pin) {
                    parsed.liberacion = { hora: parsed.liberacion?.hora || '' }
                }
                if (parsed.alertasIntegridad === undefined) parsed.alertasIntegridad = []
                // Actualizar contador para nuevos IDs
                if (parsed.evidencias.length > 0) {
                    const ultimo = parsed.evidencias[parsed.evidencias.length - 1]
                    const match = ultimo.numeroSecuencial?.match(/\d+/)
                    if (match) contadorEvidencias = parseInt(match[0])
                }
                return parsed
            } catch {
                return makeInitialState()
            }
        }
        return makeInitialState()
    })

    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    }, [state])

    const isPaso1Completado = state.paso1_completado
    const isPaso2Completado = state.paso2_completado
    const isPaso3Completado = state.paso3_completado
    const isPaso4Completado = state.paso4_completado

    const canCompletarPaso1 = state.tipoEscena === 'solo_evidencia' ||
        (state.perimetro.sellado && state.perimetro.agentes > 0 && !!state.perimetro.horaCierre)

    const todasEvidenciasCompletas = state.evidencias.length > 0 && state.evidencias.every(e =>
        e.tipo.trim() !== '' &&
        e.descripcion.trim() !== '' &&
        e.ubicacion.trim() !== '' &&
        e.responsable.trim() !== ''
    )

    const escenaNegativaValida = state.noHayEscenaNegativa ||
        (state.escenaNegativa.length > 0 && state.escenaNegativa.every(en =>
            en.elemento.trim() !== '' && en.lugar.trim() !== '' && en.resultado.trim() !== ''
        ))

    const canCompletarPaso2 = todasEvidenciasCompletas && escenaNegativaValida
    const canCompletarPaso3 = state.tipoEscena === 'solo_evidencia' || state.paso2_completado
    const canCompletarPaso4 = !!state.liberacion.hora

    // --- Acciones ---

    const setFolioExpediente = (folio: string) => {
        setState((prev: EscenaCrimenState) => ({ ...prev, folioExpediente: folio }))
    }

    const completarPaso1 = () => {
        if (!canCompletarPaso1) return
        if (isPaso1Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            paso_actual: 2,
            paso1_completado: true,
        }))
    }

    const completarPaso2 = () => {
        if (!canCompletarPaso2) return
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            paso_actual: 3,
            paso2_completado: true,
        }))
    }

    const completarPaso3 = () => {
        if (!canCompletarPaso3) return
        if (isPaso3Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            paso_actual: 4,
            paso3_completado: true,
        }))
    }

    const completarPaso4 = () => {
        if (!canCompletarPaso4) return
        if (isPaso4Completado) return
        setState((prev: EscenaCrimenState) => ({ ...prev, paso4_completado: true }))
    }

    const updatePerimetro = (patch: Partial<EscenaCrimenState['perimetro']>) => {
        if (isPaso1Completado) return
        setState((prev: EscenaCrimenState) => ({ ...prev, perimetro: { ...prev.perimetro, ...patch } }))
    }

    const setTipoEscena = (tipo: 'escena_completa' | 'solo_evidencia') => {
        if (isPaso1Completado) return
        setState((prev: EscenaCrimenState) => ({ ...prev, tipoEscena: tipo }))
    }

    const addEvidencia = () => {
        if (isPaso2Completado) return
        const nuevoNumero = generarNumeroSecuencial()
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            evidencias: [...prev.evidencias, {
                id: crypto.randomUUID(),
                numeroSecuencial: nuevoNumero,
                tipo: '',
                descripcion: '',
                ubicacion: '',
                responsable: '',
                embalaje: '',
                horaRecoleccion: '',
                hashIntegridad: undefined,
                timestamp: undefined,
            }],
        }))
    }

    const removeEvidencia = (id: string) => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            evidencias: prev.evidencias.filter(e => e.id !== id),
        }))
    }

    const updateEvidencia = (id: string, patch: Partial<Evidencia>) => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            evidencias: prev.evidencias.map(e => e.id === id ? { ...e, ...patch, timestamp: new Date().toISOString() } : e),
        }))
    }

    const updateLiberacion = (patch: Partial<EscenaCrimenState['liberacion']>) => {
        if (isPaso4Completado) return
        setState((prev: EscenaCrimenState) => ({ ...prev, liberacion: { ...prev.liberacion, ...patch } }))
    }

    const addEscenaNegativa = () => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            escenaNegativa: [...prev.escenaNegativa, {
                id: crypto.randomUUID(),
                elemento: '',
                lugar: '',
                resultado: '',
                observacion: ''
            }],
        }))
    }

    const removeEscenaNegativa = (id: string) => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            escenaNegativa: prev.escenaNegativa.filter(e => e.id !== id),
        }))
    }

    const updateEscenaNegativa = (id: string, patch: Partial<EscenaNegativaItem>) => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            escenaNegativa: prev.escenaNegativa.map(e => e.id === id ? { ...e, ...patch } : e),
        }))
    }

    const setNoHayEscenaNegativa = (value: boolean) => {
        if (isPaso2Completado) return
        setState((prev: EscenaCrimenState) => ({
            ...prev,
            noHayEscenaNegativa: value,
            escenaNegativa: value ? [] : prev.escenaNegativa
        }))
    }

    // Simular verificación de integridad (conectar con backend después)
    const verificarIntegridad = async (expedienteId: string) => {
        // Aquí iría la llamada al endpoint /expedientes/{id}/verificar-integridad
        console.log(`Verificando integridad del expediente: ${expedienteId}`)
        // Por ahora simulamos
        const nuevasAlertas = state.evidencias.map(ev => ({
            evidenciaId: ev.id,
            mensaje: `La evidencia ${ev.numeroSecuencial} ha sido modificada`,
            integro: false
        }))
        setState((prev: EscenaCrimenState) => ({ ...prev, alertasIntegridad: nuevasAlertas }))
    }

    const limpiarAlertas = () => {
        setState((prev: EscenaCrimenState) => ({ ...prev, alertasIntegridad: [] }))
    }

    const resetEscena = () => {
        localStorage.removeItem(STORAGE_KEY)
        contadorEvidencias = 0
        setState(makeInitialState())
    }

    return {
        state,
        isPaso1Completado,
        isPaso2Completado,
        isPaso3Completado,
        isPaso4Completado,
        canCompletarPaso1,
        canCompletarPaso2,
        canCompletarPaso3,
        canCompletarPaso4,
        completarPaso1,
        completarPaso2,
        completarPaso3,
        completarPaso4,
        updatePerimetro,
        setTipoEscena,
        setFolioExpediente,
        addEvidencia,
        removeEvidencia,
        updateEvidencia,
        updateLiberacion,
        addEscenaNegativa,
        removeEscenaNegativa,
        updateEscenaNegativa,
        setNoHayEscenaNegativa,
        verificarIntegridad,
        limpiarAlertas,
        resetEscena,
    }
}