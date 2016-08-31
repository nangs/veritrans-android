package id.co.veritrans.sdk.coreflow.core;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.sql.Date;
import java.util.concurrent.TimeUnit;

import id.co.veritrans.sdk.coreflow.BuildConfig;
import id.co.veritrans.sdk.coreflow.analytics.MixpanelApi;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by chetan on 16/10/15.
 */
public class VeritransRestAdapter {
    private static final RestAdapter.LogLevel LOG_LEVEL = BuildConfig.FLAVOR.equalsIgnoreCase("development") ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE;
    private static final String TAG = VeritransRestAdapter.class.getName();

    /**
     * It will return instance of PaymentAPI using that we can execute api calls.
     *
     * @param baseUrl base URL of PAPI
     * @return Payment API implementation
     */
    public static VeritransRestAPI getVeritransApiClient(String baseUrl, int timeout) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(baseUrl)
                .setConverter(new GsonConverter(gson))
                .setLogLevel(LOG_LEVEL)
                .setClient(new OkClient(okHttpClient));
        RestAdapter restAdapter = builder.build();
        return restAdapter.create(VeritransRestAPI.class);
    }

    /**
     * Create Merchant API implementation
     *
     * @param merchantBaseURL Merchant base URL
     * @return Merchant API implementation
     */
    public static MerchantRestAPI getMerchantApiClient(String merchantBaseURL, int timeout) {

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(timeout, TimeUnit.SECONDS);
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setConverter(new GsonConverter(gson))
                .setLogLevel(LOG_LEVEL)
                .setClient(new OkClient(okHttpClient))
                .setEndpoint(merchantBaseURL);
        RestAdapter restAdapter = builder.build();
        return restAdapter.create(MerchantRestAPI.class);

    }


    /**
     * Create Mixpanel API
     *
     * @return mixpanel Api implementation
     */
    public static MixpanelApi getMixpanelApi(int timeout) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(timeout, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(timeout, TimeUnit.SECONDS);
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setLogLevel(LOG_LEVEL)
                .setClient(new OkClient(okHttpClient))
                .setEndpoint(BuildConfig.MIXPANEL_URL);
        RestAdapter restAdapter = builder.build();
        return restAdapter.create(MixpanelApi.class);
    }

    /**
     * Crate Snap API
     *
     * @param snapBaseURL base URL of snap API
     * @return snap API implementation
     */
    public static SnapRestAPI getSnapRestAPI(String snapBaseURL, int timeOut) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(timeOut, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(timeOut, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(timeOut, TimeUnit.SECONDS);
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setLogLevel(LOG_LEVEL)
                .setClient(new OkClient(okHttpClient))
                .setEndpoint(snapBaseURL);
        return builder.build().create(SnapRestAPI.class);
    }
}