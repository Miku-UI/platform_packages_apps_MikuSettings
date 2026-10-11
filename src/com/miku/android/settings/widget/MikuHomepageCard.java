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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.miku.android.settings.R;

/** Homepage tile; every card uses the same height so two-line summaries fit without extra empty space. */
public class MikuHomepageCard extends FrameLayout {

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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = getResources()
                .getDimensionPixelSize(R.dimen.miku_homepage_card_height);
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
