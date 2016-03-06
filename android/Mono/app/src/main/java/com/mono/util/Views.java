package com.mono.util;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

public class Views {

    private Views() {}

    public static Animator fade(View view, float fromAlpha, float toAlpha, int duration,
            AnimatorListener listener) {
        Animator animator = ObjectAnimator.ofFloat(view, "alpha", fromAlpha, toAlpha);
        animator.setDuration(duration);

        if (listener != null) {
            animator.addListener(listener);
        }

        animator.start();

        return animator;
    }

    public static Animator scale(final View view, int height, long duration,
            AnimatorListener listener) {
        if (view.getHeight() == height) {
            return null;
        }

        ValueAnimator animator = ValueAnimator.ofInt(view.getHeight(), height);
        animator.setDuration(duration);
        animator.setStartDelay(0);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = (Integer) animator.getAnimatedValue();

                view.setLayoutParams(params);
            }
        });

        if (listener != null) {
            animator.addListener(listener);
        }

        animator.start();

        return animator;
    }

    public static Animator translateX(View view, final int left, long duration,
            final AnimatorListener listener) {
        float deltaX = left - view.getLeft();

        Animator animator = ObjectAnimator.ofFloat(view, "translationX", deltaX);
        animator.setDuration(duration);
        animator.setStartDelay(0);

        if (listener != null) {
            animator.addListener(listener);
        }

        animator.start();

        return animator;
    }

    public static Animator translateY(View view, int top, long duration,
            AnimatorListener listener) {
        float deltaY = top - view.getTop();

        Animator animator = ObjectAnimator.ofFloat(view, "translationY", deltaY);
        animator.setDuration(duration);
        animator.setStartDelay(0);

        if (listener != null) {
            animator.addListener(listener);
        }

        animator.start();

        return animator;
    }
}
