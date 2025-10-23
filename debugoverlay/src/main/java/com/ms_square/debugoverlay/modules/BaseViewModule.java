package com.ms_square.debugoverlay.modules;

import androidx.annotation.LayoutRes;

import com.ms_square.debugoverlay.ViewModule;

public abstract class BaseViewModule<T> implements ViewModule<T> {

    @LayoutRes
    protected int layoutResId;

    public BaseViewModule(@LayoutRes int layoutResId) {
        this.layoutResId = layoutResId;
    }
}
