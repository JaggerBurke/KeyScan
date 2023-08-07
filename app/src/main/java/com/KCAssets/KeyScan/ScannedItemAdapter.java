package com.KCAssets.KeyScan;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScannedItemAdapter extends ArrayAdapter<String> {

    private final List<String> scannedList;
    private final LayoutInflater inflater;
    private final SharedPreferences prefs;

    public ScannedItemAdapter(Context context, List<String> scannedList, SharedPreferences prefs) {
        super(context, 0, scannedList);
        this.scannedList = scannedList;
        inflater = LayoutInflater.from(context);
        this.prefs = prefs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_scanned, parent, false);
            holder = new ViewHolder();
            holder.itemTextView = convertView.findViewById(R.id.itemTextView);
            holder.removeButton = convertView.findViewById(R.id.removeButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String scannedItem = scannedList.get(position);
        holder.itemTextView.setText(scannedItem);
        holder.removeButton.setOnClickListener(v -> {
            // Remove item from the scanned list
            scannedList.remove(position);
            notifyDataSetChanged();

            // Update SharedPreferences
            updateSharedPreferences(scannedItem);
        });

        return convertView;
    }

    private void updateSharedPreferences(String removedItem) {
        Set<String> scannedIDsSet = prefs.getStringSet("IDs", new HashSet<>());

        // Create a mutable set and initialize it with the scanned IDs
        Set<String> mutableSet = new HashSet<>(scannedIDsSet);

        // Remove the desired item from the mutable set
        mutableSet.remove(removedItem);

        // Update SharedPreferences with the modified set
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("IDs", mutableSet);
        editor.apply();
    }

    static class ViewHolder {
        TextView itemTextView;
        Button removeButton;
    }
}

