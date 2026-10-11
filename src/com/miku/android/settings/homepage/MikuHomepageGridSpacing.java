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
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.RecyclerView;

import com.miku.android.settings.R;

/** Same inset on every card so columns stay equal width. */
final class MikuHomepageGridSpacing extends RecyclerView.ItemDecoration {

    private final int mHalfGap;

    MikuHomepageGridSpacing(Context context) {
        mHalfGap = context.getResources().getDimensionPixelSize(R.dimen.miku_homepage_grid_gap) / 2;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
            @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        final RecyclerView.Adapter<?> adapter = parent.getAdapter();
        final int position = parent.getChildAdapterPosition(view);
        if (adapter instanceof MikuHomepagePreferenceAdapter && position >= 0) {
            final Preference preference =
                    ((MikuHomepagePreferenceAdapter) adapter).preferenceAt(position);
            if (preference instanceof PreferenceCategory) {
                outRect.setEmpty();
                return;
            }
        }
        outRect.set(mHalfGap, mHalfGap, mHalfGap, mHalfGap);
    }
}
