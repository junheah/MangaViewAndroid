package ml.melun.mangaview.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Manga;

public class CustomSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private Context context;
    private List<Manga> data;
    private static LayoutInflater inflater = null;
    private CustomSpinnerListener listener;
    private int selected = -1;

    public CustomSpinnerAdapter(Context context) {
        this.context = context;
        this.selected = -1;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        if(data != null)
            return data.size();
        return 0;
    }

    public void setData(List<Manga> data, Manga m){
        this.data = data;
        setSelection(m);
        notifyDataSetChanged();
    }

    public void setSelection(int position){
        if(position != selected)
            this.selected = position;
    }

    public void setSelection(Manga m){
        for(int i=0; i< data.size(); i++){
            if(m.equals(data.get(i)))
                setSelection(i);
        }
    }
    public int getSelected(){
        return selected;
    }
    public Object getItem(int position) {
        return data.get(position);
    }

    public long getItemId(int position) {
        return data.get(position).getId();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_custom_spinner, parent, false);
        }
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        try {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_custom_spinner_dropdown, null);
                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.spinner_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if(position == selected){
                holder.name.setTextColor(Color.argb(255,230,160,220));
            }else{
                holder.name.setTextColor(Color.WHITE);
            }

            holder.name.setText(data.get(position).getName());
            holder.name.setSelected(true);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener != null && selected != position) {
                        listener.onClick(data.get(position), position);
                        setSelection(position);
                    }
                }
            });

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return convertView;
    }

    public void setListener(CustomSpinnerListener listener){
        this.listener = listener;
    }

    private class ViewHolder {
        private TextView name;
    }

    public interface CustomSpinnerListener{
        void onClick(Manga m, int i);
    }
}
