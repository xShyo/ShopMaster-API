# ShopMaster API

[![](https://jitpack.io/v/xShyo/ShopMaster-API.svg)](https://jitpack.io/#xShyo/ShopMaster-API)

Public API for the [ShopMaster](https://builtbybit.com/resources/shopmaster-advanced-shop-gui.64806/) plugin that allows developers to interact with the shop system to query prices, check item sellability, perform programmatic sales, and retrieve details of registered shop items.

---

## 📦 Installation via JitPack

### 1. Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Add the dependency:

```xml
<dependency>
    <groupId>com.github.xShyo</groupId>
    <artifactId>ShopMaster-API</artifactId>
    <version>Tag</version> <!-- Replace "Tag" with the GitHub version/tag -->
</dependency>
```

> 💡 You can also use **Gradle** – check [JitPack](https://jitpack.io/#xShyo/ShopMaster-API) for more formats.

---

## ✨ Features

- Calculate buy and sell prices.
- Check if an item is sellable, with or without player context.
- Retrieve detailed information of items registered in the shop.
- Execute individual or bulk sales from code.
- Send automatic sales summaries to the player.

---

## 🧪 Quick Usage

### Get the sell price:
```java
double price = ShopMasterAPI.getItemStackPriceSell(player, itemStack);
```

### Check if an item is sellable:
```java
boolean canSell = ShopMasterAPI.isSellablePlayer(player, itemStack);
```

### Sell all sellable items from the inventory:
```java
SellAllResult result = ShopMasterAPI.sellAllItems(player);
ShopMasterAPI.sendSaleSummary(player, result);
```

---

## 🧩 Highlighted Methods

| Method                        | Description                                             |
|------------------------------|---------------------------------------------------------|
| getItemStackPriceSell(...)   | Calculates the sell price of an `ItemStack`.            |
| getItemStackPriceBuy(...)    | Calculates the buy price of an `ItemStack`.             |
| isSellable(...)              | Checks if the item is registered in the shop.           |
| getShopItem(...)             | Returns the `ShopItem` corresponding to the item.       |
| sellItem(...)                | Sells a specific amount of an item.                     |
| sellAllItems(...)            | Sells all sellable items from the inventory.            |
| sendSaleSummary(...)         | Sends a sales summary to the player.                    |

---

## 📎 Requirements

- **ShopMaster** plugin installed on the server.
- Compatible with Spigot / Paper / Purpur.
- Java 17 or higher recommended.

