import { NeonSelect } from '../../ui/NeonSelect'
import { useExpedientesActivos } from '../../../hooks/useExpedientesActivos'

interface SelectorExpedienteProps {
    value: number | null
    onChange: (id: number | null) => void
    label?: string
}

export const SelectorExpediente = ({ value, onChange, label = 'Expediente' }: SelectorExpedienteProps) => {
    const { expedientes, loading } = useExpedientesActivos()

    return (
        <NeonSelect
            label={label}
            value={value ?? ''}
            onChange={(e) => onChange(e.target.value ? Number(e.target.value) : null)}
            options={[
                { value: '', label: loading ? 'Cargando expedientes…' : 'Seleccione un expediente' },
                ...expedientes.map(e => ({ value: e.id, label: `${e.folioCOPP} — ${e.tipoDelito ?? 'Sin tipo'}` })),
            ]}
        />
    )
}