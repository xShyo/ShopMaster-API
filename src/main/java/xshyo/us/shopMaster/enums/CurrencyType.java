package xshyo.us.shopMaster.enums;


public enum CurrencyType {
    VAULT("money"),
    LEVEL("levels"),
    EXP_POINTS("xp-points"),
    PLAYER_POINTS("player-points"),
    TOKEN_MANAGER("token-manager"),
    BEAST_TOKENS("beast-tokens"),
    DISABLED("disabled");

    private final String configKey;

    CurrencyType(String paramString) {
        this.configKey = paramString;
    }

    public static CurrencyType getType(String paramString, CurrencyType paramCostType) {
        return null;
    }
}
