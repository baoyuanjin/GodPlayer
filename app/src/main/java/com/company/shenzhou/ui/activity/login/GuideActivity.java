package com.company.shenzhou.ui.activity.login;


import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.company.shenzhou.R;
import com.company.shenzhou.base.BaseActivity;
import com.company.shenzhou.bean.Constants;
import com.company.shenzhou.bean.dbbean.UserDBRememberBean;
import com.company.shenzhou.ui.activity.MainActivity;
import com.company.shenzhou.utils.ScreenSizeUtil;
import com.company.shenzhou.utils.db.UserDBRememberBeanUtils;
import com.hjq.base.BaseDialog;
import com.yun.common.utils.LogUtils;
import com.yun.common.utils.SharePreferenceUtil;
import com.yun.common.utils.StatusBarUtils;
import com.yun.common.viewpagerlib.bean.PageBean;
import com.yun.common.viewpagerlib.callback.PageHelperListener;
import com.yun.common.viewpagerlib.indicator.TransIndicator;
import com.yun.common.viewpagerlib.view.GlideViewPager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lovelin on 2019/3/27
 * <p>
 * Describe:新手引导页
 */
public class GuideActivity extends BaseActivity {
    private String userUrl = "http://testbazi.zgszfy.com/api/bs/yc.html";  //用户协议
    private String privacyUrl = "http://jdyapi.jxsccm.com//api/index/yc.html";  //隐私条款
    private static final Integer[] RES = {R.mipmap.guide111, R.mipmap.guide222, R.mipmap.guide333
    };
    private Boolean isLogined;
    private RelativeLayout relative_guide;
    private Boolean userAgreementTag;
    private TextView mTvContent;

    @Override
    public int getContentViewId() {
        return R.layout.activity_guide;
    }

    @Override
    public void init() {
        initView();

    }


