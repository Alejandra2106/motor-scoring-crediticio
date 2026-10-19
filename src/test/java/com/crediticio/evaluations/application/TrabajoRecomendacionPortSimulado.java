package com.crediticio.evaluations.application;

import com.crediticio.evaluations.ports.output.ResultadoReclamo;
import com.crediticio.evaluations.ports.output.TrabajoRecomendacionPort;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/** Puerto simulado de la outbox: registra las llamadas y devuelve los resultados programados. */
class TrabajoRecomendacionPortSimulado implements TrabajoRecomendacionPort {

    final List<String> llamadas = new ArrayList<>();
    final Deque<ResultadoReclamo> reclamos = new ArrayDeque<>();
    boolean registroVigente = true;

    @Override
    public void crear(long idEvaluacion, UUID token, Duration concesion) {
        llamadas.add("crear:" + idEvaluacion);
    }

    @Override
    public ResultadoReclamo reclamarSiguiente(int maxIntentos, UUID token, Duration concesion) {
        llamadas.add("reclamar:" + maxIntentos + ":" + concesion.toSeconds());
        return reclamos.isEmpty() ? new ResultadoReclamo.Ninguno() : reclamos.removeFirst();
    }

    @Override
    public boolean registrarExito(long idEvaluacion, UUID token) {
        llamadas.add("exito:" + idEvaluacion);
        return registroVigente;
    }

    @Override
    public boolean registrarErrorTransitorio(long idEvaluacion, UUID token, Duration espera, String codigo,
            String detalle) {
        llamadas.add("transitorio:" + idEvaluacion + ":" + codigo + ":" + espera.toSeconds());
        return registroVigente;
    }

    @Override
    public boolean registrarErrorPermanente(long idEvaluacion, UUID token, String codigo, String detalle) {
        llamadas.add("permanente:" + idEvaluacion + ":" + codigo);
        return registroVigente;
    }
}
