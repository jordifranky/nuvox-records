# Nuvox Records – corrección del proyecto

## Cambios
- `DataInitializer.java` no depende de Lombok y conserva la lógica de creación de usuarios y demo inicial.
- La carga de datos de ejemplo está **desactivada por defecto** para que Render no cree cuentas con contraseñas publicadas.
- Las pruebas usan exclusivamente `application-test.properties` y su base `nuvox_records_test` con datos de ejemplo.
- Se conservan los controladores, entidades, repositorios, frontend HTML, Dockerfile y Maven.
- Se ha sustituido la antigua contraseña de PostgreSQL local compartida en el repositorio por `CAMBIAR_CONTRASENA_LOCAL`: antes de ejecutar localmente, reemplázala por la contraseña real **solo en tu equipo**, sin subirla a GitHub. Rota la antigua contraseña si fue publicada.

## Abrir y ejecutar localmente
1. Extrae el ZIP en una carpeta nueva, abre el `pom.xml` de la raíz como proyecto Maven en IntelliJ.
2. Edita `src/main/resources/application.properties` exclusivamente en tu computadora y completa la contraseña PostgreSQL local.
3. En el panel Maven, `Reload All Maven Projects` y luego `Build > Rebuild Project`.
4. Ejecuta `NuvoxRecordsBackendApplication` con el botón verde ▶. Comprueba `HikariPool-1 - Start completed`.

## Crear el primer usuario del comité de forma segura en Render / Neon
- Si ya existe un usuario de comité en la BD, no hace falta inicializar otro.
- Si Neon está vacío, configura temporalmente en Render:
  - `NUVOX_SEED_ENABLED=true`
  - `NUVOX_SEED_CURATOR_PASSWORD=<contraseña nueva y fuerte>`
- Despliega **una sola vez** para crear el usuario `curator@nuvoxrecords.com` con BCrypt. Después elimina ambas variables y vuelve a desplegar.
- No expongas las contraseñas en GitHub, capturas ni logs.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` deben apuntar a Neon en Render. Para crear tablas iniciales `SPRING_JPA_HIBERNATE_DDL_AUTO=update`, cambiar después a `validate` una vez comprobado el esquema. Evita `create-drop` en producción.

## Pruebas
Los tests de integración requieren PostgreSQL y una BD de pruebas independiente (`nuvox_records_test`), creada previamente. Usa `./mvnw test` desde Maven cuando esa BD esté disponible. NO apuntes los tests a desarrollo o Neon de producción.

## Verificación
Los cuatro paquetes Java (`entity`, `repository`, `controller`, `security`) están presentes en el ZIP. No se ejecutó un arranque completo contra PostgreSQL desde este entorno.
