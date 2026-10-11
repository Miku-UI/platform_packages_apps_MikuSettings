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

package com.miku.android.settings.homepage;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.miku.android.settings.R;

/** Grid that picks a column count from the available width and measures every tile. */
final class MikuHomepageGridLayoutManager extends GridLayoutManager {

    private final Context mContext;

    MikuHomepageGridLayoutManager(Context context) {
        super(context, /* spanCount= */ 1);
        mContext = context;
        setMeasurementCacheEnabled(false);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
            int widthSpec, int heightSpec) {
        updateSpanCount(View.MeasureSpec.getSize(widthSpec));
        super.onMeasure(recycler, state, widthSpec, heightSpec);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateSpanCount(getWidth());
        super.onLayoutChildren(recycler, state);
    }

    private void updateSpanCount(int width) {
        final int totalSpace = width - getPaddingRight() - getPaddingLeft();
        final int columnWidth = mContext.getResources()
                .getDimensionPixelSize(R.dimen.miku_homepage_card_min_column_width);
        final int gap = mContext.getResources()
                .getDimensionPixelSize(R.dimen.miku_homepage_grid_gap);
        if (totalSpace <= 0 || columnWidth <= 0) {
            return;
        }
        final int spanCount = Math.max(1, (totalSpace + gap) / (columnWidth + gap));
        if (spanCount != getSpanCount()) {
            getSpanSizeLookup().invalidateSpanIndexCache();
            setSpanCount(spanCount);
        }
    }
}
