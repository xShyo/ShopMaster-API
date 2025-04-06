package xshyo.us.shopMaster.utilities;

import xshyo.us.shopMaster.ShopMaster;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidad combinada para manejar formateo de números y monedas en el plugin
 */
public class CurrencyFormatter {
    // Cache para los símbolos de monedas
    private static final Map<String, String> currencySymbolCache = new HashMap<>();

    // Formateador de números
    private static DecimalFormat decimalFormat;
    private static boolean hideFraction;

    // Configuración de escala corta
    private static boolean enableShortScale;
    private static long shortScaleLimit;
    private static int shortHandDecimalLimit;
    private static int shortHandNumberLimit;
    private static String[] suffixes;

    /**
     * Inicializa el formateador
     */
    public static void initialize() {
        refreshCache();
        reloadNumberFormat();
    }

    /**
     * Recarga toda la configuración
     */
    public static void reload() {
        refreshCache();
        reloadNumberFormat();
    }

    /**
     * Obtiene el símbolo para una moneda específica
     *
     * @param currency Identificador de la moneda (ej: "VAULT", "TOKENS", etc.)
     * @return Símbolo de la moneda
     */
    public static String getCurrencySymbol(String currency) {
        // Comprobar si ya está en caché
        if (currencySymbolCache.containsKey(currency)) {
            return currencySymbolCache.get(currency);
        }

        // Obtener la configuración
        YamlDocument config = ShopMaster.getInstance().getConf();
        String symbol;

        // Si es nulo o vacío o VAULT, usar el símbolo predeterminado
        if (currency == null || currency.isEmpty() || "VAULT".equalsIgnoreCase(currency)) {
            symbol = config.getString("config.economy.default-symbol", "$");
        } else {
            // Intentar obtener un símbolo personalizado de la configuración
            symbol = config.getString("config.economy.symbols." + currency.toLowerCase(), currency);
        }

        // Guardar en caché para futuras consultas
        currencySymbolCache.put(currency, symbol);
        return symbol;
    }

    /**
     * Formatea un valor monetario con su símbolo correspondiente
     *
     * @param amount Cantidad a formatear
     * @param currency Identificador de la moneda
     * @param decimals Número de decimales a mostrar
     * @return Cadena formateada con el valor y símbolo
     */
    public static String formatCurrency(double amount, String currency, int decimals) {
        String symbol = getCurrencySymbol(currency);
        String formattedAmount = formatNumber(amount, decimals);

        // Obtener la posición del símbolo de la configuración (antes o después)
        YamlDocument config = ShopMaster.getInstance().getConf();
        boolean symbolBefore = config.getBoolean("config.economy.symbol-before", true);

        if (symbolBefore) {
            return symbol + formattedAmount;
        } else {
            return formattedAmount + " " + symbol;
        }
    }

    /**
     * Formatea un valor monetario con su símbolo correspondiente usando 2 decimales
     *
     * @param amount Cantidad a formatear
     * @param currency Identificador de la moneda
     * @return Cadena formateada con el valor y símbolo
     */
    public static String formatCurrency(double amount, String currency) {
        return formatCurrency(amount, currency, 2);
    }

    /**
     * Formatea una lista de monedas y cantidades para mostrar
     *
     * @param currencyAmounts Mapa de monedas y cantidades
     * @return Cadena formateada con las monedas y cantidades
     */
    public static String formatCurrencyList(Map<String, Double> currencyAmounts) {
        if (currencyAmounts == null || currencyAmounts.isEmpty()) {
            return "";
        }

        if (currencyAmounts.size() == 1) {
            // Si solo hay una moneda, devolver solo el símbolo
            String currency = currencyAmounts.keySet().iterator().next();
            return getCurrencySymbol(currency);
        }

        StringBuilder result = new StringBuilder("(");
        boolean first = true;

        for (Map.Entry<String, Double> entry : currencyAmounts.entrySet()) {
            if (!first) {
                result.append(", ");
            }
            first = false;

            result.append(formatCurrency(entry.getValue(), entry.getKey()));
        }

        result.append(")");
        return result.toString();
    }

