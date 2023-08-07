package com.KCAssets.KeyScan;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;

    ArrayList<String> listID;
    ArrayList<String> listDescript;

    ArrayList<String> listPastDue;

    public CustomAdapter(Context context, ArrayList<String> listID, ArrayList<String> listDescript, ArrayList<String> listPastDue){
        this.context=context;
        this.listID=listID;
        this.listDescript=listDescript;
        this.listPastDue=listPastDue;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return listID.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewgroup) {

        view = inflater.inflate(R.layout.confirm_list, null);
        TextView tvID = view.findViewById(R.id.tv_lv_id);
        TextView tvDescript = view.findViewById(R.id.tv_lv_descript);
        Button btnRemove = view.findViewById(R.id.removeBtn);

        tvID.setText(listID.get(i));
        tvDescript.setText(listDescript.get(i));

        if (listPastDue.get(i).equals("1")) {
            // Apply a red background color or any other visual indicator
            view.setBackgroundColor(context.getResources().getColor(R.color.pastDueColor));
        }

        // Add click listener for the remove button
        btnRemove.setOnClickListener(v -> {
            // Remove the item and update the arrays
            listID.remove(i);
            listDescript.remove(i);
            listPastDue.remove(i);
            notifyDataSetChanged();
        });

        return view;
    }
}
