package com.example.suren.photogallery;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class PhotoGalleryFragment extends Fragment implements FetchItemsTask.Listener {
    private static final int VIEW_COL_WIDTH = 120;

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayourManager;
    private List<GalleryItem> mItems = new ArrayList<GalleryItem>();
    private int mLastPage;

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private TextView mCaptionTextView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mCaptionTextView = (TextView)itemView;
        }

        public void bindGalleryItem(GalleryItem item) {
            mCaptionTextView.setText(item.getCaption());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mItems;

        public PhotoAdapter(List<GalleryItem> items) {
            mItems = items;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView textView = new TextView(getActivity());
            textView.setPadding(10, 10, 10, 10);
            return new PhotoHolder(textView);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            holder.bindGalleryItem(mItems.get(position));
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLastPage = 0;
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mLayourManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mRecyclerView.setLayoutManager(mLayourManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mLayourManager.findLastVisibleItemPosition() == mItems.size() - 1) {
                    new FetchItemsTask(PhotoGalleryFragment.this, mLastPage + 1).execute();
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
        new FetchItemsTask(this, mLastPage).execute();
        return view;
    }

    @Override
    public void onListFetched(List<GalleryItem> list, int page) {
        if (page == 0) {
            mItems = list;
        } else if (page > mLastPage) {
            mItems.addAll(list);
            mLastPage = page;
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
