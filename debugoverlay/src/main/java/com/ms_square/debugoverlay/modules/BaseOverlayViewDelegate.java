package com.ms_square.debugoverlay.modules;

import android.support.annotation.LayoutRes;

import com.ms_square.debugoverlay.OverlayViewDelegate;

public abstract class BaseOverlayViewDelegate<T> implements OverlayViewDelegate<T> {

    @LayoutRes
    protected int layoutResId;

    public BaseOverlayViewDelegate(@LayoutRes int layoutResId) {
        this.layoutResId = layoutResId;
    }
}
