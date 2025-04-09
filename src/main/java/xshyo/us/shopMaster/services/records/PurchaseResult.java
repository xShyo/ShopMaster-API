package xshyo.us.shopMaster.services.records;

public record PurchaseResult(
        boolean success,
        double amount,
        String itemName,
        double price
) {}
