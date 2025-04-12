package xshyo.us.shopMaster.services.records;
import xshyo.us.shopMaster.enums.SellStatus;
import xshyo.us.shopMaster.shop.data.ShopItem;

public record SellResult(SellStatus status, double price, String currency, ShopItem shopItem) {
}
