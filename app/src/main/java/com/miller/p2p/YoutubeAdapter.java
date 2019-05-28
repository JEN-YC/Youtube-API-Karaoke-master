package com.miller.p2p;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;

import java.util.List;

class YoutubeAdapter extends BaseAdapter {
    private List<VideoPojo> list;
    private Context mcontext;
    private LayoutInflater inflater;

    public YoutubeAdapter(Context context, List<VideoPojo> mlist) {
        this.list = mlist;
        this.mcontext = context;
        this.inflater = LayoutInflater.from(mcontext);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.list_item_row, null);

            holder.thumbnail = convertView
                    .findViewById(R.id.video_thumbnail);
            holder.title = convertView
                    .findViewById(R.id.video_title);
            convertView.setTag(holder);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        VideoPojo searchResult = list.get(position);

        Picasso.get()
                .load(searchResult.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(searchResult.getTitle());

        return convertView;
    }

    public class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView description;
    }

}