package com.company.shenzhou.view;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.company.shenzhou.R;
import com.just.agentweb.IWebLayout;

/**
 * company：江西神州医疗设备有限公司
 * author： LoveLin
 * time：2022/2/18 9:43
 * desc：
 */

public class WebLayout implements IWebLayout {

    private Activity mActivity;
    //    private final TwinklingRefreshLayout mTwinklingRefreshLayout;
    private final LinearLayout mTwinklingRefreshLayout;
    private WebView mWebView = null;

    public WebLayout(Activity activity) {
        this.mActivity = activity;
//        mTwinklingRefreshLayout = (TwinklingRefreshLayout) LayoutInflater.from(activity).inflate(R.layout.fragment_twk_web, null);
        mTwinklingRefreshLayout = (LinearLayout) LayoutInflater.from(activity).inflate(R.layout.fragment_twk_web, null);
//        mTwinklingRefreshLayout.setPureScrollModeOn();
//        mTwinklingRefreshLayout.setEnableOverScroll(false);
//        mTwinklingRefreshLayout.setEnableRefresh(true);
//        mTwinklingRefreshLayout.setEnableLoadmore(true);
//        mTwinklingRefreshLayout.setOverScrollHeight(0);
//        mTwinklingRefreshLayout.setMaxHeadHeight(1);
//        mTwinklingRefreshLayout.setMaxBottomHeight(1);
        mWebView = (WebView) mTwinklingRefreshLayout.findViewById(R.id.webView);
    }

    @NonNull
    @Override
    public ViewGroup getLayout() {
        return mTwinklingRefreshLayout;
    }

    @Nullable
    @Override
    public WebView getWebView() {
        return mWebView;
    }


}
