/*
 * Copyright (C) 2026 Miku UI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miku.android.settings.widget;

import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import com.miku.android.settings.R;

/** Homepage tile; every card uses the same height so two-line summaries fit without extra empty space. */
public class MikuHomepageCard extends FrameLayout {

    /** VComic ListItemEntrance timings, twice as slow. */
    private static final int ENTRANCE_STAGGER_MS = 72;
    private static final long ENTRANCE_DURATION_MS = 760L;
    private static final float ENTRANCE_SLIDE_PX = 14f;
    private static final float ENTRANCE_SCALE_FROM = 0.96f;
    private static final Interpolator ENTRANCE_INTERPOLATOR =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    private final Runnable mStartEntrance = this::startEntranceWhenReady;
    private StateListAnimator mHeldStateListAnimator;
    private boolean mEntrancePlaying;

    public MikuHomepageCard(Context context) {
        super(context);
        init();
    }

    public MikuHomepageCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MikuHomepageCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public boolean isEntrancePlaying() {
        return mEntrancePlaying;
    }

    /**
     * Alpha + translationY + scale on the draw layer only. Layout size stays full so the
     * wrap-content grid does not jump. Press scale is paused until the entrance ends.
     */
    public void playEntrance(int staggerIndex) {
        cancelEntrance();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            applyRest();
            return;
        }
        mEntrancePlaying = true;
        holdPressScale();
        setAlpha(0f);
        postDelayed(mStartEntrance,
                (long) Math.max(0, staggerIndex) * ENTRANCE_STAGGER_MS);
    }

    public void hideForEntrance() {
        cancelEntrance();
        setAlpha(0f);
        setTranslationY(0f);
        setScaleX(1f);
        setScaleY(1f);
    }

    public void resetEntrance() {
        cancelEntrance();
        applyRest();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mEntrancePlaying) {
            resetEntrance();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = getResources()
                .getDimensionPixelSize(R.dimen.miku_homepage_card_height);
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    private void startEntranceWhenReady() {
        if (!mEntrancePlaying) {
            return;
        }
        if (!isAttachedToWindow()) {
            resetEntrance();
            return;
        }
        if (getWidth() == 0 || getHeight() == 0) {
            post(mStartEntrance);
            return;
        }
        setPivotX(getWidth() / 2f);
        setPivotY(getHeight() / 2f);
        setTranslationY(ENTRANCE_SLIDE_PX);
        setScaleX(ENTRANCE_SCALE_FROM);
        setScaleY(ENTRANCE_SCALE_FROM);
        animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ENTRANCE_DURATION_MS)
                .setInterpolator(ENTRANCE_INTERPOLATOR)
                .withEndAction(this::onEntranceEnded)
                .start();
    }

    private void onEntranceEnded() {
        mEntrancePlaying = false;
        restorePressScale();
    }

    private void cancelEntrance() {
        mEntrancePlaying = false;
        removeCallbacks(mStartEntrance);
        animate().setListener(null).withEndAction(null).cancel();
        restorePressScale();
    }

    private void applyRest() {
        setAlpha(1f);
        setTranslationY(0f);
        setScaleX(1f);
        setScaleY(1f);
    }

    private void holdPressScale() {
        if (mHeldStateListAnimator != null) {
            return;
        }
        mHeldStateListAnimator = getStateListAnimator();
        setStateListAnimator(null);
    }

    private void restorePressScale() {
        if (mHeldStateListAnimator == null) {
            return;
        }
        setStateListAnimator(mHeldStateListAnimator);
        mHeldStateListAnimator = null;
    }
}
