import { useState } from 'react'
import { NeonPanel } from '../../ui/NeonPanel'
import { NeonButton } from '../../ui/NeonButton'
import { SelectorExpediente } from './SelectorExpediente'
import { useModusOperandi } from '../../../hooks/useModusOperandi'
import { reanalizarExpediente } from '../../../services/modusOperandiService'

const ESTADO_COLOR: Record<string, string> = {
    PENDIENTE: 'text-amber-400 border-amber-400/50',
    APROBADA: 'text-emerald-400 border-emerald-400/50',
    CORREGIDA: 'text-cyan-300 border-cyan-400/50',
    RECHAZADA: 'text-red-400 border-red-400/50',
    SIN_COINCIDENCIAS: 'text-cyan-600 border-cyan-600/40',
}

export const PropuestaMoPanel = () => {
    const [expedienteId, setExpedienteId] = useState<number | null>(null)
    const { propuesta, historial, loading, sinPropuesta, refetch } = useModusOperandi(expedienteId)
    const [reanalizando, setReanalizando] = useState(false)

    const handleReanalizar = async () => {
        if (expedienteId == null) return
        setReanalizando(true)
        try {
            await reanalizarExpediente(expedienteId)
            // El análisis corre asíncrono en el backend (Virtual Threads); se
            // refresca después de un momento en vez de esperar una respuesta síncrona.
            setTimeout(refetch, 3000)
        } finally {
            setReanalizando(false)
        }
    }

    return (
        <div className="space-y-6">
            <NeonPanel title="Análisis de Modus Operandi" subtitle="Propuesta generada por IA (HU2)">
                <div className="flex flex-col md:flex-row gap-4 items-end mb-4">
                    <div className="flex-1">
                        <SelectorExpediente value={expedienteId} onChange={setExpedienteId} />
                    </div>
                    <NeonButton
                        variant="outline"
                        onClick={handleReanalizar}
                        disabled={expedienteId == null || reanalizando}
                    >
                        {reanalizando ? 'Encolando…' : 'Reanalizar'}
                    </NeonButton>
                </div>

                {loading && <p className="text-xs text-cyan-500">Cargando propuesta…</p>}

                {!loading && sinPropuesta && expedienteId != null && (
                    <p className="text-xs text-cyan-500">
                        Este expediente todavía no tiene una propuesta de MO generada. El análisis se dispara
                        automáticamente al registrarse el expediente, o puede reintentarse con "Reanalizar".
                    </p>
                )}

                {!loading && propuesta && (
                    <div className="border border-cyan-400/30 rounded p-4 space-y-3">
                        <div className="flex flex-wrap justify-between items-center gap-2">
              <span className={`text-[10px] uppercase tracking-wider px-2 py-1 border rounded ${ESTADO_COLOR[propuesta.estado]}`}>
                {propuesta.estado.replace('_', ' ')}
              </span>
                            <span className="text-xs text-cyan-400">
                Confianza: <strong>{propuesta.nivelConfianza ?? 0}%</strong>
              </span>
                        </div>

                        {propuesta.estado !== 'SIN_COINCIDENCIAS' && (
                            <>
                                <div>
                                    <p className="text-[10px] uppercase tracking-wider text-cyan-500 mb-1">Características comunes</p>
                                    <p className="text-xs text-cyan-300/90">{propuesta.caracteristicasComunes}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] uppercase tracking-wider text-cyan-500 mb-1">Posible firma</p>
                                    <p className="text-xs text-cyan-300/90">{propuesta.posibleFirma}</p>
                                </div>
                                <div>
                                    <p className="text-[10px] uppercase tracking-wider text-cyan-500 mb-1">Consistencia horario/zona</p>
                                    <p className="text-xs text-cyan-300/90">{propuesta.consistenciaHorarioZona}</p>
                                </div>
                            </>
                        )}

                        <div>
                            <p className="text-[10px] uppercase tracking-wider text-cyan-500 mb-1">Resumen</p>
                            <p className="text-xs text-cyan-300/90">{propuesta.resumenGenerado}</p>
                        </div>

                        {propuesta.expedientesSimilares.length > 0 && (
                            <div>
                                <p className="text-[10px] uppercase tracking-wider text-cyan-500 mb-1">Expedientes relacionados</p>
                                <div className="flex flex-wrap gap-2">
                                    {propuesta.expedientesSimilares.map(s => (
                                        <span key={s.expedienteId} className="text-[10px] px-2 py-1 border border-cyan-400/30 rounded text-cyan-300">
                      {s.folio} · {s.similitudPorcentual?.toFixed(1)}%
                    </span>
                                    ))}
                                </div>
                            </div>
                        )}

                        {propuesta.revisadoPorExperto && (
                            <p className="text-[10px] text-emerald-400">
                                ✓ Revisado por {propuesta.analistaRevisor?.nombre ?? 'un analista'}
                            </p>
                        )}
                    </div>
                )}
            </NeonPanel>

            {historial.length > 1 && (
                <NeonPanel title="Historial de versiones" noBorder>
                    <div className="space-y-2">
                        {historial.map(h => (
                            <div key={h.id} className="text-xs text-cyan-400/80 border-l-2 border-cyan-400/30 pl-3">
                                v{h.version} — {h.estado} — {new Date(h.fechaGeneracion).toLocaleString()}
                                {h.vigente && <span className="ml-2 text-emerald-400">(vigente)</span>}
                            </div>
                        ))}
                    </div>
                </NeonPanel>
            )}
        </div>
    )
}