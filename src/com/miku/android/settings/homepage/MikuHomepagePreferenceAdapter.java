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

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.SettingsActivity;
import com.android.settings.homepage.HighlightableMenu;
import com.android.settingslib.widget.AdaptiveIcon;
import com.miku.android.settings.R;

final class MikuHomepagePreferenceAdapter extends PreferenceGroupAdapter {

    private final MikuTopLevelSettings mFragment;
    private String mHighlightKey;
    private String mHiddenKey;
    private int mHighlightPosition = RecyclerView.NO_POSITION;
    private boolean mHighlightNeeded;

    MikuHomepagePreferenceAdapter(@NonNull PreferenceGroup preferenceGroup,
            @NonNull MikuTopLevelSettings fragment) {
        super(preferenceGroup);
        mFragment = fragment;
        final Bundle args = fragment.getArguments();
        if (args != null) {
            mHighlightKey = HighlightableMenu.lookupPreferenceKey(
                    args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY));
        }
    }

    @Nullable
    String getHighlightKey() {
        return mHighlightKey;
    }

    @Nullable
    Preference preferenceAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        return getItem(position);
    }

    void highlightPreference(@Nullable String key, boolean scrollNeeded) {
        mHighlightKey = key;
        requestHighlight();
        if (scrollNeeded) {
            final RecyclerView list = mFragment.getListView();
            final int position = getHighlightPosition();
            if (list != null && position >= 0) {
                list.scrollToPosition(position);
            }
        }
    }

    void setMenuHighlightShowed(boolean show) {
        if (show) {
            mHighlightKey = mHiddenKey;
            mHiddenKey = null;
        } else {
            if (mHiddenKey == null) {
                mHiddenKey = mHighlightKey;
            }
            mHighlightKey = null;
        }
        requestHighlight();
    }

    void requestHighlight() {
        final int previous = mHighlightPosition;
        if (TextUtils.isEmpty(mHighlightKey)) {
            mHighlightPosition = RecyclerView.NO_POSITION;
            if (previous >= 0) {
                notifyItemChanged(previous);
            }
            return;
        }

        final int position = getHighlightPosition();
        if (position < 0) {
            return;
        }

        final boolean highlightNeeded = mFragment.isActivityEmbedded();
        if (highlightNeeded != mHighlightNeeded) {
            mHighlightNeeded = highlightNeeded;
            mHighlightPosition = position;
            notifyItemChanged(position);
            if (!highlightNeeded && previous >= 0 && previous != position) {
                notifyItemChanged(previous);
            }
            return;
        }

        if (position == mHighlightPosition) {
            return;
        }

        mHighlightPosition = position;
        if (!highlightNeeded) {
            return;
        }
        notifyItemChanged(position);
        if (previous >= 0) {
            notifyItemChanged(previous);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        final Preference preference = getItem(position);
        final View itemView = holder.itemView;
        if (preference instanceof PreferenceCategory) {
            itemView.setVisibility(View.GONE);
            itemView.setMinimumHeight(0);
            final ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null) {
                lp.height = 0;
                itemView.setLayoutParams(lp);
            }
            itemView.setActivated(false);
            return;
        }

        itemView.setVisibility(View.VISIBLE);
        itemView.setVerticalScrollBarEnabled(false);
        itemView.setHorizontalScrollBarEnabled(false);
        itemView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        final ViewGroup.LayoutParams lp = itemView.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            itemView.setLayoutParams(lp);
        }
        final View icon = holder.findViewById(android.R.id.icon);
        if (icon instanceof ImageView) {
            bindHomepageIcon(holder.findViewById(R.id.icon_frame), (ImageView) icon);
        }
        final View summary = holder.findViewById(android.R.id.summary);
        if (summary instanceof TextView) {
            final TextView summaryView = (TextView) summary;
            summaryView.setMinLines(2);
            summaryView.setMaxLines(2);
            summaryView.setVerticalScrollBarEnabled(false);
            summaryView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        }
        final boolean highlight = mFragment.isActivityEmbedded()
                && !TextUtils.isEmpty(mHighlightKey)
                && preference != null
                && TextUtils.equals(mHighlightKey, preference.getKey());
        itemView.setActivated(highlight);
    }

    private int getHighlightPosition() {
        if (TextUtils.isEmpty(mHighlightKey)) {
            return RecyclerView.NO_POSITION;
        }
        return getPreferenceAdapterPosition(mHighlightKey);
    }

    /**
     * Split the Settings homepage icon the same way the original list does: colored well + glyph.
     * Injected tiles (Google, Digital Wellbeing) use {@link AdaptiveIcon} with a ColorScheme;
     * XML tiles use a two-layer list with {@code homepage_*_background}. Do not unwrap into the
     * app's own {@code AdaptiveIconDrawable}, which drops that scheme.
     */
    private static void bindHomepageIcon(@Nullable View iconFrame, ImageView iconView) {
        final int glyphSize = iconView.getResources()
                .getDimensionPixelSize(R.dimen.miku_homepage_icon_size);
        Drawable wellSrc = null;
        Drawable glyph = iconView.getDrawable();
        if (glyph instanceof AdaptiveIcon) {
            final AdaptiveIcon adaptive = (AdaptiveIcon) glyph;
            if (adaptive.getNumberOfLayers() >= 2) {
                wellSrc = adaptive.getDrawable(0);
                glyph = adaptive.getDrawable(1);
            }
        } else if (glyph instanceof LayerDrawable) {
            final LayerDrawable layers = (LayerDrawable) glyph;
            final int count = layers.getNumberOfLayers();
            if (count >= 2) {
                wellSrc = layers.getDrawable(0);
                glyph = layers.getDrawable(count - 1);
            } else if (count == 1) {
                glyph = layers.getDrawable(0);
            }
        }

        if (iconFrame != null) {
            final Drawable well = iconFrame.getContext()
                    .getDrawable(R.drawable.miku_homepage_icon_well_bg);
            if (well != null) {
                final Drawable wellBg = well.mutate();
                final Integer fill = extractFillColor(wellSrc);
                if (fill != null && wellBg instanceof GradientDrawable) {
                    ((GradientDrawable) wellBg).setColor(fill);
                } else if (wellSrc != null && wellSrc.getColorFilter() != null) {
                    wellBg.setColorFilter(wellSrc.getColorFilter());
                }
                iconFrame.setBackground(wellBg);
            }
            iconFrame.setClipToOutline(true);
        }

        iconView.setVisibility(View.VISIBLE);
        iconView.setAdjustViewBounds(false);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setMaxWidth(glyphSize);
        iconView.setMaxHeight(glyphSize);
        if (glyph != null) {
            iconView.setImageDrawable(copyDrawable(glyph));
        }
        final ViewGroup.LayoutParams lp = iconView.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            final FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
            flp.width = glyphSize;
            flp.height = glyphSize;
            flp.gravity = Gravity.CENTER;
            iconView.setLayoutParams(flp);
        } else if (lp != null) {
            lp.width = glyphSize;
            lp.height = glyphSize;
            iconView.setLayoutParams(lp);
        }
    }

    @Nullable
    private static Integer extractFillColor(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        final ColorFilter filter = drawable.getColorFilter();
        if (filter instanceof PorterDuffColorFilter) {
            return ((PorterDuffColorFilter) filter).getColor();
        }
        if (filter instanceof BlendModeColorFilter) {
            return ((BlendModeColorFilter) filter).getColor();
        }
        if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        if (drawable instanceof ShapeDrawable) {
            final int paintColor = ((ShapeDrawable) drawable).getPaint().getColor();
            if (paintColor != Color.BLACK && paintColor != 0) {
                return paintColor;
            }
        }
        if (drawable instanceof GradientDrawable) {
            final ColorStateList colors = ((GradientDrawable) drawable).getColor();
            if (colors != null) {
                return colors.getDefaultColor();
            }
        }
        return sampleDrawableCenter(drawable);
    }

    @Nullable
    private static Integer sampleDrawableCenter(@NonNull Drawable drawable) {
        final Rect oldBounds = new Rect(drawable.getBounds());
        final Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, 16, 16);
        drawable.draw(canvas);
        drawable.setBounds(oldBounds);
        final int pixel = bitmap.getPixel(8, 8);
        bitmap.recycle();
        if (Color.alpha(pixel) == 0) {
            return null;
        }
        return pixel;
    }

    @Nullable
    private static Drawable copyDrawable(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        final Drawable.ConstantState state = drawable.getConstantState();
        if (state != null) {
            final Drawable copy = state.newDrawable();
            if (copy != null) {
                copy.mutate();
                if (drawable.getColorFilter() != null) {
                    copy.setColorFilter(drawable.getColorFilter());
                }
                return copy;
            }
        }
        return drawable;
    }
}
