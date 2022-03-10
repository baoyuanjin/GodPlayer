package com.company.shenzhou.ui.fragment.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.shenzhou.R;
import com.company.shenzhou.base.BaseFragment;
import com.company.shenzhou.bean.RefreshEvent;
import com.company.shenzhou.bean.dbbean.VideoDBBean01;
import com.company.shenzhou.ui.activity.vlc.VlcPlayerActivity;
import com.company.shenzhou.ui.activity.zxing.ZXingActivity;
import com.company.shenzhou.ui.fragment.video.adapter.VideoAdapter;
import com.company.shenzhou.utils.ClearEditText;
import com.company.shenzhou.utils.CommonUtil;
import com.company.shenzhou.utils.db.VideoDB01Utils;
import com.company.shenzhou.view.ListPopup;
import com.company.shenzhou.view.dialog.AddAdviceInputDialog;
import com.company.shenzhou.view.dialog.AdviceReInputDialog;
import com.company.shenzhou.view.dialog.SelectDialog;
import com.hjq.base.BaseDialog;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.yun.common.utils.DensityUtils;
import com.yun.common.utils.LogUtils;
import com.yun.common.utils.ScreenUtils;
import com.yun.common.utils.SharePreferenceUtil;
import com.yun.common.utils.popupwindow.PopupWindowTwoButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;

/**
 * LoveLin
 * <p>
 * Describe视频设备管理界面
 */
