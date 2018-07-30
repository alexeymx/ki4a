package com.staf621.ki4a;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class EndpointListAdaptor extends ArrayAdapter<EndpointItem> {

    public EndpointListAdaptor(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public EndpointListAdaptor(Context context, int resource, List<EndpointItem> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.endpoint_host_item, null);
        }

        EndpointItem e = getItem(position);

        if (e != null) {
            TextView ip_addr = (TextView) v.findViewById(R.id.ip);
            TextView host = (TextView) v.findViewById(R.id.hostname);
            ImageView icon = (ImageView) v.findViewById(R.id.icon);
            if (e.reachable) {
                int id = getContext().getResources().getIdentifier("on", "drawable", getContext().getPackageName());
                icon.setImageResource(id);
            }  else {
                int id = getContext().getResources().getIdentifier("off", "drawable", getContext().getPackageName());
                icon.setImageResource(id);
            }

            ip_addr.setText(e.ip);
            host.setText(e.host);

        }

        return v;
    }
}