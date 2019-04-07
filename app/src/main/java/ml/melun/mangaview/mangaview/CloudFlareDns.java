package ml.melun.mangaview.mangaview;

import android.os.AsyncTask;
import android.util.Log;

import org.xbill.DNS.Address;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import okhttp3.Dns;

public class CloudFlareDns implements Dns {
    private static final String LIVE_API_HOST = "manamoa.net";
    private static final String LIVE_API_IP = "104.27.129.225";

    private InetAddress mLiveApiStaticIpAddress;

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        try {
            System.out.println("ppp"+Address.getByName(hostname).getHostAddress());
            return Collections.singletonList(Address.getByName(hostname));
        } catch (UnknownHostException e) {
            // fallback to the API's static IP
            if (LIVE_API_HOST.equals(hostname) && mLiveApiStaticIpAddress != null) {
                System.out.println("pppp"+mLiveApiStaticIpAddress.getHostAddress());
                return Collections.singletonList(mLiveApiStaticIpAddress);
            } else {
                throw e;
            }
        }
    }

    public void init() {
        try {
            mLiveApiStaticIpAddress = InetAddress.getByName(LIVE_API_IP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            Resolver cfFirstResolver = new SimpleResolver("1.1.1.1");
            Lookup.setDefaultResolver(cfFirstResolver);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


    public CloudFlareDns(){
        //init();
    }
}
