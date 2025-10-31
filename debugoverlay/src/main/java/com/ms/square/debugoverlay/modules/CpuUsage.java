package com.ms.square.debugoverlay.modules;

public class CpuUsage {
    private final double total;
    private final double myPid;

    public CpuUsage(double total, double myPid) {
        this.total = total;
        this.myPid = myPid;
    }

    public double getTotal() {
        return total;
    }

    public double getMyPid() {
        return myPid;
    }
}