public class VideoFragment extends BaseFragment implements VideoAdapter.ClickCallBack {
    @BindView(R.id.videoRecycleview)
    RecyclerView mRecyclerView;
    @BindView(R.id.btn_add)
    AppCompatButton mAddMachine;
    @BindView(R.id.btn_add_code)
    AppCompatButton mAddMachineZxing;
    @BindView(R.id.page_empty_desc)
    TextView mEmpty;
    @BindView(R.id.linear_all)
    LinearLayout mLinearAll;
    @BindView(R.id.smartRefresh)
    SmartRefreshLayout mSmartRefresh;
    private TextView mAccountView, mPasswordView, mTitleView, mMessageView, mPortView, micPortView, mTypeSelecter;
    private ClearEditText mTypeView, mIPView;
    private ArrayList<VideoDBBean01> mDataList = new ArrayList<>();
    private List currentRecycleViewList = null;
    private VideoAdapter mAdapter;
    private String account;
    private String password;
    private String title;
    private String ip;
    private String makeMessage;
    private String port;
    private String micPort;
    private String type;
    private VideoDBBean01 mBean;
    private boolean isOk = false;  //信息录入开关   默认没有填写完成
    private String currentUsername;
    private PopupWindowTwoButton deletePop;
    private String popType = "";
    private AdviceReInputDialog.Builder builder;
    private AddAdviceInputDialog.Builder addInPutBuilder;      //添加设备信息对话框
    private AdviceReInputDialog.Builder mReInputPopBuilder;    //修改设备信息对话框
    private boolean DialogHan_IsShow = false;                  //输入对话框是否存在的标志,存在（设备信息对话框）只刷新数据,不存在弹出对话框,默认不存在
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0: //手动输入的时候弹出类别选择，并且选择之后输入默认值
                    showHandInputTypeDialog();
                    break;
                case 1: //设置Dialog数据
                    setDialogData();
                    break;
                case 2://只刷新设备Dialog数据
                    justRefreshDialogData();
                    break;
                case 3://第一次读取全部的DB设备数据
                    showEmptyOrContentView(mDataList);
                    break;
            }
        }
    };

    @Override
    public int getContentViewId() {
        return R.layout.fragment_video;
    }

    @Override
    protected void init(ViewGroup rootView) {
        EventBus.getDefault().register(this);
        setTitleBarVisibility(View.VISIBLE);
        setTitleLeftBtnVisibility(View.GONE);
        setTitleRightBtnVisibility(View.VISIBLE);
        setTitleRightBtnResources(R.drawable.selector_add_something);
        setTitleName("设备");
        setPageStateView();
        initView();
        responseListener();
    }

    private void initView() {
        currentUsername = (String) SharePreferenceUtil.get(getActivity(), SharePreferenceUtil.Current_Username, "");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//        mRecyclerView.addItemDecoration(new RecycleViewDivider(
//                getActivity(), 1, R.drawable.recicleview_divider_line));
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new VideoAdapter(mDataList, getActivity());
        mAdapter.setClickCallBack(this);
        mRecyclerView.setAdapter(mAdapter);
        startThreadReadDBData();
    }

    private void startThreadReadDBData() {
        new Thread(() -> {
            currentRecycleViewList = VideoDB01Utils.queryRawTag(currentUsername);
            mDataList.addAll(currentRecycleViewList);
            mHandler.sendEmptyMessage(3);
        }).start();
    }

    /**
     * 接口回调,itemClickListener
     *
     * @param bean
     * @param mList
     * @param position
     * @param type
     */
    @Override
    public void onItemCallBack(VideoDBBean01 bean, ArrayList<VideoDBBean01> mList, int position, String type) {
        showEmptyOrContentView(mList);
        switch (type) {
            case "video"://跳转播放界面
                LogUtils.e("跳转播放界面" + "username=:" + bean.getAccount() + ",  password=:" + bean.getPassword() + ",  ip:" + bean.getIp() + ",  备注:" + bean.getMakeMessage() + ",  端口:"
                        + bean.getPort() + ",  类型:" + bean.getType());
                /**
                 * HD3      rtsp://username:password@ip/MediaInput/h264/stream_1 ------   --HD3，高清:端口是80不用添加端口，不是80，就需要手动添加
                 * HD3      rtsp://username:password@ip/MediaInput/h264/stream_2 ------   --HD3，标清
                 * 一体机   rtsp://username:password@ip：port/session0.mpg ------           --一体机， 高清
                 * 一体机   rtsp://username:password@ip：port/session1.mpg ------          --一体机， 标清
                 * url      http://www.cme8848.com/live/cme.m3u8                          eg:链接地址=用户输入的url链接
                 * url      http://www.cme8848.com/live/flv                               eg:链接地址=用户输入的url链接
                 */
                String username = bean.getAccount();
                String password = bean.getPassword();
                String ip = bean.getIp();
                String port = bean.getPort();
                String currentUrl01 = "";
                String currentUrl02 = "";
                Intent intent = new Intent(getActivity(), VlcPlayerActivity.class);
                Log.e("path=====Start:=====", "bean.getType()====" + bean.getType());
                switch (bean.getType()) {
                    case SharePreferenceUtil.Type_HD3: //HD3  高清:端口是80不用添加端口，不是80，就需要手动添加
                        if ("80".equalsIgnoreCase(port + "")) {
                            currentUrl01 = "rtsp://" + username + ":" + password + "@" + ip + "/MediaInput/h264/stream_1";
                            currentUrl02 = "rtsp://" + username + ":" + password + "@" + ip + "/MediaInput/h264/stream_2";
                        } else {
                            currentUrl01 = "rtsp://" + username + ":" + password + "@" + ip + ":" + port + "/MediaInput/h264/stream_1";
                            currentUrl02 = "rtsp://" + username + ":" + password + "@" + ip + ":" + port + "/MediaInput/h264/stream_2";
                        }
                        intent.putExtra("url01", currentUrl01);
                        intent.putExtra("url02", currentUrl02);
                        intent.putExtra("urlType", "00");
                        intent.putExtra("mTitle", bean.getTitle() + " (" + "ip:" + ip + ")");
                        intent.putExtra("ip", bean.getIp());
                        intent.putExtra("micport", bean.getMicport());
                        LogUtils.e("pusher==HD3==micport===" + bean.getMicport());
                        LogUtils.e("pusher==HD3==micport===" + currentUrl01);
                        startActivity(intent);
                        break;
                    case SharePreferenceUtil.Type_Yitiji: //一体机
                        currentUrl01 = "rtsp://" + username + ":" + password + "@" + ip + ":" + port + "/session0.mpg";  //高清
                        currentUrl02 = "rtsp://" + username + ":" + password + "@" + ip + ":" + port + "/session1.mpg";  //标清
                        intent.putExtra("url01", currentUrl01);
                        intent.putExtra("url02", currentUrl02);
                        intent.putExtra("urlType", "01");
                        intent.putExtra("mTitle", bean.getTitle() + " (" + "ip:" + ip + ")");
                        intent.putExtra("ip", bean.getIp());
                        intent.putExtra("micport", bean.getMicport());
                        LogUtils.e("pusher==一体机==micport===" + bean.getMicport());
                        startActivity(intent);
                        break;
                    case SharePreferenceUtil.Type_Url: //自定义url
                        currentUrl01 = bean.getIp() + "";
                        currentUrl02 = bean.getIp() + "";
                        String replace1 = currentUrl01.replace(" ", "");
                        String replace2 = currentUrl02.replace(" ", "");
                        intent.putExtra("url01", replace1);
                        intent.putExtra("url02", replace2);
                        intent.putExtra("urlType", "02");
                        intent.putExtra("mTitle", bean.getTitle() + " (" + ip + ")");
                        intent.putExtra("ip", bean.getIp());
                        intent.putExtra("micport", bean.getMicport());
                        LogUtils.e("pusher==自定义url==currentUrl01===" + currentUrl01);
                        LogUtils.e("pusher==自定义url==micport===" + bean.getMicport());
                        startActivity(intent);
                        break;
                    default:
                        showToast("播放类型错误,请修改!");
                        break;
                }
                break;
            case "delete"://删除对话框,删除当前设备信息
                showDeletePop(bean, position);
                break;
            case "reInput":// 修改对话框,更改当前设备信息
                showReInputPop(bean);
                break;
        }

    }


    /**
     * 用户权限的判断--所有用户权限都可以
     * 查看、添加、删除、修改
     */
    private void responseListener() {
        getRightView().setOnClickListener((View v) -> {
//                setTextColor(getResources().getColor(R.color.colorAccent), "录像", false);
//                Drawable record_start = getResources().getDrawable(R.drawable.icon_record_pre);
//                recordStart.setCompoundDrawablesWithIntrinsicBounds(null, record_start, null, null);
            new ListPopup.Builder(getActivity())
                    .setList("扫一扫", "填一填")//icon_video_code
//                        .setBackground(1,getActivity().getResources().getDrawable(R.drawable.icon_video_code))
                    .setListener((ListPopup.OnListener<String>) (popupWindow, position, s) -> {
                        if (s.equals("扫一扫")) {
                            GoToZXingInput();
                        } else if (s.equals("填一填")) {
                            //先选择类型，在弹出设备dialog
                            showInputPop();
                        }
                    })
                    .setYOffset(-30)
                    .showAsDropDown(getRightView());
        });
    }

    private void GoToZXingInput() {
        XXPermissions.with(this)
                // 不适配 Android 11 可以这样写
                //.permission(Permission.Group.STORAGE)
                // 适配 Android 11 需要这样写，这里无需再写 Permission.Group.STORAGE
//                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            Intent intent = new Intent(getActivity(), ZXingActivity.class);
                            intent.putExtra("currentUsername", currentUsername);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) {
                            showToast("被永久拒绝授权，请手动授予存储权限");
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(getActivity(), permissions);
                        } else {
                            showToast("获取存储权限失败");
                        }
                    }
                });

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            int width = ScreenUtils.getScreenWidth(getActivity()) - DensityUtils.dp2px(getActivity(), 100);
            setLayoutParams(70, width);
        } else {                                                           //竖屏
            int width = ScreenUtils.getScreenWidth(getActivity()) - DensityUtils.dp2px(getActivity(), 60);
            setLayoutParams(50, width);

        }
    }

    private void setLayoutParams(int i, int width) {
        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mParams.leftMargin = i;
        mParams.rightMargin = i;
        switch (popType) {
            case "delete":
                if (deletePop != null && deletePop.isShowing()) {
                    deletePop.dismiss();
                    deletePop.setWidth(width);
                    deletePop.getLinear_pop_change().setLayoutParams(mParams);
                    deletePop.showPopupWindow(mLinearAll, Gravity.CENTER);
                }
                break;

        }
    }

    public void setDismissPop() {
        if (null != deletePop) {
            deletePop.dismiss();
        }
        if (null != addInPutBuilder) {
            addInPutBuilder.dismiss();
        }

    }

    //修改对话框
    private void showReInputPop(VideoDBBean01 bean) {
        popType = "change";
        isOk = false;
        mReInputPopBuilder = new AdviceReInputDialog.Builder(getActivity(), bean);
        mReInputPopBuilder.setTitle("设备修改");
        mTypeSelecter = mReInputPopBuilder.getTypeView();
        // 内容必须要填写
        // 确定按钮文本
        mReInputPopBuilder.setConfirm(getString(R.string.common_confirm));
        // 设置 null 表示不显示取消按钮
        mReInputPopBuilder.setCancel(getString(R.string.common_cancel));
        mReInputPopBuilder.setListener(new AdviceReInputDialog.OnListener() {
            @Override
            public void onConfirm(BaseDialog dialog, HashMap<String, String> mMap) {
//                对DB做修改或者增加的操作
                checkInputBuilderData(mMap);
                LogUtils.e("修改======" + mMap.toString());
//                {makeMessage=一体机, password=root, port=7788, ip=192.168.1.200, title=一体机的标题, type=一体机, account=root}

                //VideoDBBean beanData = getBeanData();
                if (isOk) {
                    showToast(bean.getId() + "");
                    mBean.setId(bean.getId());
                    String micport = mBean.getMicport();
                    LogUtils.e("修改===micport===" + micport);

                    VideoDB01Utils.updateData(mBean);
                    currentRecycleViewList = VideoDB01Utils.queryRawTag(currentUsername);
                    showEmptyOrContentView((ArrayList<VideoDBBean01>) currentRecycleViewList);
                    mAdapter.setListAndNotifyDataSetChanged(currentRecycleViewList);
                    showToast("修改成功");
                }
            }

            /**
             * 从新选择类别
             * @param mType
             */
            @Override
            public void onReInputTypeClick(TextView mType) {
                mTypeSelecter = mType;
                showReSelectDialog(mType);
            }

            @Override
            public void onCancel(BaseDialog dialog) {
//                showToast("取消修改");

            }
        }).show();


    }

    //类别输入对话框
    private void showInputSelectTypeDialog() {
        DialogHan_IsShow = false;

        // 单选对话框
        new SelectDialog.Builder(getActivity())
                .setTitle("请选择类型")
                .setList("HD3", "一体机", "自定义URL")  //对接协议 0:播放HD3,1:播放一体机,2:播放url链接地址
                // 设置单选模式
                .setSingleSelect()
                // 设置默认选中
                .setSelect(2)
                .setCancelable(false)
                .setListener(new SelectDialog.OnListener<String>() {
                    @Override
                    public void onSelected(BaseDialog dialog, HashMap<Integer, String> data) {
//                        showToast("确定了：" + data.toString());
                        String substring = data.toString().substring(1, 2);
//                        mType.setText(substring);
                        //先选择类型，在弹出设备dialog
                        switchHandInputBean(substring);

                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
//                        showToast("取消了");
                    }
                })
                .setBackgroundDimEnabled(false)
                .show();
    }


    private void switchHandInputBean(String substring) {
        /**
         * HD3      rtsp://username:password@ip/MediaInput/h264/stream_1 ------     --HD3，高清
         * HD3      rtsp://username:password@ip/MediaInput/h264/stream_2 ------     --HD3，标清
         * 一体机   rtsp://username:password@ip port/session0.mpg ------            --一体机，标清
         * 一体机   rtsp://username:password@ip：port/session1.mpg ------            --一体机，高清
         * url      http://www.cme8848.com/live/cme.m3u8                            eg:链接地址=用户输入的url链接
         * url      http://www.cme8848.com/live/flv                                 eg:链接地址=用户输入的url链接
         */
        switch (substring) {
            case "0":   //HD3
                account = "admin";
                password = "12345";
                title = "HD3的标题";
                ip = "192.168.1.10";
                makeMessage = "HD3的备注信息";
                port = "80";
                micPort = "7789";
                type = "HD3";
                if (!DialogHan_IsShow) {   //不存在
                    mHandler.sendEmptyMessage(1);  //设置数据
                } else {
                    mHandler.sendEmptyMessage(2); //刷新数据
                }

                break;
            case "1":   //一体机
                account = "root";
                password = "root";
                title = "一体机的标题";
                ip = "192.168.1.200";
                makeMessage = "一体机的备注信息";
                port = "7788";
                micPort = "7789";
                type = "一体机";
                if (!DialogHan_IsShow) {   //不存在
                    mHandler.sendEmptyMessage(1);
                } else {
                    mHandler.sendEmptyMessage(2);
                }
                break;
            case "2":   //自定义url
                account = "root";
                password = "root";
                title = "自定义URL的标题";
                ip = "";
                makeMessage = "自定义URL的备注信息";
                port = "7788";
                micPort = "7789";
                type = "自定义URL";
                LogUtils.e("ZZZZZZZZZ==title==" + DialogHan_IsShow);

                if (!DialogHan_IsShow) {   //不存在
                    mHandler.sendEmptyMessage(1);
                } else {
                    mHandler.sendEmptyMessage(2);
                }
                break;
        }

    }

    //从新选择类别
    private void showReSelectDialog(TextView mType) {
        // 单选对话框
        new SelectDialog.Builder(getActivity())
                .setTitle("请选择类型")
                .setList("HD3", "一体机", "自定义URL")  //对接协议 0:播放HD3,1:播放一体机,2:播放url链接地址
                // 设置单选模式
                .setSingleSelect()
                // 设置默认选中
                .setSelect(2)
                .setListener(new SelectDialog.OnListener<String>() {
                    @Override
                    public void onSelected(BaseDialog dialog, HashMap<Integer, String> data) {
//                        showToast("确定了：" + data.toString());
                        String substring = data.toString().substring(1, 2);
                        DialogHan_IsShow = true;  //说明设备修改dialog存在,只做数据的刷新
                        switchHandInputBean(substring);
                    }

                    @Override
                    public void onCancel(BaseDialog dialog) {
                        showToast("取消了");
                    }
                })
                .setBackgroundDimEnabled(false)
                .show();
    }

    private void setDialogData() {
        DialogHan_IsShow = true;    //防止点击视频类别  弹出多个Dialog的Bug
        if (null == addInPutBuilder) {
            addInPutBuilder = new AddAdviceInputDialog.Builder(getActivity());
        }
        mAccountView = addInPutBuilder.getAccountView();
        mPasswordView = addInPutBuilder.getPasswordView();
        mTitleView = addInPutBuilder.getTitleView();
        mIPView = addInPutBuilder.getIPView();
        mMessageView = addInPutBuilder.getMessageView();
        mPortView = addInPutBuilder.getPortView();
        mTypeView = addInPutBuilder.getTypeView();
        micPortView = addInPutBuilder.getMicPortView();
        mAccountView.setText("" + account);
        mPasswordView.setText("" + password);
        mTitleView.setText("" + title);
        micPortView.setText("" + micPort);
        LogUtils.e("ZZZZZZZZZ==account==" + account);
        LogUtils.e("ZZZZZZZZZ==password==" + password);
        LogUtils.e("ZZZZZZZZZ==title==" + title);
        LogUtils.e("ZZZZZZZZZ==type==" + type);
        LogUtils.e("ZZZZZZZZZ==type==" + type);
        LogUtils.e("ZZZZZZZZZ==type==" + micPort);
        if (SharePreferenceUtil.Type_Url.equals(type)) {
            mIPView.setText("");
            CommonUtil.showSoftInputFromWindow(getActivity(), mIPView);
        } else {
            mIPView.setText("" + ip);
        }
        mMessageView.setText("" + makeMessage);
        mPortView.setText("" + port);
        String currentMicPort = null == micPort ? "7789" : micPort;
        micPortView.setText("" + currentMicPort);
        mTypeView.setText("" + type);
        addInPutBuilder.setTitle("添加设备");
        // 内容必须要填写
        // 确定按钮文本
        addInPutBuilder.setConfirm(getString(R.string.common_confirm));
        // 设置 null 表示不显示取消按钮
        addInPutBuilder.setCancel(getString(R.string.common_cancel));
        addInPutBuilder.setListener(new AddAdviceInputDialog.OnListener() {
            @Override
            public void onConfirm(BaseDialog dialog, HashMap<String, String> mMap) {
                checkInputBuilderData(mMap);
                if (isOk) {
                    VideoDB01Utils.insertOrReplaceData(mBean);
//                    List currentList = VideoDB01Utils.queryAll(VideoDBBean.class);
                    currentRecycleViewList = VideoDB01Utils.queryRawTag(currentUsername);
                    showEmptyOrContentView((ArrayList<VideoDBBean01>) currentRecycleViewList);
                    mAdapter.setListAndNotifyDataSetChanged(currentRecycleViewList);
                    addInPutBuilder.dismissDialog();
                    showToast("添加成功");
                    DialogHan_IsShow = false;
                }
            }

            @Override
            public void onReInputTypeClick(TextView mTv) {
                mTypeSelecter = mTv;
                LogUtils.e("mType===========" + mTypeSelecter);
                //类别输入对话框
//                DialogHan_IsShow = true;

                showInputSelectTypeDialog();
            }

            @Override
            public void onCancel(BaseDialog dialog) {
                DialogHan_IsShow = false;

            }
        }).show();
    }

    private void justRefreshDialogData() {
        if (null != mReInputPopBuilder) {
            mAccountView = mReInputPopBuilder.getAccountView();
            mPasswordView = mReInputPopBuilder.getPasswordView();
            mTitleView = mReInputPopBuilder.getTitleView();
            mIPView = mReInputPopBuilder.getIPView();
            mMessageView = mReInputPopBuilder.getMessageView();
            mPortView = mReInputPopBuilder.getPortView();
            mTypeView = mReInputPopBuilder.getTypeView();
            micPortView = mReInputPopBuilder.getMicPortView();
            mAccountView.setText("" + account);
            mPasswordView.setText("" + password);
            mTitleView.setText("" + title);
            LogUtils.e("ZZZZZZZZZ==typetypetypetypetype==" + type);   //type==自定义URL

            if ("自定义URL".equals(type)) { //自定义URL
                CommonUtil.showSoftInputFromWindow(getActivity(), mIPView);
            } else {
                mIPView.setText("" + ip);
            }
            mMessageView.setText("" + makeMessage);
            mPortView.setText("" + port);
            mTypeView.setText("" + type);
        }

        if (null != addInPutBuilder) {
            mAccountView = addInPutBuilder.getAccountView();
            mPasswordView = addInPutBuilder.getPasswordView();
            mTitleView = addInPutBuilder.getTitleView();
            mIPView = addInPutBuilder.getIPView();
            mMessageView = addInPutBuilder.getMessageView();
            mPortView = addInPutBuilder.getPortView();
            mTypeView = addInPutBuilder.getTypeView();
            micPortView = addInPutBuilder.getMicPortView();
            mAccountView.setText("" + account);
            mPasswordView.setText("" + password);
            mTitleView.setText("" + title);
            LogUtils.e("ZZZZZZZZZ==typetypetypetypetype==" + type);   //type==自定义URL

            if ("自定义URL".equals(type)) { //自定义URL
                mIPView.setText("");
                CommonUtil.showSoftInputFromWindow(getActivity(), mIPView);
            } else {
                mIPView.setText("" + ip);
            }
            mMessageView.setText("" + makeMessage);
            mPortView.setText("" + port);
            mTypeView.setText("" + type);
        }

    }

    /**
     * 手动输入的时候，默认弹出类别选择dialog，或者再次选择类别
     */
    private void showHandInputTypeDialog() {
        showInputSelectTypeDialog();
    }

    private void showInputPop() {
        popType = "add";
        isOk = false;
        //弹出类别对话框
        mHandler.sendEmptyMessage(0);
    }

    private void showDeletePop(VideoDBBean01 bean, int position) {
        popType = "delete";
        deletePop = new PopupWindowTwoButton((Activity) getActivity());
        deletePop.getTv_content().setText("是否确认删除该设备信息?");
        deletePop.getTv_ok().setText("确定");
        deletePop.getTv_cancel().setText("取消");
        deletePop.getTv_ok().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoDB01Utils.deleteData(bean);
                currentRecycleViewList = VideoDB01Utils.queryRawTag(currentUsername);
                //删除单个item
                mDataList.remove(position);
                mAdapter.notifyItemRemoved(position);
                mAdapter.notifyItemRangeChanged(0, mDataList.size());
                showEmptyOrContentView((ArrayList<VideoDBBean01>) currentRecycleViewList);
                deletePop.dismiss();
            }
        });
        deletePop.getTv_cancel().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePop.dismiss();
            }
        });
        deletePop.showPopupWindow(mLinearAll, Gravity.CENTER);
    }

    private void checkInputBuilderData(HashMap<String, String> mMap) {
        account = mMap.get("account");
        password = mMap.get("password");
        title = mMap.get("title");
        ip = mMap.get("ip");
        makeMessage = mMap.get("makeMessage");
        port = mMap.get("port");
        micPort = mMap.get("micport");
        type = mMap.get("type");
        if ("2".equals(type)) {
            if ("".equals(title)) {
                showToast("标题不能为空");
            } else if ("".equals(ip)) {
                showToast("ip不能为空");
            } else if ("".equals(makeMessage)) {
                showToast("备注信息不能为空");
            } else if ("".equals(type)) {
                showToast("播放类型不能为空");
            } else {
                isOk = true;
                getBeanData();
            }
        } else {
            if ("".equals(account)) {
                showToast("设备账号不能为空");
            } else if ("".equals(password)) {
                showToast("设备密码不能为空");
            } else if ("".equals(title)) {
                showToast("标题不能为空");
            } else if ("".equals(ip)) {
                showToast("ip不能为空");
            } else if ("".equals(makeMessage)) {
                showToast("备注信息不能为空");
            } else if ("".equals(port)) {
                showToast("端口号不能为空");
            } else if ("".equals(type)) {
                showToast("播放类型不能为空");
            } else {
                isOk = true;
                getBeanData();
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        currentRecycleViewList = VideoDB01Utils.queryRawTag(currentUsername);
        mDataList.clear();
        mDataList.addAll(currentRecycleViewList);
        showEmptyOrContentView(mDataList);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private VideoDBBean01 getBeanData() {
        mBean = new VideoDBBean01();
        mBean.setAccount(account);
        mBean.setPassword(password);
        mBean.setTitle(title);
        mBean.setIp(ip);
        mBean.setMakeMessage(makeMessage);
        mBean.setPort(port);
        mBean.setType(type);
        String currentMicPort = null == micPort ? "7789" : micPort;
        mBean.setMicport(currentMicPort + "");
        mBean.setTag(currentUsername);
        return mBean;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setDismissPop();
        EventBus.getDefault().unregister(this);
    }

    private void showEmptyOrContentView(ArrayList<VideoDBBean01> mList) {
        if (mList.size() == 0) {
            mEmpty.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mEmpty.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        }
    }

}
