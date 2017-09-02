package com.ms_square.debugoverlay.modules;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CpuFreq {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    private final String cpuName;
    private final double minFreq;
    private final double curFreq;
    private final double maxFreq;

    public CpuFreq(String cpuName, double minFreq, double curFreq, double maxFreq) {
        this.cpuName = cpuName;
        this.minFreq = minFreq;
        this.curFreq = curFreq;
        this.maxFreq = maxFreq;
    }

    public String getCpuName() {
        return cpuName;
    }

    public double getMinFreq() {
        return minFreq;
    }

    public double getCurFreq() {
        return curFreq;
    }

    public double getMaxFreq() {
        return maxFreq;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s(GHz): %s/%s ", cpuName,
                curFreq >= 0 ? DECIMAL_FORMAT.format(curFreq / 1000000f) : "NA",
                maxFreq >= 0 ? DECIMAL_FORMAT.format(maxFreq / 1000000f) : "NA");
    }
}
