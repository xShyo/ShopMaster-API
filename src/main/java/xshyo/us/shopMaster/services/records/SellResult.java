package xshyo.us.shopMaster.services.records;
import xshyo.us.shopMaster.enums.SellStatus;

public record SellResult(SellStatus status, double price, String currency) {
}
