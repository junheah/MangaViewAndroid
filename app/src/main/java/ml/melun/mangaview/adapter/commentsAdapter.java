package ml.melun.mangaview.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.util.ArrayList;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Comment;

public class commentsAdapter extends BaseAdapter {
    Context context;
    ArrayList<Comment> data;
    LayoutInflater inflater;
    Boolean dark;
    Boolean save;
    public commentsAdapter(Context context, ArrayList<Comment> data) {
        super();
        this.dark = new Preference().getDarkTheme();
        this.save = new Preference().getDataSave();
        this.context = context;
        this.data = data;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView = inflater.inflate(R.layout.item_comment,parent,false);
        }
        Comment c = data.get(position);
        ImageView icon = convertView.findViewById(R.id.comment_icon);
        TextView content = convertView.findViewById(R.id.comment_content);
        TextView timeStamp = convertView.findViewById(R.id.comment_time);
        TextView user = convertView.findViewById(R.id.comment_user);
        if(c.getIcon().length()>1 && !save) Glide.with(context).load(c.getIcon()).into(icon);
        content.setText(c.getContent());
        timeStamp.setText(c.getTimestamp());
        user.setText(c.getUser());
        return convertView;
    }

    @Override
    public Comment getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
}
