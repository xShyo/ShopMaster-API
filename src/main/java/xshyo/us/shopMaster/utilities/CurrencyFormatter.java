package xshyo.us.shopMaster.utilities;

import xshyo.us.shopMaster.ShopMaster;
import dev.dejvokep.boostedyaml.YamlDocument;

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
        try {
            // Establecer valor por defecto
            if (currency == null || currency.isEmpty()) {
                currency = "VAULT";
            }

            // Normalizar para búsqueda en caché
            String cacheKey = currency.toUpperCase();

            // Si ya está cacheado, devuelve directamente
            if (currencySymbolCache.containsKey(cacheKey)) {
                String cachedSymbol = currencySymbolCache.get(cacheKey);
                // Si el símbolo cacheado es null, devuelve un valor por defecto
                return (cachedSymbol != null) ? cachedSymbol : "$";
            }

            YamlDocument config = ShopMaster.getInstance().getConf();
            // Si config es null, devuelve un valor por defecto
            if (config == null) {
                return "$";
            }

            String symbol;

            // Búsqueda insensible a mayúsculas/minúsculas
            if ("vault".equalsIgnoreCase(currency)) {
                // Para VAULT usar el símbolo predeterminado
                symbol = config.getString("config.economy.default-symbol");
            } else {
                // Para otras monedas, buscar en la sección symbols
                String path = "config.economy.symbols." + currency.toLowerCase();
                symbol = config.getString(path);
            }

            // Si el símbolo es null, usar un valor por defecto
            if (symbol == null) {
                symbol = "$"; // Valor por defecto seguro
            }

            // Almacenar en caché y devolver
            currencySymbolCache.put(cacheKey, symbol);
            return symbol;
        } catch (Exception e) {
            return "$";
        }
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

        // Verificar si el config es null
        if (config == null) {
            return;
        }

        try {
            // Pre-cargar el símbolo predeterminado
            String defaultSymbol = config.getString("config.economy.default-symbol", "$");
            currencySymbolCache.put("VAULT", defaultSymbol);

            // Verificar si existe la sección de símbolos
            if (config.contains("config.economy.symbols")) {

                // Verificar si es realmente una sección y no un valor individual
                if (config.isSection("config.economy.symbols")) {
                    // Obtener todas las claves directamente desde la sección
                    for (Object currencyKey : config.getSection("config.economy.symbols").getKeys()) {
                        String fullPath = "config.economy.symbols." + currencyKey;
                        String symbol = config.getString(fullPath);
                        String normalizedKey = currencyKey.toString().toUpperCase();

                        currencySymbolCache.put(normalizedKey, symbol);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * Recarga la configuración del formateador de números
     */
    private static void reloadNumberFormat() {
        YamlDocument config = ShopMaster.getInstance().getConf();

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
        shortScaleLimit = config.getLong("config.number-format.short-scale.limit", 1000000L);
        shortHandDecimalLimit = config.getInt("config.number-format.short-scale.hand-decimal-limit", 2);
        shortHandNumberLimit = config.getInt("config.number-format.short-scale.hand-number-limit", 32);

        // Cargar suffixes desde la configuración
        suffixes = cargarSuffixes(config);
    }

    /**
     * Carga los suffixes desde la configuración o usa los valores por defecto
     */
    private static String[] cargarSuffixes(YamlDocument config) {
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
    private static DecimalFormatSymbols crearSimbolosFormato(YamlDocument config) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator(config.getString("config.number-format.decimal-separator", ".").charAt(0));
        symbols.setGroupingSeparator(config.getString("config.number-format.grouping-separator", ",").charAt(0));
        return symbols;
    }

    /**
     * Crea el patrón de formato decimal según la configuración
     */
    private static String crearPatronFormato(YamlDocument config) {
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
    private static void configurarLimitesEnteros(YamlDocument config) {
        decimalFormat.setMinimumIntegerDigits(config.getInt("config.number-format.minimum-integer-digits", 1));
        decimalFormat.setMaximumIntegerDigits(config.getInt("config.number-format.maximum-integer-digits", 32));
    }
}