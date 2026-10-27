---
navigation:
  title: "Feeding Card"
  icon: ae2feedingcard:feeding_card
  position: 99
categories:
- tools
item_ids:
- ae2feedingcard:feeding_card
---

# The Feeding Card

<ItemImage id="ae2feedingcard:feeding_card" scale="4" />

The feeding card is an [upgrade card](ae2:items-blocks-machines/upgrade_cards.md) for the [wireless terminal](ae2:items-blocks-machines/wireless_terminals.md).

It pulls food from [network storage](ae2:ae2-mechanics/import-export-storage.md) and feeds it to you.

## The UI

There are several sections of a feeding card's UI.

The center section has the filter slots that define the food list.

The left section has the following buttons:

*   Open the detailed settings window
*   Turn the feeding card on or off, if a <ItemLink id="ae2:redstone_card" /> is installed

On the right there are slots for [upgrade cards](ae2:items-blocks-machines/upgrade_cards.md).

## Filtering

Food inserted into the filter slots acts as a whitelist, only allowing those specific foods to be eaten. With an empty list, the card eats nothing.

If no food is filtered, it can be placed by left-clicking a slot with food in hand, or dragged into the slots from JEI/REI even if you don't actually have any of that item.

<a name="settings"></a>

## Settings

"Store Containers in ME" and "Containers to Inventory" are two independent toggles that decide where leftover containers go first. Containers that fit in neither drop at the player's feet.

"Play Eat Sound" only affects food eaten by the feeding card.

There are two "Primary Feeding Strategies":

*   "Avoid Waste First": ranks food by how much of the hunger gap it fills, then picks the least waste within a rank
*   "Prefer High Saturation": takes the food that restores the most saturation, then the least waste

When multiple candidate foods tie, the "Secondary Feeding Strategy" decides the order. There are three:

*   "Ignore Food Effects": effects do not participate in the ranking
*   "Prefer Food Without Effects": no effect, then beneficial, then neutral, then harmful
*   "Prefer Food with Beneficial Effects": beneficial, then neutral, then no effect, then harmful

There are two "Compound Effect Handlings":

*   "Any Harmful Effect Counts as Harmful"
*   "Offset Beneficial Against Harmful": one beneficial effect cancels one harmful effect, and the food still counts as harmful if harmful effects remain

## Overflow Destruction Card

"Void Containers if Full" decides what happens to containers that fit in neither "Store Containers in ME" nor "Containers to Inventory": they are deleted when it is enabled.

There are four "Behavior at Full Hunger" options:

*   "Stop Eating"
*   "Eat Below 15 Saturation"
*   "Eat Below 10 Saturation"
*   "Eat Below 5 Saturation"

With the last three, food without saturation is never eaten.

## Crafting Card

If a <ItemLink id="ae2:crafting_card" /> is installed, the feeding card sends crafting requests for the food it wants to the [autocrafting](ae2:ae2-mechanics/autocrafting.md) system.

There are two "Crafting Timing" options:

*   "Craft Only Without Stock"
*   "Craftables Join Comparison (Costly)"

There are two "While Crafting" options:

*   "Keep Eating Other Food"
*   "Wait for Crafting"

Each request asks for one item. Craftable items are scanned every 20 ticks by default, independent of the feeding cooldown. A failed plan or submission waits 100 ticks before retrying by default, and the card falls back to stock meanwhile.

## Inverter Card

With an <ItemLink id="ae2:inverter_card" />, the food list switches from a whitelist to a blacklist, only excluding those specific foods. If the list is empty, the card eats every food in the network.

There are three "Harmful Effect Handling" options:

*   "Keep Harmful Effects"
*   "Exclude Certainly Harmful"
*   "Exclude All Harmful"

## Mod Compatibility

With Spice of Life: Carrot Edition installed, there are three "Uneaten Food Handling" options:

*   "Leave Alone"
*   "Prefer Untried Food"
*   "Prefer Untried Food, Even if Harmful": untried food is not subject to "Harmful Effect Handling"

## Upgrades

The feeding card supports the following [upgrades](ae2:items-blocks-machines/upgrade_cards.md):

*   <ItemLink id="ae2:capacity_card" /> increases the amount of filter slots
*   <ItemLink id="ae2:fuzzy_card" /> lets the filter ignore item components
*   <ItemLink id="ae2:inverter_card" /> switches the filter from a whitelist to a blacklist
*   <ItemLink id="ae2:speed_card" /> reduces the cooldown time, multiplying the cooldown time by 0.5 per card
*   <ItemLink id="ae2:crafting_card" /> enables crafting
*   <ItemLink id="ae2:equal_distribution_card" /> rotates through food in a fixed order, and makes the primary strategy, secondary strategy, and compound effect handling unavailable
*   <ItemLink id="ae2:void_card" /> enables eating past full hunger and unlocks container deletion
*   <ItemLink id="ae2:redstone_card" /> adds a toggle button in the GUI to turn the feeding card on and off

## Speeds

| Acceleration Card Count | Cooldown               |
|:------------------------|:-----------------------|
| 0                       | 200 ticks (10 seconds) |
| 1                       | 100 ticks (5 seconds)  |
| 2                       | 50 ticks (2.5 seconds) |
| 3                       | 25 ticks (1.25 seconds)|
| 4                       | 12 ticks (0.6 seconds) |

## Recipe

<RecipeFor id="ae2feedingcard:feeding_card" />
