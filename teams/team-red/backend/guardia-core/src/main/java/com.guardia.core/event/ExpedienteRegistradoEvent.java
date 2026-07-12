package com.guardia.core.event;

import com.guardia.core.model.Expediente;
import org.springframework.context.ApplicationEvent;

public class ExpedienteRegistradoEvent extends ApplicationEvent {

    private final Expediente expediente;

    public ExpedienteRegistradoEvent(Object source, Expediente expediente) {
        super(source);
        this.expediente = expediente;
    }

    public Expediente getExpediente() {
        return expediente;
    }
}