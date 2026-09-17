package com.crediticio.shared.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {

    private static final String UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final TraceIdFilter traceIdFilter = new TraceIdFilter();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void debeGenerarUnUuidCuandoNoLlegaElHeader() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        traceIdFilter.doFilterInternal(request, response, filterChain);

        String traceIdDevuelto = capturarTraceIdDevuelto(response);
        assertThat(traceIdDevuelto).matches(UUID_PATTERN);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void debeReutilizarElHeaderCuandoEsUnUuidValido() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        String uuidValido = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        when(request.getHeader("X-Trace-Id")).thenReturn(uuidValido);

        traceIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(capturarTraceIdDevuelto(response)).isEqualTo(uuidValido);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void debeGenerarUnNuevoUuidCuandoElHeaderNoEsValido() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("X-Trace-Id")).thenReturn("no-es-un-uuid");

        traceIdFilter.doFilterInternal(request, response, filterChain);

        String traceIdDevuelto = capturarTraceIdDevuelto(response);
        assertThat(traceIdDevuelto).matches(UUID_PATTERN);
        assertThat(traceIdDevuelto).isNotEqualTo("no-es-un-uuid");
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void debeLimpiarElMdcAunCuandoLaCadenaDeFiltrosFalle() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(null);
        doThrow(new RuntimeException("fallo simulado")).when(filterChain).doFilter(any(), any());

        assertThatThrownBy(() -> traceIdFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class);

        assertThat(MDC.get("traceId")).isNull();
    }

    private String capturarTraceIdDevuelto(HttpServletResponse response) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Trace-Id"), captor.capture());
        return captor.getValue();
    }
}
