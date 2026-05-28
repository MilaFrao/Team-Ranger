// Transforma el estado del formulario en el JSON
import type { IncidentePayload } from '../../../types/api.types'
import type { DelitoEntry }      from './useDelitoList.ts'
import { TIPOS_REQUIEREN_DENUNCIANTE } from '../../../context/FormContext'

type FormData = any

export interface CrearPayloadOptions {
    formData: FormData
    delitos:  DelitoEntry[]
    gpsMode:  'actual' | 'none'
}

export function crearPayloadIncidente({
                                          formData,
                                          delitos,
                                      }: CrearPayloadOptions): IncidentePayload {

    const tipoRegistro: string = formData.tipoRegistro ?? ''

    const esDenunciaFormal = tipoRegistro === 'denuncia_formal'
    const requiresDenunciante = TIPOS_REQUIEREN_DENUNCIANTE.includes(tipoRegistro as any)

    const fotos = (formData.involucrados ?? [])
        .map((inv: { foto?: File | null }, i: number) =>
            inv.foto ? { involucradoIndex: i, archivo: '<<multipart — llamada separada>>' } : null,
        )
        .filter((f: unknown): f is { involucradoIndex: number; archivo: string } => f !== null)

    return {
        tipoRegistro,

        ubicacion: {
            municipio:  formData.municipio          ?? '',
            sector:     formData.sector             ?? '',
            direccion:  formData.ubicacionDireccion ?? '',
            referencia: formData.referencia         ?? '',
            coordenadas:
                formData.lat !== null && formData.lng !== null
                    ? { lat: formData.lat, lng: formData.lng }
                    : null,
        },

        delitos: delitos.map(d => ({
            tipoDelito:    d.tipoLabel,
            subtipoDelito: d.subtipoValue,
            fechaHecho:    d.fechaHecho,
            horaInicio:    d.horaInicio,
            horaFin:       d.hechoEnCurso ? null : (d.horaFin || null),
            hechoEnCurso:  d.hechoEnCurso,
        })),

        descripcion: formData.descripcion ?? '',

        involucrados: (formData.involucrados ?? []).map((inv: {
            tipoInvolucrado: string
            nombre:          string
            identificacion:  string
            nacionalidad?:   string
            telefono?:       string
            direccion?:      string
        }) => ({
            tipo:           inv.tipoInvolucrado,
            nombre:         inv.nombre,
            identificacion: inv.identificacion,
            nacionalidad:   inv.nacionalidad  || '',
            telefono:       inv.telefono      || null,
            direccion:      inv.direccion     || null,
        })),

        esDenunciaFormal,
        denunciante: (() => {
            if (!requiresDenunciante) return null;
            const denuncianteEncontrado = (formData.involucrados ?? []).find(
                (inv: any) => inv.tipoInvolucrado === 'denunciante'
            );
            if (denuncianteEncontrado) {
                return {
                    nombre: formData.denuncianteNombre ?? '',
                    identificacion: formData.denuncianteIdentificacion ?? '',
                    telefono: formData.denuncianteTelefono ?? '',
                    nacionalidad: formData.denuncianteNacionalidad ?? '',
                    direccion: formData.denuncianteDireccion ?? '',
                    relacionConCrimen: formData.denuncianteRelacion ?? '',
                };
            }
            return null;
        })(),

        reporte: {
            fechaReporte:      formData.fechaReporte      ?? '',
            horaReporte:       formData.horaReporte       ?? '',
            agenteRegistrador: formData.agenteRegistrador ?? '',
            investigador:      formData.investigador      ?? '',
        },

        fotos,
    }
}
