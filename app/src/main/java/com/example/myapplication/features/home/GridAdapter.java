package com.example.myapplication.features.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;

public class GridAdapter extends BaseAdapter {

    Context context;
    String[] items;
    int[] images;
    boolean[] locked;

    public GridAdapter(Context context, String[] items, int[] images, boolean[] locked) {
        this.context = context;
        this.items = items;
        this.images = images;
        this.locked = locked;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_home_menu_tile, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.gridImage);
        TextView textView = convertView.findViewById(R.id.gridText);
        ImageView imgLock = convertView.findViewById(R.id.imgLock);
        View tileContainer = convertView.findViewById(R.id.tileContainer);

        imageView.setImageResource(images[position]);
        textView.setText(items[position]);

        boolean isLocked = locked != null && position < locked.length && locked[position];
        imgLock.setVisibility(isLocked ? View.VISIBLE : View.GONE);

        if (isLocked) {
            tileContainer.setBackgroundResource(R.drawable.bg_home_icon_box);
            tileContainer.setAlpha(0.3f);
            textView.setTextColor(0xFFBBBBBB);
        } else {
            tileContainer.setBackgroundResource(R.drawable.bg_home_icon_box);
            tileContainer.setAlpha(1.0f);
            textView.setTextColor(0xFF444444);
        }

        return convertView;
    }
}