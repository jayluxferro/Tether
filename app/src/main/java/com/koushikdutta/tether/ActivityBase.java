package com.koushikdutta.tether;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.HashMap;

public class ActivityBase extends Activity {
    MyAdapter mAdapter;
    HashMap<Integer, MyListAdapter> mAdapters = new HashMap<>();
    boolean mDestroyed = false;
    ListView mListView;

    class MyAdapter extends SeparatedListAdapter {
        public MyAdapter(Context context) {
            super(context);
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            if (!super.isEnabled(position)) {
                return false;
            }
            return ((ListItem) getItem(position)).Enabled;
        }
    }

    class MyListAdapter extends ArrayAdapter<ListItem> {
        public MyListAdapter(Context context) {
            super(context, 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return (getItem(position)).getView(ActivityBase.this, convertView);
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return (getItem(position)).Enabled;
        }
    }

    public MyListAdapter ensureHeader(int sectionName) {
        MyListAdapter adapter = this.mAdapters.get(sectionName);
        if (adapter != null) {
            return adapter;
        }
        MyListAdapter adapter2 = new MyListAdapter(this);
        this.mAdapters.put(sectionName, adapter2);
        this.mAdapter.addSection(getString(sectionName), adapter2);
        this.mListView.setAdapter((ListAdapter) null);
        this.mListView.setAdapter(this.mAdapter);
        return adapter2;
    }

    /* access modifiers changed from: protected */
    public ListItem addItem(int sectionName, ListItem item) {
        MyListAdapter adapter = this.mAdapters.get(sectionName);
        if (adapter == null) {
            adapter = new MyListAdapter(this);
            this.mAdapters.put(sectionName, adapter);
            this.mAdapter.addSection(getString(sectionName), adapter);
            this.mListView.setAdapter((ListAdapter) null);
            this.mListView.setAdapter(this.mAdapter);
        }
        adapter.add(item);
        return item;
    }

    /* access modifiers changed from: protected */
    public ListItem findItem(int item) {
        String text = getString(item);
        for (Adapter adapter : this.mAdapter.sections.values()) {
            MyListAdapter m = (MyListAdapter) adapter;
            int i = 0;
            while (true) {
                if (i < m.getCount()) {
                    ListItem li = m.getItem(i);
                    if (text.equals(li.Title)) {
                        return li;
                    }
                    i++;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public boolean allowThemeOverride() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        if (allowThemeOverride()) {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        this.mListView = findViewById(R.id.listview);
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int arg2, long arg3) {
                ListItem li = (ListItem) ActivityBase.this.mAdapter.getItem(arg2);
                li.onClickInternal(view);
                li.onClick(view);
            }
        });
        this.mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View arg1, int arg2, long arg3) {
                return ((ListItem) ActivityBase.this.mAdapter.getItem(arg2)).onLongClick();
            }
        });
        this.mAdapter = new MyAdapter(this);
        this.mListView.setAdapter(this.mAdapter);
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
    }

    public int getListItemResource() {
        return R.layout.list_item;
    }
}