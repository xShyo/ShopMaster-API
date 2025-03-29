package xshyo.us.shopMaster.utilities;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import xshyo.us.shopMaster.ShopMaster;

public class NumberFormatter {

    private DecimalFormat decimalFormat;
    private boolean hideFraction;

    // Configuración de escala corta
    private boolean enableShortScale;
    private long shortScaleLimit;
    private int shortHandDecimalLimit;
    private int shortHandNumberLimit;
    private String[] suffixes;

    /**
     * Inicializa el formateador de números basado en la configuración del plugin
     */
    public NumberFormatter() {
        reload();
    }

    /**
     * Recarga la configuración del formateador de números
     */
    public void reload() {
        FileConfiguration config = ShopMaster.getInstance().getConfig();

        // Cargar configuración de formato
        DecimalFormatSymbols symbols = crearSimbolosFormato(config);
        String pattern = crearPatronFormato(config);

        // Crear y configurar formateador decimal
        decimalFormat = new DecimalFormat(pattern, symbols);
        configurarLimitesEnteros(config);

        // Cargar otras configuraciones
        this.hideFraction = config.getBoolean("config.number-format.hide-fraction", true);

        // Cargar configuración de escala corta
        this.enableShortScale = config.getBoolean("config.number-format.short-scale.enable-numbering", false);
        this.shortScaleLimit = config.getLong("config.number-format.short-scale.limit", 1000000);
        this.shortHandDecimalLimit = config.getInt("config.number-format.short-scale.hand-decimal-limit", 2);
        this.shortHandNumberLimit = config.getInt("config.number-format.short-scale.hand-number-limit", 32);

        // Cargar suffixes desde la configuración
        this.suffixes = cargarSuffixes(config);
    }

    /**
     * Carga los suffixes desde la configuración o usa los valores por defecto
     */
    private String[] cargarSuffixes(FileConfiguration config) {
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
    private DecimalFormatSymbols crearSimbolosFormato(FileConfiguration config) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator(config.getString("config.number-format.decimal-separator", ".").charAt(0));
        symbols.setGroupingSeparator(config.getString("config.number-format.grouping-separator", ",").charAt(0));
        return symbols;
    }

    /**
     * Crea el patrón de formato decimal según la configuración
     */
    private String crearPatronFormato(FileConfiguration config) {
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
    private void configurarLimitesEnteros(FileConfiguration config) {
        decimalFormat.setMinimumIntegerDigits(config.getInt("config.number-format.minimum-integer-digits", 1));
        decimalFormat.setMaximumIntegerDigits(config.getInt("config.number-format.maximum-integer-digits", 32));
    }

    /**
     * Formatea un número según la configuración
     * @param number El número a formatear
     * @return El número formateado como String
     */
    public String format(double number) {
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
     * Verifica si un número es entero (sin parte decimal)
     */
    private boolean esNumeroEntero(double number) {
        return number == Math.floor(number);
    }

    /**
     * Determina el índice del sufijo apropiado para un número dado
     * @param number El número para el que se necesita determinar el sufijo
     * @return El índice del sufijo en el array de sufijos
     */
    private int determinarIndiceSufijo(double number) {
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
    private String formatShortScale(double number) {
        int suffixIndex = determinarIndiceSufijo(number);
        double absNumber = Math.abs(number);

        // Dividir el número por 1000 tantas veces como indica el índice del sufijo
        for (int i = 0; i < suffixIndex; i++) {
            absNumber /= 1000;
        }

        // Aplicar formato específico para escala corta
        DecimalFormat shortFormat = (DecimalFormat) decimalFormat.clone();
        shortFormat.setMaximumFractionDigits(shortHandDecimalLimit);
        shortFormat.setMaximumIntegerDigits(shortHandNumberLimit);

        String signo = number < 0 ? "-" : "";
        String valor = shortFormat.format(absNumber);

        return signo + valor + " " + suffixes[suffixIndex];
    }
}