package com.koushikdutta.tether;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.ScaleAnimation;

public final class AnimatedView {
    public static void setOnClickListener(final View view, final OnClickListener listener) {
        view.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ScaleAnimation scale = new ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f, 1, 0.5f, 1, 0.5f);
                scale.setDuration(250);
                view.setAnimation(scale);
                listener.onClick(view);
            }
        });
    }
}
