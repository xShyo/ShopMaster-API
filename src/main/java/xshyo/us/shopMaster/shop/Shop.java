package xshyo.us.shopMaster.shop;

import xshyo.us.shopMaster.shop.data.ShopItem;

public class Shop {



    /**
     * Busca un ShopItem por su slot
     *
     * @param slot El slot a buscar
     * @return El ShopItem encontrado o null si no existe
     */
    public ShopItem getItemBySlot(int slot) {
        return null;
    }

    /**
     * Verifica si hay un ítem en el slot especificado
     *
     * @param slot El slot a verificar
     * @return true si hay un ítem, false si no
     */
    public boolean hasItemInSlot(int slot) {
        return false;
    }

    public boolean saveChanges() {
        return false;
    }


    // Método para actualizar un ítem en el YAML
    public void updateItem(int itemId, ShopItem item) {

    }


}