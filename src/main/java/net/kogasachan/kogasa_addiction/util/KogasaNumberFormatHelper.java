package net.kogasachan.kogasa_addiction.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class KogasaNumberFormatHelper {
    /**
     * 最多保留 maxDecimals 位小数，自动去掉末尾无意义的 0。
     * 例：4.0 → "4"，4.50 → "4.5"，1.20 → "1.2"，1.00 → "1"
     */
    public static String formatTrimmed(double value, int maxDecimals) {
        StringBuilder pattern = new StringBuilder("#");
        if (maxDecimals > 0) {
            pattern.append('.');
            pattern.append("#".repeat(maxDecimals));
        }
        DecimalFormat df = new DecimalFormat(
                pattern.toString(),
                DecimalFormatSymbols.getInstance(Locale.ROOT));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(value);
    }

    /**
     * 百分比，输入原始比值（0.33 → "33%"），最多 maxDecimals 位小数，去尾零。
     *
     */
    public static String formatPercentTrimmed(double ratio, int maxDecimals) {
        DecimalFormat df = new DecimalFormat(
                "#" + (maxDecimals > 0 ? "." + "#".repeat(maxDecimals) : "") + "%",
                DecimalFormatSymbols.getInstance(Locale.ROOT));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(ratio);
    }
}
