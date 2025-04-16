package xshyo.us.shopMaster;

import xshyo.us.shopMaster.enums.CurrencyType;
import xshyo.us.shopMaster.managers.CurrencyManager;
import xshyo.us.shopMaster.managers.ShopManager;
import xshyo.us.shopMaster.services.PurchaseService;
import xshyo.us.shopMaster.services.SellService;

import java.util.HashMap;


public final class ShopMaster {


    public ShopMaster() {

    }

    private static ShopMaster instance;
    private ShopManager shopManager;
    private SellService sellService;
    private PurchaseService purchaseService;
    private ItemComparator itemComparator;
    private final HashMap<CurrencyType, CurrencyManager> currencyMap = new HashMap<>();

    public static ShopMaster getInstance() {
        return instance;
    }


    public ShopManager getShopManager() {
        return shopManager;
    }

    public SellService getSellService() {
        return sellService;
    }

    public PurchaseService getPurchaseService() {
        return purchaseService;
    }

    public ItemComparator getItemComparator() {
        return itemComparator;
    }

    public HashMap<CurrencyType, CurrencyManager> getCurrencyMap() {
        return currencyMap;
    }



}
