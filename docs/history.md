# Cómo evolucionó el proyecto

*[Read in English](history.en.md)*

El objetivo nunca cambió: un reproductor de música barato y siempre listo para el escritorio, que solo sirva para
escuchar música, hecho con un móvil que ya estaba en un cajón. Llevaba años en Android 8.1, sin actualizaciones de seguridad desde febrero de 2019, y su
batería estaba gastada por los años y el uso, así que ya no era un teléfono seguro ni práctico para el día a día. Como
aparato de escritorio sin cuentas, que solo reproduce música y está enchufado, ninguna de las dos cosas es un problema real. Coste total: 0 €. El camino sí cambió varias veces.

## 1. Elegir el móvil

El primer candidato fue un **Samsung Galaxy S3**. Se quedó en Android 4.3 y Spotify pide Android 7.0, así que habría
necesitado una ROM no oficial: LineageOS 14.1 ya no tiene versiones oficiales, y LineageOS 20/21 son betas lentas con
su 1 GB de RAM.

Un **Motorola Moto G5 Plus** (XT1685) cumple el mínimo de Spotify con su Android 8.1 de fábrica, es de 64 bits y tiene
más RAM. También se descartó ponerle LineageOS: solo hay versiones no oficiales abandonadas desde 2021, no mejoran la
seguridad respecto al sistema de fábrica y desbloquear el bootloader borra el móvil. Así que se quedó con su sistema de
fábrica, bloqueado y sin root.

## 2. Dejarlo limpio

Con la depuración USB y `adb`:

- **63 apps quitadas** para el usuario actual (`pm uninstall --user 0`), algo que un restablecimiento de fábrica
  deshace: apps de Google y de Motorola, teléfono, contactos, cámara, telemetría, restos de operadoras…
- **Conservadas a propósito:** Chrome (es el WebView en Android 8.1), los servicios de Google Play, el teclado, Ajustes,
  Bluetooth, el ecualizador y la **Radio FM**.
- Spotify instalado desde **Aurora Store** en modo anónimo, con la firma de F-Droid comprobada; el móvil no tiene cuenta
  de Google.
- **Tempus**, cliente Subsonic de código abierto, para la música de un servidor en casa (Substreamer pide Android 10).

## 3. Primera versión: una app de kiosco de código abierto

**FreeKiosk** (código abierto, con el SHA-256 comprobado frente a la versión publicada) se activó como device owner y se
configuró por `adb` para permitir solo Spotify, Tempus y la radio. El reloj del salvapantallas era una página HTML cargada
como dirección `data:`, escrita en sus ajustes de 150 en 150 caracteres con `adb shell input text`, porque su vista web no
podía leer archivos del móvil.

Funcionaba, con problemas conocidos:

- Tras cada arranque salía una y otra vez el aviso "Esta aplicación no se puede dejar de fijar" (su pantalla de carga
  chocaba con el bloqueo en Android 8.1).
- Con su opción de "launcher por defecto" activada, al arrancar se abrían dos a la vez, chocaban y el móvil quedaba
  **sin bloqueo** en 2 de 4 arranques de prueba.
- Cambiar el reloj obligaba a volver a escribir la página entera.

## 4. Una app propia

Sustituir FreeKiosk por una app pequeña en Kotlin resolvió los tres de golpe: al ser ella misma el launcher y el device
owner, no hay pantalla de carga que choque con el bloqueo ni nada que compita al arrancar.

Después fueron llegando funciones, cada una probada en el móvil:

1. **Reloj que también funciona con batería.** El `DreamService` de Android solo funciona cargando en este móvil, así que
   la app vigila los toques con una ventana de 1×1 y muestra su propia ventana de reloj.
2. **Qué está sonando** a partir de la sesión multimedia, con controles que funcionan.
3. **Modo noche** en rojo tenue.
4. **Volver a bloquearse tras actualizar**, descubierto cuando un `adb install -r` dejó el móvil en el launcher de fábrica.
5. **Icono de ajustes en lugar de PIN**, porque el móvil nunca sale de casa.
6. **Radio FM:** nombre de la emisora por RDS, el modo "solo favoritas" de la propia radio para anterior/siguiente, y
   logos de las emisoras buscados por frecuencia.
7. **Menú de apagado y gestos** de brillo y volumen como en un reproductor de vídeo.

## 5. De mi móvil a cualquier móvil antiguo

Con el reproductor ya funcionando, el siguiente paso fue que otra persona pudiera montarlo en su propio móvil antiguo, sin
saber nada de `adb`:

1. **Música y podcasts.** Se añadieron Auxio (MP3 del móvil o de una tarjeta microSD) y AntennaPod, los dos de F-Droid.
2. **Apps elegibles.** Los botones dejaron de estar fijos en el código: hasta 5 apps, con su orden, su icono y el nombre
   que se quiera (Tempus aparece como «Servidor»). Si nunca se han configurado, solo salen las apps por defecto que estén
   instaladas, porque no todos los móviles tienen radio FM.
3. **Instalar y desinstalar sin salir del kiosco.** Las pruebas destaparon que el bloqueo impedía también la confirmación
   de instalación de Android, así que no se podía instalar nada. Ahora la app permite siempre ese instalador y la tienda
   elegida (Aurora Store, F-Droid o Droid-ify; Google Play no, coherente con no usar cuenta de Google), y desinstala como
   device owner.
4. **Configuración inicial sin ordenador.** Cada permiso que antes se daba con `adb` tiene un paso con su estado y un
   botón que abre la pantalla de Android correspondiente. La primera versión soltaba el bloqueo para abrir esas pantallas
   y, en Android 8.1, el móvil se quedó en una pantalla negra. El rediseño no suelta nunca el bloqueo: permite solo la
   pantalla necesaria hasta que se vuelve a la app.
5. **Logos de emisoras desde el móvil**, con el selector de imágenes de Android y la frecuencia escrita con coma o punto.

## 6. Lo que queda abierto

- Activar el modo kiosco sigue exigiendo un ordenador una vez (`adb shell dpm set-device-owner`). El siguiente paso es
  hacerlo con un código QR tras restablecer de fábrica, el método de Android para empresas, que necesita una versión
  publicada a la que apunte el QR.

- Por la batería gastada, la idea es tenerlo enchufado con un cable con interruptor, para que no esté cargando todo el día
  (sin root no se puede limitar la carga). No se ha medido cuánto dura desenchufado; si la tapa empieza a levantarse, la
  batería se está hinchando y hay que cambiarla.
- El móvil no recibe actualizaciones de seguridad (último parche: febrero de 2019). No tiene cuentas y solo usa apps de
  música, pero es el motivo por el que, si Spotify sube su versión mínima, la alternativa más barata es otro Android de
  segunda mano.
