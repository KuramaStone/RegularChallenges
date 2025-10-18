# RegularChallenges

RegularChallenges is a flexible challenge system for Minecraft servers. It allows you to define rotating **challenge boards**, where each board contains randomly selected **challenges** drawn from configurable **pools**. This makes it ideal for daily, weekly, or event-based objectives.

---

## File Structure

All configuration files are stored inside the plugin’s `config` or `challenges` directory.

```
/config/
└─ RegularChallenges/
    ├─ boards/                # Put all Boards in this directory as a .yml file
    │   └─ daily.yml          # Example challenge board
    ├─ config.yml             # Configure the Boards gui and other values
    └─ pools.yml              # Defines all challenge pools and templates
```

---

## How It Works

### 1. Boards

A **board** is what players see in-game — e.g., a “Daily Challenges” board.

Each board:
- Defines a GUI layout.
- Chooses one or more challenge slots.
- Pulls random challenges from **Pools**.
- Resets on a defined interval.

When a board resets, it re-rolls random challenges based on the pools and weights you define. Players receive their rewards when the board resets.

Example:
```yaml
id: daily
display-name: <yellow>Daily</yellow>

# The order of challenges here will be in the same order as they are listed
gui:
  window-name: "Daily Challenges"
  structure:
    - "# # # # # # # # #"
    - "# # . # . # . # #"
    - "# # # # # # # # #"
  ingredients:
    '#':
      type: minecraft:black_stained_glass_pane
      custom-model-data: 0
      display-name: "<gradient:#ff3333:#ff6e00>Regular Challenges"

# How often this board resets
reset-interval: 24h
# The start time of the interval. FORMAT MUST BE "YYYY-DD-MM HH:mm:ss EST"
interval-start: "2025-01-01 18:00:00 America/New_York"

# Insert the challenges for this board. The order of pools here will determine their order in the gui.
pool-per-challenge:
  pool_id_0: # Pool Entry Id
    weight: 1.0
    pool_entries:
      - id: capture_pokemon_example
        weight: 1
  pool_id_1: # Pool Entry Id
    weight: 1.0
    pool_entries:
      - id: break_block_example
        weight: 1
  pool_id_2: # Pool Entry Id
    weight: 1.0
    pool_entries:
      - id: evolve_pokemon_example
        weight: 1
```

---

### 2. Pools

A **pool** groups one or more challenge templates.  
When a board slot points to a pool, the system picks one random template from that pool using weights.

Example (`pools.yml`) with only one challenge template:
```yaml
pools:
  capture_pokemon_example:
    very_specific_grass_pokemon:
      weight: 1
      type: capture_pokemon
      properties: "hasType=fire minLevel=10 timeOfDay=0/12000 inBiome=minecraft:plains pokeball=cobblemon:master_ball"
      amount: 30
      rewards:
        - give {player} diamond
      menu:
        board_view:
          type: cobblemon:poke_ball
          hide-tooltip: true
          display-name: "<yellow>Capture {amount} Pokémon!"
          lore:
            - "<gold>Capture {amount} level 10+ fire Pokémon during the day in a plains biome using a master ball."
```

---

## Challenge Types

These define what **type** of challenge you can configure.  
Each type has its own properties and triggers.
The API makes registering custom challenges very easy for developers.

| Type               | Trigger Description                      |
|--------------------|------------------------------------------|
| `break_block`      | Player breaks a block.                   |
| `capture_pokemon`  | Player captures a Pokémon.               |
| `evolve_pokemon`   | Player evolves a Pokémon.                |
| `exp_gain`         | Player gains Pokémon experience.         |
| `harvest_apricorn` | Player harvests an apricorn.             |
| `harvest_berry`    | Player harvests a berry.                 |
| `hatch_egg`        | Player hatches an egg.                   |
| `kill_entity`      | Player kills a specific entity.          |
| `level_up`         | Player levels up a Pokémon.              |
| `place_block`      | Player places a block.                   |
| `playtime`         | Player reaches a set amount of playtime. |
| `trade`            | Player completes a trade.                |
| `use_on_pokemon`   | Player uses an item on a Pokémon.        |
| `win_battle`       | Player wins a Pokémon battle.            |

You can find examples for each inside the `pools.yml`.

---

### Custom Challenges
Developers can add custom challenges easily by registering it it via `ChallengeRegister`.
`ChallengeRegister#POST_SETUP_REGISTER_EVENT` is called after the built-in challenges are registered through `ChallengeRegister#register`.
You can use this event to register your own challenges at the same time. `TemplateChallenge.java` is an example challenge provided.

#### Saving/Loading
Each challenge can theoretically save or load in any way it wants as it reads/writes the challenge as a String.
I use `Gson` to convert the entire challenge instance into a JSON that is easily loaded. I recommend keeping to this format, but it's not requried.

---

## Rewards

Each challenge template includes a `rewards:` section.  
You can execute commands using `{player}` as a placeholder. These rewards are given to a player when a challenge board resets.
If the player is offline, then they the command will trigger when the player comes online.

Example:
```yaml
rewards:
  - give {player} diamond 3
  - eco give {player} 500
```

---

## GUI Customization

Under each challenge’s `menu:` → `board_view:`, you can set:
- `type`: icon material or item ID. Supports modded items.
- `custom-model-data`: Optional for custom models
- `display-name`: Optional for a custom display name
- `lore`: List of lore lines

---

## Board Resets

Boards regenerate challenges automatically based on:
- `reset-interval`: e.g. `24h`, `7d`, `1h`
- `interval-start`: base time reference (`YYYY-DD-MM HH:mm:ss Timezone`)

When the next reset is due, a new set of random challenges is rolled.

---

## Tips

- Always keep your `pools.yml` well-structured. Indentation matters.
- Use weights to control rarity (`weight: 1` is common; higher = more likely).
- You can reuse pools across multiple boards.
- For debugging, temporarily set a short reset interval (e.g. `10m`).

---

## Example Setup Flow

1. Edit `pools.yml` to define challenge templates. Changes to templates also changes existing challenges.
2. Create a board YAML in `/boards/`.
3. Link each GUI slot (`gui_key`) to a pool via `pool-per-challenge`.
4. Reload the config via `/rcadmin reload`
5. Players can view and complete challenges in the GUI.