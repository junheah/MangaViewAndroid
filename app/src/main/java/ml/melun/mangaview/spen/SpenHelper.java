package ml.melun.mangaview.spen;

import android.content.Context;

import com.samsung.android.sdk.penremote.SpenEventListener;
import com.samsung.android.sdk.penremote.SpenRemote;
import com.samsung.android.sdk.penremote.SpenUnit;
import com.samsung.android.sdk.penremote.SpenUnitManager;

public class SpenHelper {
  SpenUnitManager mSpenUnitManager;

  public SpenHelper(Context context, SpenEventListener listener) {
    SpenRemote spenRemote = SpenRemote.getInstance();
    if (!spenRemote.isConnected()) {
      spenRemote.connect(
          context,
          new SpenRemote.ConnectionResultCallback() {
            @Override
            public void onSuccess(SpenUnitManager manager) {
              mSpenUnitManager = manager;
                SpenUnit button = mSpenUnitManager.getUnit(SpenUnit.TYPE_BUTTON);
                mSpenUnitManager.registerSpenEventListener(listener, button);
            }

            @Override
            public void onFailure(int error) {
                mSpenUnitManager = null;
            }
          });
    }
  }

  public void disconnect(Context context) {
      if(mSpenUnitManager != null)SpenRemote.getInstance().disconnect(context);
  }
}
