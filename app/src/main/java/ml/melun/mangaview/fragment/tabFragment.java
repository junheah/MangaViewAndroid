package ml.melun.mangaview.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.commentsAdapter;
import ml.melun.mangaview.mangaview.Comment;

public class tabFragment extends Fragment {
    commentsAdapter madapter;
    public tabFragment() {
    }
    public void setAdapter(commentsAdapter adapter){
        madapter = adapter;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_test, container, false);
        ListView list = rootView.findViewById(R.id.section_list);
        list.setAdapter(madapter);
        return rootView;
    }
}
