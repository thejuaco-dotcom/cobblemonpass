# Cobblemonpass 🎟️

**Cobblemonpass** es un mod de Pase de Batalla premium y completamente personalizable diseñado para servidores de **Cobblemon** bajo la plataforma **Fabric (Minecraft 1.21.1)**. El mod ofrece una interfaz gráfica moderna inspirada en la estética clásica y limpia de Pokémon, permitiendo a los administradores gestionar temporadas, misiones y recompensas directamente dentro del juego.

---

## ✨ Características Principales

* **Interfaz de Usuario Estilo Pokémon**: Una GUI interactiva y elegante con barra de experiencia dinámica, línea de tiempo de niveles y tarjetas detalladas de premios.
* **Doble Nivel de Recompensa (Gratis y Premium)**: Sistema de recompensas diferenciadas para usuarios free-to-play y poseedores del pase Premium.
* **Panel de Administración In-Game**: Interfaz visual intuitiva para crear, editar o eliminar misiones y recompensas de niveles en tiempo real, sin necesidad de reiniciar el servidor.
* **Soporte Inteligente de NBT y Data Components**:
  * Diseñado específicamente para Minecraft 1.21.1+ (Data Components).
  * Permite asignar libros encantados, armas personalizadas, pociones o ítems con propiedades NBT específicas en las recompensas.
* **Tooltips de Ítems en Tiempo Real**: Al pasar el cursor sobre las recompensas de la GUI, se muestra el tooltip flotante nativo de Minecraft con los encantamientos, nombres y descripciones correspondientes.
* **Guardado Atómico de Progreso**: Sistema avanzado de escritura segura utilizando archivos temporales (`.tmp`) y reemplazo atómico (`ATOMIC_MOVE`), evitando la corrupción de datos y pérdida de progreso de los jugadores ante apagados inesperados o caídas del servidor.
* **Gestión de Temporadas Automatizada**: Comandos para iniciar, pausar y reiniciar temporadas enteras limpiando el progreso de los jugadores de forma ordenada.

---

## 🎮 Comandos y Sintaxis

El comando principal es `/cobblemonpass`. Se han registrado los alias `/cobblepass` y `/battlepass` que redirigen de forma transparente al comando principal.

### Comandos de Jugador (Nivel de Permiso 0)
* `/cobblemonpass` o `/cobblemonpass open` o `/cobblemonpass gui`: Abre la interfaz gráfica del Pase de Batalla para el jugador.

### Comandos de Administrador (Nivel de Permiso 2 / OP)
* `/cobblemonpass reload`: Recarga todas las configuraciones, misiones y recompensas de los archivos JSON de configuración.
* `/cobblemonpass addxp <jugador> <cantidad>`: Añade experiencia al pase del jugador objetivo.
* `/cobblemonpass setpremium <jugador> <true/false>`: Otorga o revoca el Pase Premium al jugador seleccionado.
* `/cobblemonpass season start`: Inicia la temporada activa.
* `/cobblemonpass season stop`: Pausa la temporada activa.
* `/cobblemonpass season create <duracion_dias>`: Inicia una nueva temporada con la duración indicada en días, reiniciando todo el progreso de party/PC de los jugadores.
* `/cobblemonpass testcraft <jugador> <item_id> <cantidad>`: Simulación de crafteo para probar el progreso de misiones de tipo crafteo.

---

## 📂 Archivos de Configuración

Todos los archivos se generan y almacenan en la carpeta `config/cobblepass/`:

* **`config.json`**: Ajustes generales de temporada y costo del pase premium.
* **`quests.json`**: Lista de misiones activas (Diarias, Semanales, Temporada) editables desde el panel in-game.
* **`rewards.json`**: Base de datos de recompensas del pase por nivel.
* **`players/`**: Carpeta que almacena los archivos `<UUID>.json` de cada jugador con su nivel, experiencia y recompensas reclamadas.

---

## 🔧 Ejemplo de Configuración de Recompensa (NBT / Components)

Para entregar un libro encantado con **Toque de Seda (Silk Touch)**, la recompensa se define de la siguiente manera en `rewards.json`:

```json
{
  "level": 15,
  "xpRequired": 100,
  "freeReward": null,
  "premiumReward": {
    "type": "ITEM",
    "value": "minecraft:enchanted_book",
    "amount": 1,
    "nbt": "stored_enchantments:{levels:{\"minecraft:silk_touch\":1}}"
  }
}
```

* **`type`**: Puede ser `ITEM`, `POKEMON` o `COMMAND`.
* **`value`**: ID base del ítem o Pokémon (ej. `minecraft:diamond` o `abra shiny level=25`).
* **`nbt`**: El bloque de componentes en formato de texto. El mod lo procesará y convertirá dinámicamente en Data Components nativos para Minecraft 1.21.1.
