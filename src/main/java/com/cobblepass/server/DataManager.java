package com.cobblepass.server;

import com.cobblepass.common.PlayerProgress;
import com.cobblepass.common.Quest;
import com.cobblepass.common.Reward;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("cobblepass");
    private static final Path PLAYERS_DIR = CONFIG_DIR.resolve("players");
    private static final Path QUESTS_FILE = CONFIG_DIR.resolve("quests.json");
    private static final Path REWARDS_FILE = CONFIG_DIR.resolve("rewards.json");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    public static class Config {
        public String currencyType = "ITEM"; // "ITEM" or "SCOREBOARD" or "ADMIN_ONLY"
        public String currencyTarget = "minecraft:diamond"; // Objective name or Item ID
        public int premiumCost = 32; // Cost
        public int seasonDurationDays = 60;
        public long seasonStartTime = 0;
        public long seasonEndTime = 0;
        public boolean seasonActive = false;
    }
    public static Config config = new Config();

    private static final Map<UUID, PlayerProgress> progressCache = new HashMap<>();
    private static List<Quest> quests = new ArrayList<>();
    private static List<Reward> rewards = new ArrayList<>();

    private static final String[] EVOLUTIONARY_POOL = {
        "cobblemon:fire_stone", "cobblemon:water_stone", "cobblemon:thunder_stone", "cobblemon:leaf_stone",
        "cobblemon:moon_stone", "cobblemon:sun_stone", "cobblemon:shiny_stone", "cobblemon:dusk_stone",
        "cobblemon:dawn_stone", "cobblemon:ice_stone", "cobblemon:oval_stone", "cobblemon:everstone",
        "cobblemon:kings_rock", "cobblemon:metal_coat", "cobblemon:dragon_scale", "cobblemon:up_grade",
        "cobblemon:dubious_disc", "cobblemon:prism_scale", "cobblemon:deep_sea_tooth", "cobblemon:deep_sea_scale",
        "cobblemon:protector", "cobblemon:electirizer", "cobblemon:magmarizer", "cobblemon:reaper_cloth",
        "cobblemon:whipped_dream", "cobblemon:sachet", "cobblemon:sweet_apple", "cobblemon:tart_apple",
        "cobblemon:galarica_cuff", "cobblemon:galarica_wreath", "cobblemon:black_augurite", "cobblemon:peat_block",
        "cobblemon:linking_cord", "cobblemon:razor_claw", "cobblemon:razor_fang", "cobblemon:auspicious_armor",
        "cobblemon:malicious_armor", "cobblemon:chipped_pot", "cobblemon:cracked_pot", "cobblemon:scroll_of_darkness",
        "cobblemon:scroll_of_waters", "cobblemon:masterpiece_teacup", "cobblemon:unremarkable_teacup",
        "cobblemon:metal_alloy", "cobblemon:syrupy_apple", "cobblemon:strawberry_sweet", "cobblemon:love_sweet",
        "cobblemon:berry_sweet", "cobblemon:clover_sweet", "cobblemon:flower_sweet", "cobblemon:star_sweet",
        "cobblemon:ribbon_sweet"
    };

    private static final String[] COMPETITIVE_POOL = {
        "cobblemon:choice_band", "cobblemon:choice_specs", "cobblemon:choice_scarf", "cobblemon:life_orb",
        "cobblemon:leftovers", "cobblemon:focus_sash", "cobblemon:assault_vest", "cobblemon:lucky_egg",
        "cobblemon:exp_share", "cobblemon:ability_patch", "cobblemon:ability_capsule", "cobblemon:weakness_policy",
        "cobblemon:rocky_helmet", "cobblemon:eviolite", "cobblemon:heavy_duty_boots", "cobblemon:black_sludge",
        "cobblemon:expert_belt", "cobblemon:flame_orb", "cobblemon:toxic_orb", "cobblemon:damp_rock",
        "cobblemon:heat_rock", "cobblemon:smooth_rock", "cobblemon:icy_rock", "cobblemon:light_clay",
        "cobblemon:white_herb", "cobblemon:mental_herb", "cobblemon:power_herb", "cobblemon:scope_lens",
        "cobblemon:muscle_band", "cobblemon:wise_glasses", "cobblemon:wide_lens", "cobblemon:zoom_lens",
        "cobblemon:air_balloon", "cobblemon:red_card", "cobblemon:eject_button", "cobblemon:eject_pack",
        "cobblemon:safety_goggles", "cobblemon:protective_pads", "cobblemon:covert_cloak", "cobblemon:loaded_dice",
        "cobblemon:clear_amulet", "cobblemon:punching_glove", "cobblemon:ability_shield", "cobblemon:mirror_herb",
        "cobblemon:focus_band", "cobblemon:quick_claw", "cobblemon:bright_powder", "cobblemon:shell_bell",
        "cobblemon:metronome", "cobblemon:binding_band", "cobblemon:grip_claw", "cobblemon:shed_shell",
        "cobblemon:big_root", "cobblemon:terrain_extender", "cobblemon:utility_umbrella", "cobblemon:throat_spray",
        "cobblemon:blunder_policy", "cobblemon:adrenaline_orb", "cobblemon:room_service", "cobblemon:sticky_barb",
        "cobblemon:lagging_tail", "cobblemon:iron_ball", "cobblemon:ring_target", "cobblemon:absorb_bulb",
        "cobblemon:cell_battery", "cobblemon:snowball", "cobblemon:luminous_moss", "cobblemon:destiny_knot",
        "cobblemon:magnet", "cobblemon:charcoal_stick", "cobblemon:mystic_water", "cobblemon:miracle_seed",
        "cobblemon:never_melt_ice", "cobblemon:sharp_beak", "cobblemon:poison_barb", "cobblemon:soft_sand",
        "cobblemon:hard_stone", "cobblemon:spell_tag", "cobblemon:dragon_fang", "cobblemon:silk_scarf",
        "cobblemon:twisted_spoon", "cobblemon:silver_powder", "cobblemon:black_glasses", "cobblemon:black_belt"
    };

    private static final String[] COMMON_FREE_POOL = {
        "cobblemon:poke_ball", "cobblemon:great_ball", "cobblemon:ultra_ball", "cobblemon:quick_ball",
        "cobblemon:dusk_ball", "cobblemon:timer_ball", "cobblemon:repeat_ball", "cobblemon:net_ball",
        "cobblemon:dive_ball", "cobblemon:luxury_ball", "cobblemon:premier_ball",
        "minecraft:diamond", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:emerald",
        "minecraft:copper_ingot", "minecraft:amethyst_shard",
        "cobblemon:oran_berry", "cobblemon:pecha_berry", "cobblemon:leppa_berry", "cobblemon:sitrus_berry",
        "cobblemon:lum_berry",
        "cobblemon:exp_candy_xs", "cobblemon:exp_candy_s", "cobblemon:exp_candy_m"
    };

    private static final String[] COMMON_PREMIUM_POOL = {
        "cobblemon:exp_candy_l", "cobblemon:exp_candy_xl", "cobblemon:rare_candy",
        "cobblemon:hp_up", "cobblemon:protein", "cobblemon:iron", "cobblemon:calcium",
        "cobblemon:zinc", "cobblemon:carbos", "cobblemon:pp_up", "cobblemon:pp_max",
        "cobblemon:friend_ball", "cobblemon:fast_ball", "cobblemon:heavy_ball", "cobblemon:level_ball",
        "cobblemon:love_ball", "cobblemon:lure_ball", "cobblemon:moon_ball", "cobblemon:dream_ball",
        "cobblemon:beast_ball"
    };

    public static void initialize() {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(PLAYERS_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadConfig();
        loadQuests();
        loadRewards();
    }

    private static void loadConfig() {
        File file = CONFIG_FILE.toFile();
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(config, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileReader reader = new FileReader(file)) {
            config = GSON.fromJson(reader, Config.class);
            if (config == null) {
                config = new Config();
            }
        } catch (IOException e) {
            e.printStackTrace();
            config = new Config();
        }
    }

    public static void saveConfig() {
        File file = CONFIG_FILE.toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isSeasonActive() {
        if (!config.seasonActive) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (config.seasonStartTime > 0 && now < config.seasonStartTime) {
            return false;
        }
        if (config.seasonEndTime > 0 && now > config.seasonEndTime) {
            return false;
        }
        return true;
    }

    public static void startSeason() {
        config.seasonActive = true;
        config.seasonStartTime = System.currentTimeMillis();
        config.seasonEndTime = config.seasonStartTime + (config.seasonDurationDays * 24L * 60L * 60L * 1000L);
        saveConfig();
    }

    public static void stopSeason() {
        config.seasonActive = false;
        saveConfig();
    }

    public static void createSeason(MinecraftServer server, int durationDays) {
        config.seasonDurationDays = durationDays;
        config.seasonActive = true;
        config.seasonStartTime = System.currentTimeMillis();
        config.seasonEndTime = config.seasonStartTime + (durationDays * 24L * 60L * 60L * 1000L);
        saveConfig();

        // Clear cache and delete player files
        progressCache.clear();
        File playersDir = PLAYERS_DIR.toFile();
        if (playersDir.exists() && playersDir.isDirectory()) {
            File[] files = playersDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }

        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerProgress newProgress = new PlayerProgress(player.getUuid());
                progressCache.put(player.getUuid(), newProgress);
                saveProgress(newProgress);
                CobblePassServer.syncPlayerProgress(player);
            }
        }
    }

    public static void reloadConfigs(MinecraftServer server) {
        initialize();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                CobblePassServer.syncPlayerProgress(player);
            }
        }
    }

    private static void loadQuests() {
        File file = QUESTS_FILE.toFile();
        boolean forceRecreate = false;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                List<Quest> loaded = GSON.fromJson(reader, new TypeToken<List<Quest>>() {}.getType());
                if (loaded == null || loaded.size() < 20) {
                    forceRecreate = true;
                } else {
                    boolean hasOldQuest = loaded.stream().anyMatch(q -> q.getId().equals("craft_seasonal_great") || q.getId().equals("craft_seasonal_healing_machine") || (q.getId().equals("mine_weekly_gold") && q.getRequiredAmount() < 100) || q.getDescription().contains("pizarra") || q.getDescription().contains("detritos"));
                    if (hasOldQuest) {
                        forceRecreate = true;
                    }
                }
            } catch (Exception e) {
                forceRecreate = true;
            }
        } else {
            forceRecreate = true;
        }

        if (forceRecreate) {
            System.out.println("[CobblePass] Old or missing quests.json found. Regenerating Scenario B quests...");
            createDefaultQuests();
        }

        try (FileReader reader = new FileReader(file)) {
            quests = GSON.fromJson(reader, new TypeToken<List<Quest>>() {}.getType());
            if (quests == null) {
                quests = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            quests = new ArrayList<>();
        }
    }

    private static void createDefaultQuests() {
        List<Quest> defaults = new ArrayList<>();

        // --- 1. DAILY QUESTS (84 Quests) ---
        String[] types = {"fire", "water", "grass", "electric", "normal", "flying", "bug", "poison", "ground", "rock", "ghost", "steel", "psychic", "ice", "dragon", "dark", "fairy"};
        String[] typeNames = {"Fuego", "Agua", "Planta", "Eléctrico", "Normal", "Volador", "Bicho", "Veneno", "Tierra", "Roca", "Fantasma", "Acero", "Psíquico", "Hielo", "Dragón", "Siniestro", "Hada"};
        for (int i = 0; i < types.length; i++) {
            defaults.add(new Quest("cap_daily_" + types[i], "Cazador " + typeNames[i], "Captura 3 Pokémon de tipo " + typeNames[i], Quest.Type.CAPTURE_POKEMON, types[i], 3, 150, "DAILY"));
        }

        String[] commonSpecies = {"pikachu", "bulbasaur", "charmander", "squirtle", "pidgey", "rattata", "zubat", "diglett", "meowth", "psyduck", "growlithe", "poliwag", "abra", "machop", "geodude", "gastly", "eevee", "magikarp", "caterpie", "weedle"};
        String[] speciesNames = {"Pikachu", "Bulbasaur", "Charmander", "Squirtle", "Pidgey", "Rattata", "Zubat", "Diglett", "Meowth", "Psyduck", "Growlithe", "Poliwag", "Abra", "Machop", "Geodude", "Gastly", "Eevee", "Magikarp", "Caterpie", "Weedle"};
        for (int i = 0; i < commonSpecies.length; i++) {
            defaults.add(new Quest("cap_daily_" + commonSpecies[i], "Cazador de " + speciesNames[i], "Captura 1 " + speciesNames[i] + " salvaje", Quest.Type.CAPTURE_POKEMON, commonSpecies[i], 1, 150, "DAILY"));
        }

        for (int count = 5; count <= 25; count += 5) {
            defaults.add(new Quest("def_daily_" + count, "Entrenamiento Diario " + count, "Derrota " + count + " Pokémon salvajes", Quest.Type.DEFEAT_POKEMON, "any", count, 150, "DAILY"));
        }

        String[] ores = {"minecraft:coal_ore", "minecraft:iron_ore", "minecraft:copper_ore", "minecraft:gold_ore", "minecraft:redstone_ore", "minecraft:lapis_ore", "minecraft:diamond_ore", "minecraft:emerald_ore"};
        String[] oreNames = {"Carbón", "Hierro", "Cobre", "Oro", "Redstone", "Lapis", "Diamante", "Esmeralda"};
        int[] oreAmounts = {16, 12, 12, 8, 16, 12, 4, 2};
        for (int i = 0; i < ores.length; i++) {
            defaults.add(new Quest("mine_daily_" + oreNames[i].toLowerCase(), "Excavador de " + oreNames[i], "Pica " + oreAmounts[i] + " bloques de mineral de " + oreNames[i], Quest.Type.MINE_BLOCK, ores[i], oreAmounts[i], 150, "DAILY"));
            defaults.add(new Quest("mine_daily_deep_" + oreNames[i].toLowerCase(), "Excavador Profundo de " + oreNames[i], "Pica " + oreAmounts[i] + " bloques de " + oreNames[i] + " de Pizarra Profunda", Quest.Type.MINE_BLOCK, "minecraft:deepslate_" + ores[i].split(":")[1], oreAmounts[i], 150, "DAILY"));
        }

        defaults.add(new Quest("craft_daily_balls", "Artesano de Poké Balls", "Craftea 10 Poké Balls de cualquier tipo", Quest.Type.CRAFT_ITEM, "cobblemon:*_ball", 10, 150, "DAILY"));
        defaults.add(new Quest("craft_daily_iron", "Herrero de Hierro", "Craftea 8 lingotes de hierro", Quest.Type.CRAFT_ITEM, "minecraft:iron_ingot", 8, 150, "DAILY"));
        defaults.add(new Quest("craft_daily_gold", "Herrero de Oro", "Craftea 4 lingotes de oro", Quest.Type.CRAFT_ITEM, "minecraft:gold_ingot", 4, 150, "DAILY"));

        String[] extraSpecies = {"pidgeot", "sandshrew", "nidoran", "clefairy", "vulpix", "jigglypuff", "oddish", "paras", "venonat", "mankey", "arcanine", "tentacool", "ponyta", "slowpoke", "magnemite", "doduo", "grimer", "shellder", "onix", "drowzee", "krabby"};
        String[] extraSpeciesNames = {"Pidgeot", "Sandshrew", "Nidoran", "Clefairy", "Vulpix", "Jigglypuff", "Oddish", "Paras", "Venonat", "Mankey", "Arcanine", "Tentacool", "Ponyta", "Slowpoke", "Magnemite", "Doduo", "Grimer", "Shellder", "Onix", "Drowzee", "Krabby"};
        for (int i = 0; i < extraSpecies.length; i++) {
            defaults.add(new Quest("cap_daily_" + extraSpecies[i], "Capturar a " + extraSpeciesNames[i], "Captura 1 " + extraSpeciesNames[i] + " salvaje", Quest.Type.CAPTURE_POKEMON, extraSpecies[i], 1, 150, "DAILY"));
        }

        // --- 2. WEEKLY QUESTS (40 Quests - Scaled and Original) ---
        for (int i = 0; i < types.length; i++) {
            defaults.add(new Quest("cap_weekly_" + types[i], "Experto en Tipo " + typeNames[i], "Captura 75 Pokémon de tipo " + typeNames[i], Quest.Type.CAPTURE_POKEMON, types[i], 75, 600, "WEEKLY"));
        }
        for (int count : new int[]{200, 300, 400}) {
            defaults.add(new Quest("cap_weekly_any_" + count, "Gran Safari " + count, "Captura " + count + " Pokémon de cualquier tipo", Quest.Type.CAPTURE_POKEMON, "any", count, 600, "WEEKLY"));
        }
        for (int count : new int[]{250, 350, 500}) {
            defaults.add(new Quest("def_weekly_" + count, "Desafío de Combate " + count, "Derrota " + count + " Pokémon salvajes", Quest.Type.DEFEAT_POKEMON, "any", count, 600, "WEEKLY"));
        }
        
        int[] weeklyOreAmounts = {350, 300, 300, 150, 300, 200, 40, 15};
        for (int i = 0; i < ores.length; i++) {
            defaults.add(new Quest("mine_weekly_" + oreNames[i].toLowerCase(), "Gran Minería de " + oreNames[i], "Pica " + weeklyOreAmounts[i] + " bloques de mineral de " + oreNames[i], Quest.Type.MINE_BLOCK, ores[i], weeklyOreAmounts[i], 600, "WEEKLY"));
        }
        defaults.add(new Quest("craft_weekly_balls", "Maestro Manufacturero", "Craftea 300 Poké Balls de cualquier tipo", Quest.Type.CRAFT_ITEM, "cobblemon:*_ball", 300, 600, "WEEKLY"));
        defaults.add(new Quest("craft_weekly_iron", "Industrial de Hierro", "Craftea 250 lingotes de hierro", Quest.Type.CRAFT_ITEM, "minecraft:iron_ingot", 250, 600, "WEEKLY"));
        defaults.add(new Quest("craft_weekly_gold", "Industrial de Oro", "Craftea 150 lingotes de oro", Quest.Type.CRAFT_ITEM, "minecraft:gold_ingot", 150, 600, "WEEKLY"));

        defaults.add(new Quest("cap_weekly_eeveelutions", "Entrenador de Eevee", "Captura 15 Eevee", Quest.Type.CAPTURE_POKEMON, "eevee", 15, 600, "WEEKLY"));
        defaults.add(new Quest("cap_weekly_dragons", "Domadragones Semanal", "Captura 15 Pokémon de tipo Dragón", Quest.Type.CAPTURE_POKEMON, "dragon", 15, 600, "WEEKLY"));
        defaults.add(new Quest("cap_weekly_water", "Pescador Semanal", "Captura 100 Pokémon de tipo Agua", Quest.Type.CAPTURE_POKEMON, "water", 100, 600, "WEEKLY"));
        defaults.add(new Quest("mine_weekly_diamond", "Buscador de Diamantes", "Pica 40 bloques de mineral de Diamante", Quest.Type.MINE_BLOCK, "minecraft:diamond_ore", 40, 600, "WEEKLY"));
        defaults.add(new Quest("mine_weekly_gold", "Fiebre del Oro", "Pica 150 bloques de mineral de Oro", Quest.Type.MINE_BLOCK, "minecraft:gold_ore", 150, 600, "WEEKLY"));

        // Original weekly additions
        defaults.add(new Quest("cap_weekly_starters", "Cazador de Iniciales", "Captura 12 iniciales salvajes en su forma base (Bulbasaur, Charmander, Squirtle, etc.)", Quest.Type.CAPTURE_POKEMON, "regex:(bulbasaur|charmander|squirtle|chikorita|cyndaquil|totodile|treecko|torchic|mudkip|turtwig|chimchar|piplup|snivy|tepig|oshawott|chespin|fennekin|froakie|rowlet|litten|popplio|grookey|scorbunny|sobble|sprigatito|fuecoco|quaxly)", 12, 600, "WEEKLY"));
        defaults.add(new Quest("cap_weekly_steel", "Voluntad de Acero", "Captura 30 Pokémon de tipo Acero", Quest.Type.CAPTURE_POKEMON, "steel", 30, 600, "WEEKLY"));
        defaults.add(new Quest("cap_weekly_fossils_weekly", "Pequeño Paleontólogo", "Captura 8 Pokémon fósiles/prehistóricos", Quest.Type.CAPTURE_POKEMON, "regex:(omanyte|omastar|kabuto|kabutops|aerodactyl|lileep|cradily|anorith|armaldo|cranidos|rampardos|shieldon|bastiodon|tirtouga|carracosta|archen|archeops|tyrunt|tyrantrum|amaura|aurorus|dracozolt|arctozolt|dracovish|arctovish)", 8, 600, "WEEKLY"));
        defaults.add(new Quest("def_weekly_ghost_dark", "Pesadilla Nocturna", "Derrota 150 Pokémon de tipo Fantasma o Siniestro", Quest.Type.DEFEAT_POKEMON, "regex:(ghost|dark)", 150, 600, "WEEKLY"));
        defaults.add(new Quest("mine_weekly_amethyst", "Buscador de Amatista", "Pica 120 bloques de amatista en geodas", Quest.Type.MINE_BLOCK, "minecraft:amethyst_block", 120, 600, "WEEKLY"));
        defaults.add(new Quest("mine_weekly_blackstone", "Fiebre del Oro Negro", "Pica 350 bloques de piedra negra en el Nether", Quest.Type.MINE_BLOCK, "minecraft:blackstone", 350, 600, "WEEKLY"));
        defaults.add(new Quest("mine_weekly_obsidian", "Corazón de Lava", "Pica 64 bloques de obsidiana", Quest.Type.MINE_BLOCK, "minecraft:obsidian", 64, 600, "WEEKLY"));

        // --- 3. SEASONAL QUESTS (10 Quests - Original & Challenging) ---
        defaults.add(new Quest("cap_seasonal_all", "Safari Definitivo", "Captura 3000 Pokémon", Quest.Type.CAPTURE_POKEMON, "any", 3000, 2000, "SEASONAL"));
        defaults.add(new Quest("def_seasonal_wild", "Campeón de la Región", "Derrota 2500 Pokémon salvajes en combate", Quest.Type.DEFEAT_POKEMON, "any", 2500, 2000, "SEASONAL"));
        defaults.add(new Quest("cap_seasonal_fossils", "Coleccionista de Fósiles", "Captura 35 Pokémon prehistóricos/fósiles resucitados", Quest.Type.CAPTURE_POKEMON, "regex:(omanyte|omastar|kabuto|kabutops|aerodactyl|lileep|cradily|anorith|armaldo|cranidos|rampardos|shieldon|bastiodon|tirtouga|carracosta|archen|archeops|tyrunt|tyrantrum|amaura|aurorus|dracozolt|arctozolt|dracovish|arctovish)", 35, 2000, "SEASONAL"));
        defaults.add(new Quest("cap_seasonal_babies", "Guardería Pokémon", "Captura 40 Pokémon bebé salvajes (Pichu, Togepi, Riolu, Happiny, etc.)", Quest.Type.CAPTURE_POKEMON, "regex:(pichu|cleffa|igglybuff|togepi|tyrogue|smoochum|elekid|magby|happiny|munchlax|riolu|bonsly|budew|chingling|toxel|mime_jr)", 40, 2000, "SEASONAL"));
        defaults.add(new Quest("cap_seasonal_dragons_rare", "Furia Dragón", "Captura 120 Pokémon salvajes de tipo Dragón", Quest.Type.CAPTURE_POKEMON, "dragon", 120, 2000, "SEASONAL"));
        defaults.add(new Quest("def_seasonal_elements", "Frenesí Elemental", "Derrota 800 Pokémon de tipo Fuego, Agua o Planta", Quest.Type.DEFEAT_POKEMON, "regex:(fire|water|grass)", 800, 2000, "SEASONAL"));
        defaults.add(new Quest("mine_seasonal_ancient_debris", "Arqueólogo del Nether", "Pica 30 bloques de escombros ancestrales", Quest.Type.MINE_BLOCK, "minecraft:ancient_debris", 30, 2000, "SEASONAL"));
        defaults.add(new Quest("mine_seasonal_diamond_deep", "Fiebre del Diamante", "Pica 200 bloques de mineral de Diamante", Quest.Type.MINE_BLOCK, "regex:minecraft:(deepslate_)?diamond_ore", 200, 2000, "SEASONAL"));
        defaults.add(new Quest("mine_seasonal_gold_deep", "El Dorado Profundo", "Pica 500 bloques de mineral de Oro", Quest.Type.MINE_BLOCK, "regex:minecraft:(deepslate_)?gold_ore", 500, 2000, "SEASONAL"));
        defaults.add(new Quest("craft_seasonal_balls", "Fábrica Regional de Poké Balls", "Craftea 1500 Poké Balls de cualquier tipo", Quest.Type.CRAFT_ITEM, "cobblemon:*_ball", 1500, 2000, "SEASONAL"));

        try (FileWriter writer = new FileWriter(QUESTS_FILE.toFile())) {
            GSON.toJson(defaults, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadRewards() {
        File file = REWARDS_FILE.toFile();
        boolean forceRecreate = false;
        if (file.exists()) {
            try {
                String content = Files.readString(REWARDS_FILE);
                com.google.gson.JsonElement jsonElement = GSON.fromJson(content, com.google.gson.JsonElement.class);
                if (jsonElement == null) {
                    forceRecreate = true;
                } else if (jsonElement.isJsonArray()) {
                    List<Reward> loaded = GSON.fromJson(jsonElement, new TypeToken<List<Reward>>() {}.getType());
                    if (loaded == null || loaded.size() < 20) {
                        forceRecreate = true;
                    }
                } else if (jsonElement.isJsonObject()) {
                    com.google.gson.JsonArray tiers = jsonElement.getAsJsonObject().getAsJsonArray("tiers");
                    if (tiers == null || tiers.size() < 20) {
                        forceRecreate = true;
                    }
                } else {
                    forceRecreate = true;
                }
            } catch (Exception e) {
                forceRecreate = true;
            }
        } else {
            forceRecreate = true;
        }

        if (forceRecreate) {
            System.out.println("[CobblePass] Old or missing rewards.json found. Regenerating Scenario B rewards...");
            createDefaultRewards();
        }

        try {
            String content = Files.readString(REWARDS_FILE);
            com.google.gson.JsonElement jsonElement = GSON.fromJson(content, com.google.gson.JsonElement.class);
            Map<String, Reward.Action> templates = new HashMap<>();
            List<Reward> loadedTiers = new ArrayList<>();

            if (jsonElement.isJsonArray()) {
                loadedTiers = GSON.fromJson(jsonElement, new TypeToken<List<Reward>>() {}.getType());
            } else if (jsonElement.isJsonObject()) {
                com.google.gson.JsonObject obj = jsonElement.getAsJsonObject();
                if (obj.has("templates")) {
                    templates = GSON.fromJson(obj.get("templates"), new TypeToken<Map<String, Reward.Action>>() {}.getType());
                }
                if (obj.has("tiers")) {
                    loadedTiers = GSON.fromJson(obj.get("tiers"), new TypeToken<List<Reward>>() {}.getType());
                }
            }

            // Resolve templates
            for (Reward reward : loadedTiers) {
                resolveRewardActionTemplate(reward.getFreeReward(), templates);
                resolveRewardActionTemplate(reward.getPremiumReward(), templates);
            }

            rewards = loadedTiers;
            if (rewards == null) {
                rewards = new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            rewards = new ArrayList<>();
        }
    }

    private static void resolveRewardActionTemplate(Reward.Action action, Map<String, Reward.Action> templates) {
        if (action == null || action.getTemplateRef() == null || action.getTemplateRef().isEmpty()) {
            return;
        }
        Reward.Action template = templates.get(action.getTemplateRef());
        if (template != null) {
            action.setType(template.getType());
            action.setValue(template.getValue());
            action.setAmount(template.getAmount() > 0 ? template.getAmount() : (action.getAmount() > 0 ? action.getAmount() : 1));
            action.setNbt(template.getNbt());
        } else {
            System.err.println("[CobblePass] Warning: Template '" + action.getTemplateRef() + "' referenced but not defined!");
        }
    }

    private static void createDefaultRewards() {
        Random rand = new Random(2026);
        
        List<String> evoList = new ArrayList<>(Arrays.asList(EVOLUTIONARY_POOL));
        Collections.shuffle(evoList, rand);
        
        List<String> compList = new ArrayList<>(Arrays.asList(COMPETITIVE_POOL));
        Collections.shuffle(compList, rand);
        
        List<String> commonFreeList = new ArrayList<>(Arrays.asList(COMMON_FREE_POOL));
        List<String> commonPremList = new ArrayList<>(Arrays.asList(COMMON_PREMIUM_POOL));
        
        List<String[]> commonPairs = new ArrayList<>();
        for (String freeItem : commonFreeList) {
            for (String premItem : commonPremList) {
                commonPairs.add(new String[]{freeItem, premItem});
            }
        }
        Collections.shuffle(commonPairs, rand);
        
        int evoIdx = 0;
        int compIdx = 0;
        int pairIdx = 0;

        List<Reward> defaults = new ArrayList<>();
        for (int lvl = 1; lvl <= 100; lvl++) {
            int xpRequired = 100 + (lvl * 10);
            
            Reward.Action freeAction;
            Reward.Action premiumAction;
            
            if (lvl == 100) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "minecraft:elytra", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "mew shiny=true level=5", 1);
            } else if (lvl == 90) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "minecraft:shulker_box", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "beldum shiny=true level=20", 1);
            } else if (lvl == 80) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "minecraft:enchanted_golden_apple", 2);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "rotom shiny=true level=30", 1);
            } else if (lvl == 70) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "minecraft:netherite_ingot", 2);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "bagon level=20", 1);
            } else if (lvl == 60) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:master_ball", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "gengar shiny=true level=40", 1);
            } else if (lvl == 50) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "minecraft:totem_of_undying", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "larvitar level=15", 1);
            } else if (lvl == 40) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:exp_share", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "eevee shiny=true level=5", 1);
            } else if (lvl == 30) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:ability_capsule", 1);
                premiumAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:lucky_egg", 1);
            } else if (lvl == 20) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:choice_band", 1);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "gyarados shiny=true level=20", 1);
            } else if (lvl == 10) {
                freeAction = new Reward.Action(Reward.Type.ITEM, "cobblemon:rare_candy", 3);
                premiumAction = new Reward.Action(Reward.Type.POKEMON, "dratini level=10", 1);
            } else {
                int group = lvl % 3;
                if (group == 0) {
                    // Evolutionary Item (Free) + Competitive Item (Premium)
                    String freeItem = evoList.get(evoIdx++);
                    int freeQty = getQuantityForItem(freeItem, rand);
                    freeAction = new Reward.Action(Reward.Type.ITEM, freeItem, freeQty);
                    
                    String premItem = compList.get(compIdx++);
                    int premQty = getQuantityForItem(premItem, rand);
                    premiumAction = new Reward.Action(Reward.Type.ITEM, premItem, premQty);
                } else if (group == 1) {
                    // Unique Common Pair
                    String[] pair = commonPairs.get(pairIdx++);
                    String freeItem = pair[0];
                    int freeQty = getQuantityForItem(freeItem, rand);
                    freeAction = new Reward.Action(Reward.Type.ITEM, freeItem, freeQty);
                    
                    String premItem = pair[1];
                    int premQty = getQuantityForItem(premItem, rand);
                    premiumAction = new Reward.Action(Reward.Type.ITEM, premItem, premQty);
                } else {
                    // Common Free Item + Competitive Item (Premium)
                    String freeItem = commonFreeList.get(rand.nextInt(commonFreeList.size()));
                    int freeQty = getQuantityForItem(freeItem, rand);
                    freeAction = new Reward.Action(Reward.Type.ITEM, freeItem, freeQty);
                    
                    String premItem = compList.get(compIdx++);
                    int premQty = getQuantityForItem(premItem, rand);
                    premiumAction = new Reward.Action(Reward.Type.ITEM, premItem, premQty);
                }
            }
            
            defaults.add(new Reward(lvl, xpRequired, freeAction, premiumAction));
        }

        try (FileWriter writer = new FileWriter(REWARDS_FILE.toFile())) {
            GSON.toJson(defaults, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getQuantityForItem(String itemId, Random rand) {
        if (itemId.endsWith("poke_ball") || itemId.endsWith("_ball")) {
            return 3 + rand.nextInt(3); // 3, 4, 5
        } else if (itemId.endsWith("exp_candy_xs")) {
            return 5 + rand.nextInt(4); // 5, 6, 7, 8
        } else if (itemId.endsWith("exp_candy_s")) {
            return 3 + rand.nextInt(3); // 3, 4, 5
        } else if (itemId.endsWith("exp_candy_m")) {
            return 2 + rand.nextInt(2); // 2, 3
        } else if (itemId.endsWith("exp_candy_l")) {
            return 1 + rand.nextInt(2); // 1, 2
        } else if (itemId.endsWith("exp_candy_xl") || itemId.endsWith("rare_candy")) {
            return 1 + rand.nextInt(2); // 1, 2
        } else if (itemId.endsWith("diamond")) {
            return 1 + rand.nextInt(2); // 1, 2
        } else if (itemId.endsWith("iron_ingot")) {
            return 3 + rand.nextInt(3); // 3, 4, 5
        } else if (itemId.endsWith("gold_ingot")) {
            return 2 + rand.nextInt(2); // 2, 3
        } else if (itemId.endsWith("emerald")) {
            return 2 + rand.nextInt(3); // 2, 3, 4
        } else if (itemId.endsWith("copper_ingot")) {
            return 4 + rand.nextInt(5); // 4, 5, 6, 7, 8
        } else if (itemId.endsWith("amethyst_shard")) {
            return 2 + rand.nextInt(3); // 2, 3, 4
        } else if (itemId.endsWith("_berry")) {
            return 3 + rand.nextInt(3); // 3, 4, 5
        } else if (itemId.endsWith("hp_up") || itemId.endsWith("protein") || itemId.endsWith("iron") || 
                   itemId.endsWith("calcium") || itemId.endsWith("zinc") || itemId.endsWith("carbos") || 
                   itemId.endsWith("pp_up")) {
            return 1 + rand.nextInt(2); // 1, 2
        }
        return 1;
    }

    public static void saveQuestsJson(String json) {
        try {
            Files.writeString(QUESTS_FILE, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveRewardsJson(String json) {
        try {
            Files.writeString(REWARDS_FILE, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConfigJson(String json) {
        try {
            Files.writeString(CONFIG_FILE, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Quest> getQuests() {
        return quests;
    }

    public static List<Reward> getRewards() {
        return rewards;
    }

    public static PlayerProgress getProgress(UUID uuid) {
        PlayerProgress progress = progressCache.get(uuid);
        if (progress == null) {
            File file = PLAYERS_DIR.resolve(uuid.toString() + ".json").toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    progress = GSON.fromJson(reader, PlayerProgress.class);
                } catch (IOException e) {
                    e.printStackTrace();
                    progress = new PlayerProgress(uuid);
                }
            } else {
                progress = new PlayerProgress(uuid);
                saveProgress(progress);
            }
            progressCache.put(uuid, progress);
        }

        List<String> allDailyIds = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("DAILY")).map(Quest::getId).toList();
        List<String> allWeeklyIds = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("WEEKLY")).map(Quest::getId).toList();
        if (progress.checkAndResetQuests(allDailyIds, allWeeklyIds)) {
            saveProgress(progress);
        }
        return progress;
    }

    public static void saveProgress(PlayerProgress progress) {
        File file = PLAYERS_DIR.resolve(progress.getUuid().toString() + ".json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(progress, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAllCached() {
        for (PlayerProgress progress : progressCache.values()) {
            saveProgress(progress);
        }
    }

    public static void unloadPlayer(UUID uuid) {
        PlayerProgress progress = progressCache.remove(uuid);
        if (progress != null) {
            saveProgress(progress);
        }
    }

    public static void addXp(ServerPlayerEntity player, int xpAmount) {
        if (!isSeasonActive()) {
            return;
        }
        PlayerProgress progress = getProgress(player.getUuid());
        int currentXp = progress.getCurrentXp();
        int currentLvl = progress.getLevel();
        int maxLvl = rewards.size();

        if (currentLvl >= maxLvl) {
            // Already at max level
            return;
        }

        final int initialLvl = currentLvl;
        Reward nextLvlReward = rewards.stream()
                .filter(r -> r.getLevel() == (initialLvl + 1))
                .findFirst()
                .orElse(null);

        int xpNeeded = (nextLvlReward != null) ? nextLvlReward.getXpRequired() : 100;
        int nextXp = currentXp + xpAmount;

        while (nextXp >= xpNeeded && currentLvl < maxLvl) {
            nextXp -= xpNeeded;
            currentLvl++;
            player.sendMessage(Text.literal("§a§l¡Subiste de nivel en tu Pase de Batalla! Nivel " + currentLvl));

            // Fetch the required XP for the next level
            final int finalCurrentLvl = currentLvl;
            Reward subsequentLvlReward = rewards.stream()
                    .filter(r -> r.getLevel() == (finalCurrentLvl + 1))
                    .findFirst()
                    .orElse(null);
            xpNeeded = (subsequentLvlReward != null) ? subsequentLvlReward.getXpRequired() : 100;
        }

        progress.setCurrentXp(nextXp);
        progress.setLevel(currentLvl);
        saveProgress(progress);

        // Sync with client
        CobblePassServer.syncPlayerProgress(player);
    }

    public static List<Quest> getActiveQuestsForPlayer(ServerPlayerEntity player) {
        PlayerProgress progress = getProgress(player.getUuid());
        return getActiveQuestsForPlayer(progress);
    }

    public static List<Quest> getActiveQuestsForPlayer(PlayerProgress progress) {
        List<String> allDailyIds = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("DAILY")).map(Quest::getId).toList();
        List<String> allWeeklyIds = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("WEEKLY")).map(Quest::getId).toList();

        if (progress.checkAndResetQuests(allDailyIds, allWeeklyIds)) {
            saveProgress(progress);
        }

        List<Quest> active = new ArrayList<>();

        // 1. Dailies (4 active)
        List<Quest> dailies = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("DAILY")).toList();
        if (!dailies.isEmpty()) {
            long daySeed = java.time.LocalDate.now().toEpochDay();
            Random r = new Random(daySeed);
            List<Quest> shuffled = new ArrayList<>(dailies);
            Collections.shuffle(shuffled, r);
            for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
                active.add(shuffled.get(i));
            }
        }

        // 2. Weeklies (4 active)
        List<Quest> weeklies = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("WEEKLY")).toList();
        if (!weeklies.isEmpty()) {
            long weekSeed = java.time.LocalDate.now()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    .toEpochDay();
            Random r = new Random(weekSeed);
            List<Quest> shuffled = new ArrayList<>(weeklies);
            Collections.shuffle(shuffled, r);
            for (int i = 0; i < Math.min(4, shuffled.size()); i++) {
                active.add(shuffled.get(i));
            }
        }

        // 3. Seasonals (10 active)
        List<Quest> seasonals = quests.stream().filter(q -> q.getCategory().equalsIgnoreCase("SEASONAL")).toList();
        active.addAll(seasonals);

        return active;
    }

    public static String getActiveQuestsJsonForPlayer(ServerPlayerEntity player) {
        return GSON.toJson(getActiveQuestsForPlayer(player));
    }

    public static String getQuestsJson() {
        return GSON.toJson(quests);
    }

    public static String getRewardsJson() {
        return GSON.toJson(rewards);
    }

    public static String getProgressJson(UUID uuid) {
        return GSON.toJson(getProgress(uuid));
    }
}
