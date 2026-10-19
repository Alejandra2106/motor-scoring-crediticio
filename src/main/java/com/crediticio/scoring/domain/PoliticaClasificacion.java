package com.crediticio.scoring.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Política de clasificación inmutable. Una política nueva solo se construye si sus rangos,
 * evaluados en conjunto, cubren exactamente todos los enteros entre -100 y 130 con una única
 * categoría por nivel; de lo contrario se reportan todos los errores encontrados juntos.
 * No impone un orden entre BAJO, MEDIO y ALTO: CA09 solo exige que la política esté completa
 * (todas las categorías presentes) y que ninguna categoría esté duplicada.
 * Toda publicación nueva registra su autor (CA14); las versiones publicadas antes de HU10 no lo
 * tienen y se reconstruyen con autor nulo.
 */
public class PoliticaClasificacion {

    public static final int LIMITE_MINIMO = -100;
    public static final int LIMITE_MAXIMO = 130;
    private static final int CANTIDAD_RANGOS = CategoriaRiesgo.values().length;

    private final Long idPolitica;
    private final Integer version;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaPublicacion;
    private final UUID publicadoPor;
    private final List<RangoClasificacion> rangos;

    private PoliticaClasificacion(Long idPolitica, Integer version, LocalDateTime fechaCreacion,
            LocalDateTime fechaPublicacion, UUID publicadoPor, List<RangoClasificacion> rangos) {
        this.idPolitica = idPolitica;
        this.version = version;
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "fechaCreacion es obligatoria");
        this.fechaPublicacion = Objects.requireNonNull(fechaPublicacion, "fechaPublicacion es obligatoria");
        if (!fechaCreacion.equals(fechaPublicacion)) {
            throw new IllegalArgumentException("fechaCreacion y fechaPublicacion deben ser el mismo instante");
        }
        this.publicadoPor = publicadoPor;
        this.rangos = Objects.requireNonNull(rangos, "rangos es obligatorio").stream()
                .sorted(Comparator.comparingInt(RangoClasificacion::getLimiteInferior))
                .toList();
    }

    public static PoliticaClasificacion nueva(LocalDateTime instantePublicacion, UUID publicadoPor,
            List<DefinicionRango> definiciones) {
        Objects.requireNonNull(instantePublicacion, "instantePublicacion es obligatorio");
        Objects.requireNonNull(publicadoPor, "publicadoPor es obligatorio");
        List<String> errores = validarDefiniciones(definiciones);
        if (!errores.isEmpty()) {
            throw new PoliticaClasificacionInvalidaException(errores);
        }
        List<RangoClasificacion> rangos = definiciones.stream()
                .map(definicion -> RangoClasificacion.nuevo(
                        definicion.categoria(), definicion.limiteInferior(), definicion.limiteSuperior()))
                .toList();
        return new PoliticaClasificacion(null, null, instantePublicacion, instantePublicacion, publicadoPor, rangos);
    }

    /** publicadoPor es nulo en las versiones publicadas antes de que se registrara el autor. */
    public static PoliticaClasificacion reconstruir(Long idPolitica, Integer version, LocalDateTime fechaCreacion,
            LocalDateTime fechaPublicacion, UUID publicadoPor, List<RangoClasificacion> rangos) {
        Objects.requireNonNull(idPolitica, "idPolitica es obligatorio");
        Objects.requireNonNull(version, "version es obligatoria");
        if (version <= 0) {
            throw new IllegalArgumentException("version debe ser mayor que 0");
        }
        return new PoliticaClasificacion(idPolitica, version, fechaCreacion, fechaPublicacion, publicadoPor, rangos);
    }

    private static List<String> validarDefiniciones(List<DefinicionRango> definiciones) {
        List<String> errores = new ArrayList<>();
        if (definiciones == null || definiciones.isEmpty()) {
            errores.add("La política debe contener exactamente " + CANTIDAD_RANGOS + " rangos");
            return errores;
        }
        if (definiciones.size() != CANTIDAD_RANGOS) {
            errores.add("La política debe contener exactamente " + CANTIDAD_RANGOS + " rangos y contiene "
                    + definiciones.size());
        }

        boolean limitesValidos = true;
        Set<CategoriaRiesgo> categoriasPresentes = EnumSet.noneOf(CategoriaRiesgo.class);
        for (int i = 0; i < definiciones.size(); i++) {
            DefinicionRango definicion = definiciones.get(i);
            String prefijo = "rangos[" + i + "]: ";
            if (definicion == null) {
                errores.add(prefijo + "el rango es obligatorio");
                limitesValidos = false;
                continue;
            }
            if (definicion.categoria() == null) {
                errores.add(prefijo + "categoria es obligatoria");
            } else if (!categoriasPresentes.add(definicion.categoria())) {
                errores.add("La categoría " + definicion.categoria() + " está duplicada");
            }
            limitesValidos &= validarLimites(definicion, prefijo, errores);
        }

        for (CategoriaRiesgo categoria : CategoriaRiesgo.values()) {
            if (!categoriasPresentes.contains(categoria)) {
                errores.add("Falta el rango de la categoría " + categoria);
            }
        }

        if (limitesValidos) {
            validarCobertura(definiciones, errores);
        }
        return errores;
    }

    private static boolean validarLimites(DefinicionRango definicion, String prefijo, List<String> errores) {
        Integer limiteInferior = definicion.limiteInferior();
        Integer limiteSuperior = definicion.limiteSuperior();
        boolean valido = true;
        if (limiteInferior == null) {
            errores.add(prefijo + "limiteInferior es obligatorio");
            valido = false;
        } else if (estaFueraDelIntervalo(limiteInferior)) {
            errores.add(prefijo + "limiteInferior debe estar entre " + LIMITE_MINIMO + " y " + LIMITE_MAXIMO);
            valido = false;
        }
        if (limiteSuperior == null) {
            errores.add(prefijo + "limiteSuperior es obligatorio");
            valido = false;
        } else if (estaFueraDelIntervalo(limiteSuperior)) {
            errores.add(prefijo + "limiteSuperior debe estar entre " + LIMITE_MINIMO + " y " + LIMITE_MAXIMO);
            valido = false;
        }
        if (limiteInferior != null && limiteSuperior != null && limiteInferior > limiteSuperior) {
            errores.add(prefijo + "limiteInferior no puede ser mayor que limiteSuperior");
            valido = false;
        }
        return valido;
    }

    private static boolean estaFueraDelIntervalo(int limite) {
        return limite < LIMITE_MINIMO || limite > LIMITE_MAXIMO;
    }

    private static void validarCobertura(List<DefinicionRango> definiciones, List<String> errores) {
        List<DefinicionRango> ordenadas = definiciones.stream()
                .sorted(Comparator.comparing(DefinicionRango::limiteInferior)
                        .thenComparing(DefinicionRango::limiteSuperior))
                .toList();
        int siguienteValorEsperado = LIMITE_MINIMO;
        for (DefinicionRango definicion : ordenadas) {
            if (definicion.limiteInferior() > siguienteValorEsperado) {
                errores.add("Los valores entre " + siguienteValorEsperado + " y " + (definicion.limiteInferior() - 1)
                        + " no están cubiertos por ningún rango");
            } else if (definicion.limiteInferior() < siguienteValorEsperado) {
                errores.add("El rango [" + definicion.limiteInferior() + ", " + definicion.limiteSuperior()
                        + "] se superpone con otro rango entre " + definicion.limiteInferior() + " y "
                        + Math.min(definicion.limiteSuperior(), siguienteValorEsperado - 1));
            }
            siguienteValorEsperado = Math.max(siguienteValorEsperado, definicion.limiteSuperior() + 1);
        }
        if (siguienteValorEsperado <= LIMITE_MAXIMO) {
            errores.add("Los valores entre " + siguienteValorEsperado + " y " + LIMITE_MAXIMO
                    + " no están cubiertos por ningún rango");
        }
    }

    public Long getIdPolitica() {
        return idPolitica;
    }

    public Integer getVersion() {
        return version;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public UUID getPublicadoPor() {
        return publicadoPor;
    }

    public List<RangoClasificacion> getRangos() {
        return rangos;
    }

    /**
     * Categoría del rango que contiene el puntaje, o vacío si ningún rango lo contiene. Qué hacer
     * con un puntaje sin categoría (HU12 CA04) lo decide quien clasifica, no la política.
     */
    public Optional<CategoriaRiesgo> clasificar(int puntaje) {
        return rangos.stream()
                .filter(rango -> rango.contiene(puntaje))
                .map(RangoClasificacion::getCategoria)
                .findFirst();
    }
}
