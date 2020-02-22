package ml.melun.mangaview.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.CommentsAdapter;

public class CommentsTabFragment extends Fragment {
    CommentsAdapter madapter;
    public CommentsTabFragment() {
    }
    public void setAdapter(CommentsAdapter adapter){
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
