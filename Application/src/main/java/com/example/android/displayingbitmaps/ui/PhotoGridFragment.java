/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.displayingbitmaps.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.android.displayingbitmaps.R;
import com.example.android.displayingbitmaps.database.DBHelper;
import com.example.android.displayingbitmaps.server.VolleyJsonObjectTask;
import com.example.android.displayingbitmaps.util.Constants;
import com.example.android.displayingbitmaps.util.ImageCache;
import com.example.android.displayingbitmaps.util.ImageFetcher;
import com.example.android.displayingbitmaps.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Khatri on 26/1/2015.
 */

public class PhotoGridFragment extends Fragment {
    private static final String IMAGE_CACHE_DIR = "thumbs";

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private PhotoGridAdapter mAdapter;
    private ImageFetcher mImageFetcher;
    private ArrayList<String> mPhotoIdsList = new ArrayList<String>();
    private ArrayList<PhotoBean> mPhotos = new ArrayList<PhotoBean>();

    private EditText mSearchEditText;
    private Button mSearchButton;
    private GridView mGridView;
    private ProgressBar mPhotoProgress;
    private DBHelper db;

    public PhotoGridFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        db = new DBHelper(getActivity());

        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new PhotoGridAdapter(getActivity());

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f);
        mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);

        //Register all the views and listener on them
        registerViews(v);
        registerListeners();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    /**
     * Register all the views
     * @param v
     */
    private void registerViews(View v) {
        mSearchEditText = (EditText) v.findViewById(R.id.search_edittext);
        mSearchButton = (Button) v.findViewById(R.id.search_button);
        mPhotoProgress = (ProgressBar) v.findViewById(R.id.photo_progress);
        mGridView = (GridView) v.findViewById(R.id.gridView);
    }

    /**
     * Register all the listeners
     */
    private void registerListeners() {
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchButtonClicked();
            }
        });
        mGridView.setAdapter(mAdapter);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    if (!Utils.hasHoneycomb()) {
                        mImageFetcher.setPauseWork(true);
                    }
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
            }
        });
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @TargetApi(VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =(mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (Utils.hasJellyBean()) {
                                    mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                } else {
                                    mGridView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Method to hide the keyboard
     */
    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
    }

    /**
     * Start searching
     */
    private void searchButtonClicked() {
        String text = mSearchEditText.getText().toString();
        if(TextUtils.isEmpty(text)) {
            Toast.makeText(getActivity(), "Nothing to search", Toast.LENGTH_SHORT).show();
        } else {
            hideKeyboard();
            mPhotoIdsList.clear();
            mPhotos.clear();
            mAdapter.notifyDataSetChanged();
            getPhotoIdApiCall(text);
        }
    }

    private void getPhotoIdApiCall(final String text) {
        mPhotoProgress.setVisibility(View.VISIBLE);
        String url = Constants.FLICKR_BASE_URL + Constants.FLICKR_PHOTO_SEARCH_URL + "&api_key=" + Constants.FLICKR_API_KEY + "&text=" + text + "&sort=relevance&page=1" + Constants.FLICKR_URL_SUFFIX;
        VolleyJsonObjectTask volleyJsonObjectTask = new VolleyJsonObjectTask(getActivity(), url, null, new VolleyJsonObjectTask.Callback() {

            @Override
            public void callSuccess(JSONObject result) {
                getPhotoIds(result, text);
            }

            @Override
            public boolean callFailed(Exception e) {
                mPhotoProgress.setVisibility(View.GONE);
                fetchFromCache(text);
                return super.callFailed(e);
            }
        });
        volleyJsonObjectTask.execute();
    }

    private void getPhotoIds(JSONObject result, String search) {
        try {
            String status = result.getString("stat");
            if(status.equalsIgnoreCase("ok")) {
                JSONObject photosObject = result.getJSONObject("photos");
                JSONArray photoArray = photosObject.getJSONArray("photo");
                for(int i=0; i<photoArray.length(); i++) {
                    JSONObject item = photoArray.getJSONObject(i);
                    String photoId = item.getString("id");
                    mPhotoIdsList.add(photoId);
                }
                getPhotoUrlApiCall(search);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getPhotoUrlApiCall(final String search) {
        if(mPhotoIdsList.size() > 0) {
            for(int i=0; i<mPhotoIdsList.size(); i++) {
                final String id = mPhotoIdsList.get(i);
                String url = Constants.FLICKR_BASE_URL + Constants.FLICKR_PHOTO_SIZES_URL + "&api_key=" + Constants.FLICKR_API_KEY + "&photo_id=" + id + Constants.FLICKR_URL_SUFFIX;
                VolleyJsonObjectTask volleyJsonObjectTask = new VolleyJsonObjectTask(getActivity(), url, null, new VolleyJsonObjectTask.Callback() {

                    @Override
                    public void callSuccess(JSONObject result) {
                        getPhotoUrl(result, id, search);
                    }

                    @Override
                    public boolean callFailed(Exception e) {
                        mPhotoProgress.setVisibility(View.GONE);
                        return super.callFailed(e);
                    }
                });
                volleyJsonObjectTask.execute();
            }
        }
    }

    private void getPhotoUrl(JSONObject result, final String id, String search) {
        try {
            String status = result.getString("stat");
            if(status.equalsIgnoreCase("ok")) {
                JSONObject photoObject = result.getJSONObject("sizes");
                JSONArray photoArray = photoObject.getJSONArray("size");
                for(int i=0; i<photoArray.length(); i++) {
                    JSONObject item = photoArray.getJSONObject(i);
                    String photoLabel = item.getString("label");
                    if(photoLabel.equalsIgnoreCase("Large Square")) {
                        String photoSource = item.getString("source").replace("\"", "");
                        PhotoBean photo = new PhotoBean();
                        photo.setPhotoid(id);
                        photo.setPhotourl(photoSource);
                        photo.setPhotosearch(search);
                        mPhotos.add(photo);
                        db.insertPhotoRecordIfNotExist(id, photoSource, search);
                        mPhotoProgress.setVisibility(View.GONE);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchFromCache(String search) {
        ArrayList<PhotoBean> photoList = db.getAllPhotos(search);
        for(int i=0; i<photoList.size(); i++) {
            PhotoBean photo = new PhotoBean();
            photo.setPhotoid(photoList.get(i).getPhotoid());
            photo.setPhotourl(photoList.get(i).getPhotourl());
            photo.setPhotosearch(photoList.get(i).getPhotosearch());
            mPhotos.add(photo);
        }
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Photo grid adapter
     */
    private class PhotoGridAdapter extends BaseAdapter {

        private final Context mContext;
        private int mItemHeight = 0;
        private int mNumColumns = 0;
        private int mActionBarHeight = 0;
        private GridView.LayoutParams mImageViewLayoutParams;

        public PhotoGridAdapter(Context context) {
            super();
            mContext = context;
            mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            // Calculate ActionBar height
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
        }

        @Override
        public int getCount() {
            if (getNumColumns() == 0) {
                return 0;
            }
            return mPhotos.size();
        }

        @Override
        public Object getItem(int position) {
            return position < mNumColumns ? null : mPhotos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new CustomImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(mImageViewLayoutParams);
            } else {
                imageView = (ImageView) convertView;
            }

            // Check the height matches our calculated column width
            if (imageView.getLayoutParams().height != mItemHeight) {
                imageView.setLayoutParams(mImageViewLayoutParams);
            }

            String url = mPhotos.get(position).getPhotourl();
            mImageFetcher.loadImage(url, imageView);
            return imageView;
        }

        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }
            mItemHeight = height;
            mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
            mImageFetcher.setImageSize(height);
            notifyDataSetChanged();
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }
}
