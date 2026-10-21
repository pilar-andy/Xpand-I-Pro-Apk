package com.xpandpro.app.shell;

interface IShellService {
    // Transacción reservada por Shizuku para cerrar el servicio.
    void destroy() = 16777114;

    // Ejecuta un comando permitido y devuelve "<codigo>\n<salida>".
    String exec(String command) = 1;
}
