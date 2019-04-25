package com.koushikdutta.tether;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class ListItem {
    public boolean CheckboxVisible;
    public ActivityBase Context;
    public boolean Enabled;
    public int Icon;
    public String Summary;
    public String Title;
    boolean checked;

    public void setEnabled(boolean enabled) {
        this.Enabled = enabled;
        this.Context.mAdapter.notifyDataSetChanged();
    }

    public void setTitle(int title) {
        if (title == 0) {
            setTitle(null);
        } else {
            setTitle(this.Context.getString(title));
        }
    }

    public void setTitle(String title) {
        this.Title = title;
        this.Context.mAdapter.notifyDataSetChanged();
    }

    public void setSummary(int summary) {
        if (summary == 0) {
            setSummary(null);
        } else {
            setSummary(this.Context.getString(summary));
        }
    }

    public void setSummary(String summary) {
        this.Summary = summary;
        this.Context.mAdapter.notifyDataSetChanged();
    }

    public ListItem(ActivityBase context, int title, int summary) {
        this.Enabled = true;
        this.CheckboxVisible = false;
        this.checked = false;
        if (title != 0) {
            this.Title = context.getString(title);
        }
        if (summary != 0) {
            this.Summary = context.getString(summary);
        }
        this.Context = context;
    }

    public ListItem(ActivityBase context, String title, String summary) {
        this.Enabled = true;
        this.CheckboxVisible = false;
        this.checked = false;
        this.Title = title;
        this.Summary = summary;
        this.Context = context;
    }

    public ListItem(ActivityBase context, int title, int summary, int icon) {
        this(context, title, summary);
        this.Icon = icon;
    }

    public ListItem(ActivityBase context, String title, String summary, int icon) {
        this(context, title, summary);
        this.Icon = icon;
    }

    public boolean getIsChecked() {
        return this.checked;
    }

    public void setIsChecked(boolean isChecked) {
        this.checked = isChecked;
        this.Context.mAdapter.notifyDataSetChanged();
    }

    public View getView(Context context, View convertView) {
        int i;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(this.Context.getListItemResource(), null);
        }
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView summary = (TextView) convertView.findViewById(R.id.summary);
        CheckBox cb = (CheckBox) convertView.findViewById(R.id.checkbox);
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(this.checked);
        final View cv = convertView;
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ListItem.this.checked = isChecked;
                ListItem.this.onClick(cv);
            }
        });
        if (this.CheckboxVisible) {
            i = 0;
        } else {
            i = 8;
        }
        cb.setVisibility(i);
        cb.setChecked(this.checked);
        title.setEnabled(this.Enabled);
        summary.setEnabled(this.Enabled);
        title.setText(this.Title);
        if (this.Summary != null) {
            summary.setVisibility(View.VISIBLE);
            summary.setText(this.Summary);
        } else {
            summary.setVisibility(View.GONE);
        }
        ImageView iv = (ImageView) convertView.findViewById(R.id.image);
        if (iv != null) {
            if (this.Icon != 0) {
                iv.setVisibility(View.VISIBLE);
                iv.setImageResource(this.Icon);
            } else {
                iv.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    /* Access modifiers changed, original: 0000 */
    public void onClickInternal(View view) {
        if (this.CheckboxVisible) {
            CheckBox cb = (CheckBox) view.findViewById(R.id.checkbox);
            cb.setChecked(!cb.isChecked());
        }
    }

    public void onClick(View view) {
    }

    public boolean onLongClick() {
        return false;
    }
}