    /**
     * Formatea un número según la configuración
     * @param number El número a formatear
     * @return El número formateado como String
     */
    public static String formatNumber(double number) {
        // Si es un número entero y hideFraction está activado
        if (hideFraction && esNumeroEntero(number)) {
            return String.format("%,d", (long) number);
        }

        // Si se debe usar formato de escala corta
        if (enableShortScale && Math.abs(number) >= shortScaleLimit) {
            return formatShortScale(number);
        }

        // Formato normal
        return decimalFormat.format(number);
    }

    /**
     * Formatea un número con un número específico de decimales
     * @param number El número a formatear
     * @param decimals Número de decimales a usar
     * @return El número formateado como String
     */
    public static String formatNumber(double number, int decimals) {
        // Si es un número entero y hideFraction está activado
        if (hideFraction && esNumeroEntero(number)) {
            return String.format("%,d", (long) number);
        }

        // Si se debe usar formato de escala corta
        if (enableShortScale && Math.abs(number) >= shortScaleLimit) {
            return formatShortScale(number, decimals);
        }

        // Formato normal con decimales específicos
        DecimalFormat customFormat = (DecimalFormat) decimalFormat.clone();
        customFormat.setMaximumFractionDigits(decimals);
        customFormat.setMinimumFractionDigits(decimals);

        return customFormat.format(number);
    }

    /**
     * Verifica si un número es entero (sin parte decimal)
     */
    private static boolean esNumeroEntero(double number) {
        return number == Math.floor(number);
    }

    /**
     * Determina el índice del sufijo apropiado para un número dado
     * @param number El número para el que se necesita determinar el sufijo
     * @return El índice del sufijo en el array de sufijos
     */
    private static int determinarIndiceSufijo(double number) {
        int suffixIndex = 0;
        double absNumber = Math.abs(number);

        // Cada iteración representa una multiplicación por 1000:
        // 0-999: suffixIndex = 0 (sin sufijo)
        // 1,000-999,999: suffixIndex = 1 ("K")
        // 1,000,000-999,999,999: suffixIndex = 2 ("M")
        // etc.
        while (absNumber >= 1000 && suffixIndex < suffixes.length - 1) {
            absNumber /= 1000;
            suffixIndex++;
        }

        return suffixIndex;
    }

    /**
     * Formatea un número en escala corta usando los suffixes configurados
     * @param number El número a formatear
     * @return El número formateado en escala corta (ej: "1.5 M")
     */
    private static String formatShortScale(double number) {
        return formatShortScale(number, shortHandDecimalLimit);
    }

    /**
     * Formatea un número en escala corta usando los suffixes configurados
     * @param number El número a formatear
     * @param decimals Número de decimales a usar
     * @return El número formateado en escala corta (ej: "1.5 M")
     */
    private static String formatShortScale(double number, int decimals) {
        int suffixIndex = determinarIndiceSufijo(number);
        double absNumber = Math.abs(number);

        // Dividir el número por 1000 tantas veces como indica el índice del sufijo
        for (int i = 0; i < suffixIndex; i++) {
            absNumber /= 1000;
        }

        // Aplicar formato específico para escala corta
        DecimalFormat shortFormat = (DecimalFormat) decimalFormat.clone();
        shortFormat.setMaximumFractionDigits(decimals);
        shortFormat.setMaximumIntegerDigits(shortHandNumberLimit);

        String signo = number < 0 ? "-" : "";
        String valor = shortFormat.format(absNumber);

        return signo + valor + " " + suffixes[suffixIndex];
    }

    /**
     * Limpia la caché de símbolos
     */
    public static void clearCache() {
        currencySymbolCache.clear();
    }