    public void initView() {
        ViewGroup rootView = getWindow().getDecorView().findViewById(R.id.root_layout);
        //设置向下移动状态栏的高度
        rootView.setPadding(0, StatusBarUtils.getStatusBarHeight(this), 0, 0);
        setTitleBarVisibility(View.GONE);
        GlideViewPager viewPager = (GlideViewPager) findViewById(R.id.splase_viewpager);
        TransIndicator linearLayout = (TransIndicator) findViewById(R.id.splase_bottom_layout);
        //点击跳转的按钮
        Button button = (Button) findViewById(R.id.splase_start_btn);
        mTvContent = findViewById(R.id.tv_content);
        relative_guide = (RelativeLayout) findViewById(R.id.relative_guide);
        userAgreementTag = (Boolean) SharePreferenceUtil.get(GuideActivity.this, Constants.Sp_UserAgreement_Tag, false);
        isLogined = (Boolean) SharePreferenceUtil.get(this, Constants.Is_Logined, false);
        setCurrentUserMsg();

        if (!userAgreementTag) {
            showUserAgreementDialog();
        }
        //先把本地的图片 id 装进 list 容器中
        List<Integer> imagesList = new ArrayList<>();
        for (int i = 0; i < RES.length; i++) {
            imagesList.add(RES[i]);

        }
        //配置pagerbean，这里主要是为了viewpager的指示器的作用，注意记得写上泛型
        PageBean bean = new PageBean.Builder<Integer>()
                .setDataObjects(imagesList)
                .setIndicator(linearLayout)
                .setOpenView(button)
                .builder();

        // 把数据添加到 viewpager中，并把view提供出来，这样除了方便调试，也不会出现一个view，多个
        // parent的问题，这里在轮播图比较明显
        viewPager.setPageListener(bean, R.layout.guide_image_layout, new PageHelperListener() {
            @Override
            public void getItemView(View view, Object data) {
                ImageView imageView = view.findViewById(R.id.icon);
                imageView.setImageResource((Integer) data);
            }
        });

        //点击实现跳转功能
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isLogined) {  //未登入,跳转登入界面
                    Intent intent = new Intent();
                    intent.setClass(GuideActivity.this, LoginAnimatorActivity.class);
                    startActivity(intent);
                    SharePreferenceUtil.put(GuideActivity.this, Constants.SP_IS_FIRST_IN, false);
                    finish();
                } else {
                    SharePreferenceUtil.put(GuideActivity.this, Constants.SP_IS_FIRST_IN, false);   //false 不是第一次登入了
                    SharePreferenceUtil.put(GuideActivity.this, Constants.Is_Logined, false);
                    startActivity(new Intent(GuideActivity.this, MainActivity.class));
                    finish();
                }
            }
        });
    }

    /**
     * 因为系统默认给的账号密码admin 权限是超级用户:2
     */
    private void setCurrentUserMsg() {
        SharePreferenceUtil.put(GuideActivity.this, SharePreferenceUtil.Current_Username, "admin");
        SharePreferenceUtil.put(GuideActivity.this, SharePreferenceUtil.Current_Password, "admin");
        SharePreferenceUtil.put(GuideActivity.this, SharePreferenceUtil.Current_UserType, 2);
        SharePreferenceUtil.put(GuideActivity.this, SharePreferenceUtil.Current_ID, 1);
        SharePreferenceUtil.put(GuideActivity.this, SharePreferenceUtil.Current_Admin_ChangePassword, false);    //超级用户只能修改一次密码

        //存入数据库
        long ID = 1;
        UserDBRememberBean userDBBean = new UserDBRememberBean();
//        UserDBBean userDBBean = new UserDBBean();
        userDBBean.setUsername("admin");
        userDBBean.setPassword("admin");
        userDBBean.setTag("admin");
        userDBBean.setUserType(2);
        userDBBean.setId(ID);
        UserDBRememberBeanUtils.insertOrReplaceData(userDBBean);
//        UserDBUtils.insertOrReplaceData(userDBBean);
        boolean isExist = UserDBRememberBeanUtils.queryListIsExist("admin");
        LogUtils.e("DB=====isExist===" + isExist);
        String str = "admin";
        List<UserDBRememberBean> userDBRememberBeans = UserDBRememberBeanUtils.queryListByMessage(str);
        for (int i = 0; i < userDBRememberBeans.size(); i++) {
            String username = userDBRememberBeans.get(i).getUsername();
            String password = userDBRememberBeans.get(i).getPassword();
            LogUtils.e("DB=====username===" + username + "==password==" + password);
        }
        LogUtils.e("DB=====isExist===" + isExist);
    }


    /**
     * 用户协议dialog 不然上不了应用市场
     */
    private void showUserAgreementDialog() {
        SpannableString textSpanned1 = new SpannableString("在你使用CME Player之前，请你认真阅读并了解《CME Player用户协议》和《CME Player隐私权政策》,点击同意即表示你已阅读并且了解。");
        //设置颜色
        textSpanned1.setSpan(new ForegroundColorSpan(Color.BLUE),
                26, 42, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textSpanned1.setSpan(new ForegroundColorSpan(Color.BLUE),
                43, 60, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        //设置Hello World前三个字符有点击事件
//        SpannableStringBuilder textSpanned4 = new SpannableStringBuilder("Hello World");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
//                Bundle bundle = new Bundle();
//                bundle.putString("typeUrl", "1");
//                openActivity(SomeRequestActivity.class, bundle);
                Toast.makeText(GuideActivity.this, "跳转---用户协议界面", Toast.LENGTH_SHORT).show();

            }
        };
        ClickableSpan clickableSpan2 = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                Toast.makeText(GuideActivity.this, "跳转---隐私权界面", Toast.LENGTH_SHORT).show();

//                Bundle bundle = new Bundle();
//                bundle.putString("typeUrl", "2");
//                openActivity(SomeRequestActivity.class, bundle);
            }
        };
        textSpanned1.setSpan(clickableSpan,
                26, 42, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textSpanned1.setSpan(clickableSpan2,
                43, 60, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//注意：此时必须加这一句，不然点击事件不会生效
//        text4.setMovementMethod(LinkMovementMethod.getInstance());
//        text4.setText(textSpanned4);
        int screenWidth = ScreenSizeUtil.getScreenWidth(this);

        // 自定义对话框
        BaseDialog.Builder<BaseDialog.Builder> builderBuilder = new BaseDialog.Builder<>(this);
        builderBuilder.setContentView(R.layout.dialog_useragreement)
                .setAnimStyle(BaseDialog.ANIM_IOS)
                .setCanceledOnTouchOutside(false)
                .setWidth(screenWidth / 2)
                .setText(R.id.tv_content, textSpanned1)
                .setOnClickListener(R.id.btn_dialog_custom_ok, new BaseDialog.OnClickListener() {
                    @Override
                    public void onClick(BaseDialog dialog, View view) {
                        SharePreferenceUtil.put(GuideActivity.this, Constants.Sp_UserAgreement_Tag, true);
                        dialog.dismiss();
                    }
                })
                .setOnClickListener(R.id.btn_dialog_custom_cancle, new BaseDialog.OnClickListener() {
                    @Override
                    public void onClick(BaseDialog dialog, View view) {
                        Toast.makeText(GuideActivity.this, "Dialog 取消了", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        finish();
                    }
                })
//                .addOnShowListener(dialog -> Toast.makeText(this, "Dialog 显示了", Toast.LENGTH_SHORT).show())
//                .addOnCancelListener(new BaseDialog.OnCancelListener() {
//                    @Override
//                    public void onCancel(BaseDialog dialog) {
//
//                    }
//                })
//                .addOnDismissListener(dialog -> Toast.makeText(this, "Dialog 销毁了", Toast.LENGTH_SHORT).show())
//                .setOnKeyListener((dialog, event) -> {
//                    Toast.makeText(this, "按键代码：" + event.getKeyCode(), Toast.LENGTH_SHORT).show();
//                    return false;
//                })
                .show();
        //注意：此时必须加这一句，不然点击事件不会生效
        TextView viewById = builderBuilder.findViewById(R.id.tv_content);
        viewById.setMovementMethod(LinkMovementMethod.getInstance());


    }

}