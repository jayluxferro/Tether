package com.koushikdutta.tether;

import android.app.Activity;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;

public class ActivityBase extends Activity {
    private static final String LOGTAG = ActivityBase.class.getSimpleName();
    MyAdapter mAdapter;
    HashMap<Integer, MyListAdapter> mAdapters = new HashMap();
    boolean mDestroyed = false;
    ListView mListView;

    /* renamed from: com.koushikdutta.tether.ActivityBase$1 */
    class C01591 implements OnItemClickListener {
        C01591() {
        }

        public void onItemClick(AdapterView<?> adapterView, View view, int arg2, long arg3) {
            ListItem li = (ListItem) ActivityBase.this.mAdapter.getItem(arg2);
            li.onClickInternal(view);
            li.onClick(view);
        }
    }

    /* renamed from: com.koushikdutta.tether.ActivityBase$2 */
    class C01602 implements OnItemLongClickListener {
        C01602() {
        }

        public boolean onItemLongClick(AdapterView<?> adapterView, View arg1, int arg2, long arg3) {
            return ((ListItem) ActivityBase.this.mAdapter.getItem(arg2)).onLongClick();
        }
    }

    class MyListAdapter extends ArrayAdapter<ListItem> {
        public MyListAdapter(Context context) {
            super(context, 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return ((ListItem) getItem(position)).getView(ActivityBase.this, convertView);
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return ((ListItem) getItem(position)).Enabled;
        }
    }

    class MyAdapter extends com.koushikdutta.tether.SeparatedListAdapter {
        public MyAdapter(Context context) {
            super(context);
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            if (super.isEnabled(position)) {
                return ((ListItem) getItem(position)).Enabled;
            }
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public MyListAdapter ensureHeader(int sectionName) {
        MyListAdapter adapter = (MyListAdapter) this.mAdapters.get(Integer.valueOf(sectionName));
        if (adapter != null) {
            return adapter;
        }
        adapter = new MyListAdapter(this);
        this.mAdapters.put(Integer.valueOf(sectionName), adapter);
        this.mAdapter.addSection(getString(sectionName), adapter);
        this.mListView.setAdapter(null);
        this.mListView.setAdapter(this.mAdapter);
        return adapter;
    }

    /* Access modifiers changed, original: protected */
    public ListItem addItem(int sectionName, ListItem item) {
        MyListAdapter adapter = (MyListAdapter) this.mAdapters.get(Integer.valueOf(sectionName));
        if (adapter == null) {
            adapter = new MyListAdapter(this);
            this.mAdapters.put(Integer.valueOf(sectionName), adapter);
            this.mAdapter.addSection(getString(sectionName), adapter);
            this.mListView.setAdapter(null);
            this.mListView.setAdapter(this.mAdapter);
        }
        adapter.add(item);
        return item;
    }

    /* Access modifiers changed, original: protected */
    public ListItem findItem(int item) {
        String text = getString(item);
        for (Adapter adapter : this.mAdapter.sections.values()) {
            MyListAdapter m = (MyListAdapter) adapter;
            for (int i = 0; i < m.getCount(); i++) {
                ListItem li = (ListItem) m.getItem(i);
                if (text.equals(li.Title)) {
                    return li;
                }
            }
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public boolean allowThemeOverride() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        if (VERSION.SDK_INT >= 11 && allowThemeOverride()) {
            setTheme(R.style.AppTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        this.mListView = (ListView) findViewById(R.id.listview);
        this.mListView.setOnItemClickListener(new C01591());
        this.mListView.setOnItemLongClickListener(new C01602());
        this.mAdapter = new MyAdapter(this);
        this.mListView.setAdapter(this.mAdapter);
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        this.mDestroyed = true;
    }

    public int getListItemResource() {
        return R.layout.list_item;
    }
}