    /**
     * Actualiza la caché con valores de la configuración actual
     */
    public static void refreshCache() {
        clearCache();
        YamlDocument config = ShopMaster.getInstance().getConf();

        // Pre-cargar el símbolo predeterminado
        currencySymbolCache.put("VAULT", config.getString("config.economy.default-symbol", "$"));

        // Pre-cargar símbolos personalizados si existen
        if (config.isSection("config.economy.symbols")) {
            for (String key : config.getSection("config.economy.symbols").getRoutesAsStrings(false)) {
                String currencyId = key.substring(key.lastIndexOf('.') + 1).toUpperCase();
                String symbol = config.getString(key);
                currencySymbolCache.put(currencyId, symbol);
            }
        }
    }

    /**
     * Recarga la configuración del formateador de números
     */
    private static void reloadNumberFormat() {
        FileConfiguration config = ShopMaster.getInstance().getConfig();

        // Cargar configuración de formato
        DecimalFormatSymbols symbols = crearSimbolosFormato(config);
        String pattern = crearPatronFormato(config);

        // Crear y configurar formateador decimal
        decimalFormat = new DecimalFormat(pattern, symbols);
        configurarLimitesEnteros(config);

        // Cargar otras configuraciones
        hideFraction = config.getBoolean("config.number-format.hide-fraction", true);

        // Cargar configuración de escala corta
        enableShortScale = config.getBoolean("config.number-format.short-scale.enable-numbering", false);
        shortScaleLimit = config.getLong("config.number-format.short-scale.limit", 1000000);
        shortHandDecimalLimit = config.getInt("config.number-format.short-scale.hand-decimal-limit", 2);
        shortHandNumberLimit = config.getInt("config.number-format.short-scale.hand-number-limit", 32);

        // Cargar suffixes desde la configuración
        suffixes = cargarSuffixes(config);
    }

    /**
     * Carga los suffixes desde la configuración o usa los valores por defecto
     */
    private static String[] cargarSuffixes(FileConfiguration config) {
        // Valores predeterminados si no se encuentra la configuración
        String[] defaultSuffixes = {"", "K", "M", "B", "T", "Q"};

        // Intentar cargar desde la configuración
        if (config.contains("config.number-format.short-scale.suffixes")) {
            List<String> suffixList = config.getStringList("config.number-format.short-scale.suffixes");

            // Verificar si la lista no está vacía
            if (!suffixList.isEmpty()) {
                return suffixList.toArray(new String[0]);
            }
        }

        return defaultSuffixes;
    }

    /**
     * Crea los símbolos de formato decimal según la configuración
     */
    private static DecimalFormatSymbols crearSimbolosFormato(FileConfiguration config) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator(config.getString("config.number-format.decimal-separator", ".").charAt(0));
        symbols.setGroupingSeparator(config.getString("config.number-format.grouping-separator", ",").charAt(0));
        return symbols;
    }

    /**
     * Crea el patrón de formato decimal según la configuración
     */
    private static String crearPatronFormato(FileConfiguration config) {
        StringBuilder pattern = new StringBuilder("#,##0");

        int minFractionDigits = config.getInt("config.number-format.minimum-fraction-digits", 0);
        int maxFractionDigits = config.getInt("config.number-format.maximum-fraction-digits", 8);

        if (maxFractionDigits > 0) {
            pattern.append(".");
            for (int i = 0; i < minFractionDigits; i++) {
                pattern.append("0");
            }
            for (int i = minFractionDigits; i < maxFractionDigits; i++) {
                pattern.append("#");
            }
        }

        return pattern.toString();
    }

    /**
     * Configura los límites de dígitos enteros en el formateador
     */
    private static void configurarLimitesEnteros(FileConfiguration config) {
        decimalFormat.setMinimumIntegerDigits(config.getInt("config.number-format.minimum-integer-digits", 1));
        decimalFormat.setMaximumIntegerDigits(config.getInt("config.number-format.maximum-integer-digits", 32));
    }
}