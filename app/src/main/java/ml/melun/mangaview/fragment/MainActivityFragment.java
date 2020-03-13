package ml.melun.mangaview.fragment;

import androidx.fragment.app.Fragment;
import ml.melun.mangaview.activity.MainActivity;

public class MainActivityFragment extends Fragment {
    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).hideProgressPanel();
    }
}
