Feature: Aceptacion del registro y consulta de solicitantes

  Background:
    * def baseUrl = karate.properties['acceptance.baseUrl']

  Scenario: Registrar un solicitante y consultar sus datos
    Given url baseUrl
    And path 'api', 'v1', 'solicitantes'
    And request { nombreCompleto: 'Ana Maria Perez', numeroDocumento: '987654321000001', ingresosMensuales: 3000000, deudasMensuales: 500000, numeroMoras: 0, historialCrediticio: 'BUENO', antiguedadLaboral: 2.5 }
    When method post
    Then status 201
    And match response.idSolicitante == '#number'
    And match response.numeroDocumento == '987654321000001'
    And match response.fechaRegistro == '#string'
    And match response.ingresosMensuales == '#notpresent'
    * def idRegistrado = response.idSolicitante

    Given url baseUrl
    And path 'api', 'v1', 'solicitantes', 'documento', '987654321000001'
    When method get
    Then status 200
    And match response.idSolicitante == idRegistrado
    And match response.ingresosMensuales == 3000000
    And match response.deudasMensuales == 500000
    And match response.historialCrediticio == 'BUENO'

  Scenario: Rechazar un documento duplicado con un error funcional
    * def solicitud = { nombreCompleto: 'Luis Gomez', numeroDocumento: '987654321000002', ingresosMensuales: 2000000, deudasMensuales: 200000, numeroMoras: 0, historialCrediticio: 'BUENO', antiguedadLaboral: 1 }
    Given url baseUrl
    And path 'api', 'v1', 'solicitantes'
    And request solicitud
    When method post
    Then status 201

    Given url baseUrl
    And path 'api', 'v1', 'solicitantes'
    And request solicitud
    When method post
    Then status 409
    And match response.errorCode == 'DOCUMENTO_DUPLICADO'
    And match response.stackTrace == '#notpresent'

  Scenario: Rechazar datos invalidos antes de guardar
    Given url baseUrl
    And path 'api', 'v1', 'solicitantes'
    And request { nombreCompleto: 'An', numeroDocumento: 'abc', ingresosMensuales: -1, deudasMensuales: -1, numeroMoras: -1, historialCrediticio: 'BUENO', antiguedadLaboral: -1 }
    When method post
    Then status 400
    And match response.errorCode == 'VALIDATION_ERROR'

  Scenario: Informar cuando un solicitante no existe
    Given url baseUrl
    And path 'api', 'v1', 'solicitantes', 'documento', '987654321999999'
    When method get
    Then status 404
    And match response.errorCode == 'SOLICITANTE_NO_ENCONTRADO'
