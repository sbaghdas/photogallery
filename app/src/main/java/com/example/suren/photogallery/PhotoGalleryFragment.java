package com.example.suren.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.HttpAuthHandler;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends Fragment implements FetchItemsTask.Listener {
    private static final int VIEW_COL_WIDTH = 240;
    private static final int PRELOAD_ITEM_COUNT = 50;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayourManager;
    private List<GalleryItem> mItems = new ArrayList<GalleryItem>();
    private int mLastPage;
    private boolean mFetching;
    private String mSearchString;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(View view) {
            super(view);
            mImageView = (ImageView)view.findViewById(R.id.gallery_item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mItems;

        public PhotoAdapter(List<GalleryItem> items) {
            mItems = items;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem item = mItems.get(position);
            holder.bindDrawable(null);
            mThumbnailDownloader.queueThumbnail(holder, item.getUrl());
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    public PhotoGalleryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // new handler for UI thread
        mThumbnailDownloader = new ThumbnailDownloader<>(new Handler());
        mThumbnailDownloader.setThumbnailDownloaderListener(
                new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownload(PhotoHolder target, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                target.bindDrawable(drawable);
            }
        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper(); // make sure Looper is created
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        MenuItem searchMenu = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView)searchMenu.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mSearchString = query;
                mLastPage = 0;
                fetchItems(mLastPage);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        MenuItem clearMenu = menu.findItem(R.id.menu_item_clear);
        clearMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mSearchString = null;
                mLastPage = 0;
                fetchItems(mLastPage);
                return false;
            }
        });
    }

    private void fetchItems(int page) {
        mFetching = true;
        new FetchItemsTask(PhotoGalleryFragment.this, mSearchString, page).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mSearchString = null;
        mLastPage = 0;
        mFetching = false;
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mLayourManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mRecyclerView.setLayoutManager(mLayourManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastItem = mLayourManager.findLastVisibleItemPosition();
                if (!mFetching) {
                    if (lastItem == mItems.size() - 1) {
                        fetchItems(mLastPage + 1);
                    } else {
                        int firstItem = mLayourManager.findFirstVisibleItemPosition();
                        preload(firstItem - PRELOAD_ITEM_COUNT, PRELOAD_ITEM_COUNT);
                        preload(lastItem, PRELOAD_ITEM_COUNT);
                    }
                }
            }
        });
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLayourManager.setSpanCount(mRecyclerView.getWidth() / VIEW_COL_WIDTH);
            }
        });
        setupAdapter();
        setRetainInstance(true);
        fetchItems(mLastPage);
        return view;
    }

    private void preload(int position, int count) {
        if (position < 0) {
            position = 0;
        }
        int end = position + count;
        if (end > mItems.size()) {
            end = mItems.size();
        }
        for (int i = position; i < end; i++) {
            mThumbnailDownloader.queueThumbnail(null, mItems.get(i).getUrl());
        }
    }

    @Override
    public void onListFetched(List<GalleryItem> list, int page) {
        if (list.size() == 0) {
            return;
        }
        mFetching = false;
        if (page == 0) {
            mItems = list;
        } else if (page > mLastPage) {
            mItems.addAll(list);
            mLastPage = page;
        } else {
            return;
        }
        int position = mLayourManager.findFirstCompletelyVisibleItemPosition();
        setupAdapter();
        mRecyclerView.scrollToPosition(position);
    }

    private void setupAdapter() {
        // this check is necessary because PhotoAdapter uses getActivity() which might be null
        // if fragment was not attached to activity yet
        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
}
