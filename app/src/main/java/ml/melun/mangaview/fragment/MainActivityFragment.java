package ml.melun.mangaview.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainActivityFragment extends Fragment{
    boolean loaded = false;
    boolean force = false;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loaded = true;
        if(force){
            postDrawerJob();
            force = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        loaded = false;
    }

    public void drawerClosed() {
        if(!loaded)
            force = true;
        else {
            postDrawerJob();
            loaded = false;
        }
    }

    public void postDrawerJob(){

    }
}
