package com.example.smartnoisemonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NoiseLogAdapter extends BaseAdapter {

    private final Context context;
    private final List<NoiseLog> logList;
    private final LayoutInflater inflater;

    public NoiseLogAdapter(Context context, List<NoiseLog> logList) {
        this.context = context;
        this.logList = logList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return logList.size();
    }

    @Override
    public Object getItem(int position) {
        return logList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView logTime;
        TextView logDetails;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.noise_log_item, parent, false);
            holder = new ViewHolder();
            holder.logTime = convertView.findViewById(R.id.logTime);
            holder.logDetails = convertView.findViewById(R.id.logDetails);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NoiseLog log = logList.get(position);
        if (log != null) {
            holder.logTime.setText(log.getTimestamp());
            holder.logDetails.setText(String.format("dB: %.2f\nLat: %.4f | Lng: %.4f",
                    log.getDecibel(), log.getLatitude(), log.getLongitude()));
        }

        return convertView;
    }
}
