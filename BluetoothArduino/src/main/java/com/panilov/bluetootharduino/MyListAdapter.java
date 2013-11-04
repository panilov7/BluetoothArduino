package com.panilov.bluetootharduino;

import android.content.Context;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by peter on 11/3/13.
 */
public class MyListAdapter extends ArrayAdapter<Item> {
    private Context context;
    private ArrayList<Item> items;

    public MyListAdapter(Context context, int resource, ArrayList<Item> objects) {
        super(context, resource, objects);
        this.context = context;
        items = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = convertView;

        if (itemView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            itemView = inflater.inflate(R.layout.list_item, parent, false);

        }

        Item currentItem = items.get(position);

        TextView tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
        TextView tvDesc = (TextView) itemView.findViewById(R.id.tvDescription);

        tvTitle.setText(currentItem.getTitle());
        tvDesc.setText(currentItem.getDescription());

        return itemView;
    }
}
