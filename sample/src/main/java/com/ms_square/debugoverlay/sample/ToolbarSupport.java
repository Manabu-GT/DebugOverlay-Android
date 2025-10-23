package com.ms_square.debugoverlay.sample;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

public class ToolbarSupport {

    public static void addTo(@NonNull final AppCompatActivity activity) {
        addTo(activity, 0);
    }

    public static void addTo(@NonNull final AppCompatActivity activity, @ColorRes int toolBarBgColor) {
        addTo(activity, 0, toolBarBgColor);
    }

    public static void addTo(@NonNull final AppCompatActivity activity, @StringRes int toolBarTitle,
                             @ColorRes int toolBarBgColor) {
        Toolbar toolbar = getToolbar(activity);
        if (toolBarTitle != 0) {
            toolbar.setTitle(toolBarTitle);
        }
        if (toolBarBgColor != 0) {
            toolbar.setBackgroundColor(ContextCompat.getColor(activity, toolBarBgColor));
        }
        activity.setSupportActionBar(toolbar);

        if (hasParentActivity(activity)) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent upIntent = NavUtils.getParentActivityIntent(activity);
                    if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
                        // This activity is NOT part of this app's task, so create a new task
                        // when navigating up, with a synthesized back stack.
                        TaskStackBuilder.create(activity)
                                // Add all of this activity's parents to the back stack
                                .addNextIntentWithParentStack(upIntent)
                                // Navigate up to the closest parent
                                .startActivities();
                    } else {
                        // This activity is part of this app's task, so simply
                        // navigate up to the logical parent activity.
                        NavUtils.navigateUpTo(activity, upIntent);
                    }
                }
            });
        }
    }

    public static Toolbar getToolbar(AppCompatActivity activity) {
        return (Toolbar) activity.findViewById(R.id.toolbar);
    }

    private static boolean hasParentActivity(Activity activity) {
        try {
            return NavUtils.getParentActivityName(activity) != null;
        } catch (IllegalArgumentException e) {
            // Component name of supplied activity does not exist...
            return false;
        }
    }
}