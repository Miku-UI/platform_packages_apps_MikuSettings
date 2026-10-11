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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.core.widget.NestedScrollView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.SettingsActivity;
import com.android.settings.homepage.HighlightableMenu;
import com.android.settings.homepage.TopLevelSettings;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import com.miku.android.settings.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Homepage list as an adaptive card grid. Destinations stay on TopLevelSettings XML.
 */
public class MikuTopLevelSettings extends TopLevelSettings {

    private static final String EXTRA_GROUP_KEY = "miku_homepage_group_key";
    private static final String EXTRA_ORIG_ORDER = "miku_homepage_orig_order";

    private MikuHomepagePreferenceAdapter mAdapter;
    private MikuHomepageGridLayoutManager mGridLayoutManager;

    @Override
    public String getCategoryKey() {
        return CategoryKey.CATEGORY_HOMEPAGE;
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        final RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent,
                savedInstanceState);
        recyclerView.setItemAnimator(null);
        recyclerView.setClipToPadding(false);
        recyclerView.setClipChildren(false);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        final ViewGroup.LayoutParams listParams = recyclerView.getLayoutParams();
        if (listParams != null) {
            listParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            recyclerView.setLayoutParams(listParams);
        }

        mGridLayoutManager = new MikuHomepageGridLayoutManager(recyclerView.getContext());
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mAdapter != null
                        && mAdapter.preferenceAt(position) instanceof PreferenceCategory) {
                    return Math.max(1, mGridLayoutManager.getSpanCount());
                }
                return 1;
            }
        });
        recyclerView.setLayoutManager(mGridLayoutManager);
        recyclerView.addItemDecoration(new MikuHomepageGridSpacing(recyclerView.getContext()));
        applyGridPadding(/* extraHorizontal= */ 0);
        tintHomepageScrollBackground(recyclerView);
        attachViewportTracking(recyclerView);
        return recyclerView;
    }

    @Override
    public void setPaddingHorizontal(int padding) {
        super.setPaddingHorizontal(padding);
        applyGridPadding(padding);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        flattenHomepagePreferences(getPreferenceScreen());
    }

    @Override
    public void onCategoriesChanged(Set<String> categories) {
        super.onCategoriesChanged(categories);
        flattenHomepagePreferences(getPreferenceScreen());
    }

    @Override
    protected void refreshDashboardTiles(String tag) {
        super.refreshDashboardTiles(tag);
        flattenHomepagePreferences(getPreferenceScreen());
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        styleHomepagePreferences(getPreferenceScreen());
    }

    @Override
    protected Preference createPreference(Tile tile) {
        final Preference preference = super.createPreference(tile);
        preference.setLayoutResource(R.layout.miku_homepage_card);
        if (tile.hasGroupKey()) {
            preference.getExtras().putString(EXTRA_GROUP_KEY, tile.getGroupKey());
        }
        return preference;
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        flattenHomepagePreferences(preferenceScreen);
        mAdapter = new MikuHomepagePreferenceAdapter(preferenceScreen, this);
        return mAdapter;
    }

    @Override
    public void setHighlightPreferenceKey(String prefKey) {
        super.setHighlightPreferenceKey(prefKey);
        if (mAdapter != null) {
            mAdapter.highlightPreference(prefKey, /* scrollNeeded= */ false);
        }
    }

    @Override
    public boolean isDuplicateClick(Preference pref) {
        return mAdapter != null
                && TextUtils.equals(pref.getKey(), mAdapter.getHighlightKey())
                && isActivityEmbedded();
    }

    @Override
    public void setMenuHighlightShowed(boolean show) {
        super.setMenuHighlightShowed(show);
        if (mAdapter != null) {
            mAdapter.setMenuHighlightShowed(show);
        }
    }

    @Override
    public void setHighlightMenuKey(String menuKey, boolean scrollNeeded) {
        super.setHighlightMenuKey(menuKey, scrollNeeded);
        if (mAdapter != null) {
            mAdapter.highlightPreference(HighlightableMenu.lookupPreferenceKey(menuKey),
                    scrollNeeded);
        }
    }

    @Override
    public void highlightPreferenceIfNeeded() {
        super.highlightPreferenceIfNeeded();
        if (mAdapter != null) {
            mAdapter.requestHighlight();
        }
    }

    @Override
    protected void reloadHighlightMenuKey() {
        super.reloadHighlightMenuKey();
        if (mAdapter == null || getArguments() == null) {
            return;
        }
        final String menuKey = getArguments().getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY);
        mAdapter.highlightPreference(HighlightableMenu.lookupPreferenceKey(menuKey),
                /* scrollNeeded= */ true);
    }

    private void tintHomepageScrollBackground(View start) {
        final TypedValue value = new TypedValue();
        start.getContext().getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
        final int color = value.data;
        ViewParent parent = start.getParent();
        while (parent instanceof View) {
            final View view = (View) parent;
            view.setBackgroundColor(color);
            if (view instanceof NestedScrollView) {
                break;
            }
            parent = parent.getParent();
        }
    }

    private void attachViewportTracking(RecyclerView recyclerView) {
        recyclerView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if (mAdapter != null) {
                        mAdapter.scheduleViewportPass();
                    }
                });
        recyclerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                final NestedScrollView scroll = homepageScroll(v);
                if (scroll == null) {
                    return;
                }
                scroll.setOnScrollChangeListener((NestedScrollView nested,
                        int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
                    if (mAdapter != null) {
                        mAdapter.onViewportChanged();
                    }
                });
                if (mAdapter != null) {
                    mAdapter.scheduleViewportPass();
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                final NestedScrollView scroll = homepageScroll(v);
                if (scroll != null) {
                    scroll.setOnScrollChangeListener(
                            (NestedScrollView.OnScrollChangeListener) null);
                }
            }
        });
    }

    @Nullable
    private static NestedScrollView homepageScroll(View start) {
        ViewParent parent = start.getParent();
        while (parent instanceof View) {
            if (parent instanceof NestedScrollView) {
                return (NestedScrollView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void relayoutHomepageGrid() {
        final RecyclerView recyclerView = getListView();
        if (recyclerView == null) {
            return;
        }
        recyclerView.requestLayout();
        ViewParent parent = recyclerView.getParent();
        while (parent instanceof View) {
            final View view = (View) parent;
            view.requestLayout();
            if (view instanceof NestedScrollView) {
                break;
            }
            parent = parent.getParent();
        }
        if (mAdapter != null) {
            mAdapter.scheduleViewportPass();
        }
    }

    private void applyGridPadding(int extraHorizontal) {
        final RecyclerView recyclerView = getListView();
        if (recyclerView == null) {
            return;
        }
        final int grid = getResources().getDimensionPixelSize(R.dimen.miku_homepage_grid_padding);
        final int top = getResources().getDimensionPixelSize(R.dimen.miku_homepage_grid_padding_top);
        recyclerView.setPadding(grid + extraHorizontal, top, grid + extraHorizontal,
                getResources().getDimensionPixelSize(R.dimen.miku_homepage_grid_padding_bottom));
    }

    /**
     * Move injected tiles out of PreferenceCategory onto the screen so the grid can place them.
     * Empty categories stay on the screen (hidden) so later dashboard refreshes can still find
     * the group key. Tile order follows the XML category order, not the injected tile order.
     */
    private void flattenHomepagePreferences(@Nullable PreferenceScreen screen) {
        if (screen == null) {
            return;
        }
        final List<PreferenceCategory> categories = new ArrayList<>();
        final int screenCount = screen.getPreferenceCount();
        for (int i = 0; i < screenCount; i++) {
            final Preference preference = screen.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                categories.add((PreferenceCategory) preference);
            }
        }
        categories.sort(Comparator.comparingInt(Preference::getOrder));

        final List<Preference> tiles = new ArrayList<>();
        collectTiles(screen, tiles);
        for (int i = 0; i < tiles.size(); i++) {
            final Preference tile = tiles.get(i);
            if (!tile.getExtras().containsKey(EXTRA_ORIG_ORDER)) {
                tile.getExtras().putInt(EXTRA_ORIG_ORDER, tile.getOrder());
            }
            final PreferenceGroup parent = tile.getParent();
            if (parent instanceof PreferenceCategory && parent.getKey() != null
                    && !tile.getExtras().containsKey(EXTRA_GROUP_KEY)) {
                tile.getExtras().putString(EXTRA_GROUP_KEY, parent.getKey());
            }
        }

        final Map<String, List<Preference>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            grouped.put(categories.get(i).getKey(), new ArrayList<>());
        }
        final List<Preference> ungrouped = new ArrayList<>();
        for (int i = 0; i < tiles.size(); i++) {
            final Preference tile = tiles.get(i);
            final String groupKey = tile.getExtras().getString(EXTRA_GROUP_KEY);
            final List<Preference> bucket =
                    groupKey != null ? grouped.get(groupKey) : null;
            if (bucket != null) {
                bucket.add(tile);
            } else {
                ungrouped.add(tile);
            }
        }
        final Comparator<Preference> byOrigOrder =
                Comparator.comparingInt(MikuTopLevelSettings::originalOrder);
        for (List<Preference> bucket : grouped.values()) {
            bucket.sort(byOrigOrder);
        }
        ungrouped.sort(byOrigOrder);
        final List<Preference> ordered = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            ordered.addAll(grouped.get(categories.get(i).getKey()));
        }
        ordered.addAll(ungrouped);

        for (int i = 0; i < ordered.size(); i++) {
            final Preference tile = ordered.get(i);
            tile.setLayoutResource(R.layout.miku_homepage_card);
            final PreferenceGroup parent = tile.getParent();
            if (parent instanceof PreferenceCategory) {
                parent.removePreference(tile);
                screen.addPreference(tile);
            }
            tile.setOrder(i);
        }
        for (int i = 0; i < categories.size(); i++) {
            categories.get(i).setVisible(false);
        }
        relayoutHomepageGrid();
    }

    private static int originalOrder(Preference preference) {
        if (preference.getExtras().containsKey(EXTRA_ORIG_ORDER)) {
            return preference.getExtras().getInt(EXTRA_ORIG_ORDER);
        }
        return preference.getOrder();
    }

    private static void collectTiles(PreferenceGroup group, List<Preference> out) {
        final int count = group.getPreferenceCount();
        final Preference[] children = new Preference[count];
        for (int i = 0; i < count; i++) {
            children[i] = group.getPreference(i);
        }
        for (int i = 0; i < children.length; i++) {
            final Preference preference = children[i];
            if (preference instanceof PreferenceCategory) {
                collectTiles((PreferenceGroup) preference, out);
            } else {
                out.add(preference);
            }
        }
    }

    private void styleHomepagePreferences(@Nullable PreferenceGroup group) {
        if (group == null) {
            return;
        }
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference preference = group.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                styleHomepagePreferences((PreferenceGroup) preference);
            } else {
                preference.setLayoutResource(R.layout.miku_homepage_card);
            }
        }
    }
}
