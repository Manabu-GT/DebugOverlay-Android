package com.ms.square.debugoverlay;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

class OverlayViewManager {

    private static final String TAG = "OverlayViewManager";

    private final Context context;
    private final DebugOverlay.Config config;
    private final WindowManager windowManager;

    private List<OverlayModule<?>> overlayModules = Collections.emptyList();
    private ViewGroup rootView;

    private boolean overlayPermissionRequested;

    public OverlayViewManager(@NonNull Context context, @NonNull DebugOverlay.Config config) {
        this.context = context;
        this.config = config;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setOverlayModules(@NonNull List<OverlayModule<?>> overlayModules) {
        this.overlayModules = overlayModules;
    }

    public void showDebugSystemOverlay() {
        if (config.isAllowSystemLayer() && rootView == null) {
            if (!canDrawOnSystemLayer(context, getWindowTypeForOverlay(true))) {
                Toast.makeText(context, R.string.debugoverlay_overlay_permission_prompt, Toast.LENGTH_LONG).show();
                requestDrawOnSystemLayerPermission(context);
                overlayPermissionRequested = true;
                return;
            }

            overlayPermissionRequested = false;

            rootView = createRoot();

            int layoutParamsWidth = WindowManager.LayoutParams.WRAP_CONTENT;

            for (OverlayModule<?> overlayModule : overlayModules) {
                View view = overlayModule.createView(rootView, config.getTextColor(), config.getTextSize(), config.getTextAlpha());
                if (view.getParent() == null) {
                    if (view.getLayoutParams() != null && view.getLayoutParams().width == MATCH_PARENT) {
                        layoutParamsWidth = WindowManager.LayoutParams.MATCH_PARENT;
                    }
                    rootView.addView(view);
                }
            }

            WindowManager.LayoutParams params = createLayoutParams(config.isAllowSystemLayer(), layoutParamsWidth, null);
            windowManager.addView(rootView, params);
        }
    }

    public void hideDebugSystemOverlay() {
        if (config.isAllowSystemLayer() && rootView != null) {
            windowManager.removeView(rootView);
            rootView = null;
        }
    }

    public boolean isSystemOverlayShown() {
        return rootView != null;
    }

    public boolean isOverlayPermissionRequested() {
        return overlayPermissionRequested;
    }

    public OverlayViewAttachStateChangeListener createAttachStateChangeListener() {
        return new OverlayViewAttachStateChangeListener();
    }

    // for layoutParams.gravity assignment
    @SuppressLint("WrongConstant")
    private WindowManager.LayoutParams createLayoutParams(boolean allowSystemLayer, int width, IBinder windowToken) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = width;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        if (windowToken != null) {
            layoutParams.token = windowToken;
        }
        layoutParams.type = getWindowTypeForOverlay(allowSystemLayer);
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = config.getPosition().getGravity();
        return layoutParams;
    }

    private ViewGroup createRoot() {
        LinearLayout overlayRoot = new LinearLayout(context);
        overlayRoot.setOrientation(LinearLayout.VERTICAL);
        if (config.getBgColor() != Color.TRANSPARENT) {
            overlayRoot.setBackgroundColor(config.getBgColor());
        }
        return overlayRoot;
    }

    class OverlayViewAttachStateChangeListener implements View.OnAttachStateChangeListener {

        private ViewGroup _rootView;

        public void onActivityResumed() {
            if (_rootView != null && _rootView.getChildCount() > 0) {
                if (DebugOverlay.DEBUG) {
                    Log.i(TAG, "overlay views recreated on Activity's onResume");
                }
                _rootView.removeAllViews();
                for (OverlayModule<?> overlayModule : overlayModules) {
                    View view = overlayModule.createView(_rootView, config.getTextColor(), config.getTextSize(), config.getTextAlpha());
                    if (view.getParent() == null) {
                        _rootView.addView(view);
                    }
                }
                // force-update recreated views with the latest data
                for (OverlayModule<?> overlayModule : overlayModules) {
                    overlayModule.notifyObservers();
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(@NonNull View v) {
            if (DebugOverlay.DEBUG) {
                Log.i(TAG, "onViewAttachedToWindow");
            }
            _rootView = createRoot();
            int layoutParamsWidth = WindowManager.LayoutParams.WRAP_CONTENT;
            for (OverlayModule<?> overlayModule : overlayModules) {
                View view = overlayModule.createView(_rootView, config.getTextColor(), config.getTextSize(), config.getTextAlpha());
                if (view.getParent() == null) {
                    if (view.getLayoutParams() != null && view.getLayoutParams().width == MATCH_PARENT) {
                        layoutParamsWidth = WindowManager.LayoutParams.MATCH_PARENT;
                    }
                    _rootView.addView(view);
                }
            }

            windowManager.addView(_rootView, createLayoutParams(config.isAllowSystemLayer(),
                    layoutParamsWidth, v.getWindowToken()));
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull View v) {
            if (DebugOverlay.DEBUG) {
                Log.i(TAG, "onViewDetachedFromWindow");
            }
            windowManager.removeViewImmediate(_rootView);
            v.removeOnAttachStateChangeListener(this);
        }
    }

    public static void requestDrawOnSystemLayerPermission(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static boolean canDrawOnSystemLayer(@NonNull Context context, int systemWindowType) {
        if (isSystemLayer(systemWindowType)) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public static boolean isSystemLayer(int windowType) {
        return windowType >= FIRST_SYSTEM_WINDOW;
    }

    public static int getWindowTypeForOverlay(boolean allowSystemLayer) {
        if (allowSystemLayer) {
            return TYPE_APPLICATION_OVERLAY;
        } else {
            // make layout of the window happens as that of a top-level window, not as a child of its container
            return TYPE_APPLICATION_ATTACHED_DIALOG;
        }
    }
}
