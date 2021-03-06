package com.example.chinesejar.sipdemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, Toolbar.OnMenuItemClickListener, Spinner.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener, IMainView {

    private MainPresenter mainPresenter;

    private Toolbar toolbar = null;

    private LinearLayout srcIPLinearLayout = null;
    private Spinner spinnerNetworks = null;
    private EditText editDstIP = null;
    private EditText editSrcIP = null;
    private ArrayAdapter<String> adapter = null;
    private TextView tvReceive = null;
    private Button btnSend = null;
    private Button btnClear = null;

    private RadioGroup socketRadioGroup = null;
    private RadioGroup packageRadioGroup = null;
    private String socketType = "TCP";
    private String packageType = "REGISTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

    }

    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);

        srcIPLinearLayout = (LinearLayout) findViewById(R.id.src_area);
        spinnerNetworks = (Spinner) findViewById(R.id.spinner_networks);
        editDstIP = (EditText) findViewById(R.id.et_dstip);
        editSrcIP = (EditText) findViewById(R.id.et_srcip);
        tvReceive = (TextView) findViewById(R.id.tv_receive);

        socketRadioGroup = (RadioGroup) findViewById(R.id.radio_group_socket);
        socketRadioGroup.setOnCheckedChangeListener(this);
        packageRadioGroup = (RadioGroup) findViewById(R.id.radio_group_package);
        packageRadioGroup.setOnCheckedChangeListener(this);
        btnSend = (Button) findViewById(R.id.btn_send);
        btnSend.setOnClickListener(this);
        btnClear = (Button) findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(this);

        mainPresenter = new MainPresenter(this);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mainPresenter.getNetworkInterfaces());
        //设置下拉列表风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //将适配器添加到spinner中去
        spinnerNetworks.setAdapter(adapter);
        spinnerNetworks.setOnItemSelectedListener(this);

        mainPresenter.initPackageData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item_text = spinnerNetworks.getAdapter().getItem(position).toString();
        Log.d("selected", item_text);
        if(item_text.equals("自定义")){
            srcIPLinearLayout.setAlpha(0f);
            srcIPLinearLayout.setVisibility(View.VISIBLE);
            srcIPLinearLayout.animate().alpha(1f).setDuration(500);
        }else{
            srcIPLinearLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        RadioButton rd = (RadioButton) findViewById(checkedId);
        switch (group.getId()){
            case R.id.radio_group_socket:
                socketType = rd.getText().toString();
                Log.d("socket checked", socketType);
                break;
            case R.id.radio_group_package:
                packageType = rd.getText().toString();
                Log.d("package checked", packageType);
                break;
        }
    }

    private void tvClear(){
        tvReceive.setText("");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_clear:
                Log.d("click", "CLEAR");
                tvClear();
                break;
            case R.id.btn_send:
                Log.d("click", "CLICK");
                mainPresenter.socketSend(editDstIP.getText().toString(), socketType, packageType);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_modify:
                new AlertDialog.Builder(MainActivity.this).setTitle("选择修改的报文").setItems(
                        new String[]{getString(R.string.action_register), getString(R.string.action_invite), getString(R.string.action_option), getString(R.string.action_refer)}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(MainActivity.this, PackageActivity.class);
                                switch (which){
                                    case 0:
                                        Log.d("package", "REGISTER");
                                        intent.putExtra("msg", "REGISTER");
                                        intent.putExtra("data", mainPresenter.getPackageData("REGISTER"));
                                        break;
                                    case 1:
                                        Log.d("package", "INVITE");
                                        intent.putExtra("msg", "INVITE");
                                        intent.putExtra("data", mainPresenter.getPackageData("INVITE"));
                                        break;
                                    case 2:
                                        Log.d("package", "OPTION");
                                        intent.putExtra("msg", "OPTION");
                                        intent.putExtra("data", mainPresenter.getPackageData("OPTION"));
                                        break;
                                    case 3:
                                        Log.d("package", "REFER");
                                        intent.putExtra("msg", "REFER");
                                        intent.putExtra("data", mainPresenter.getPackageData("REFER"));
                                        break;
                                }
                                startActivityForResult(intent, 1000);
                            }
                        }).show();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            String type = intent.getStringExtra("type");
            String data = intent.getStringExtra("data");
            if(requestCode == 1000 && resultCode == 1001)
            {
                mainPresenter.setPackageData(type, data);
            }
        }
        catch (Exception e){
            Log.e("err", e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        // 创建退出对话框
        AlertDialog isExit = new AlertDialog.Builder(this).create();
        // 设置对话框标题
        isExit.setTitle("退出程序");
        // 设置对话框消息
        isExit.setMessage("确定要退出吗");
        // 添加选择按钮并注册监听
        isExit.setButton(AlertDialog.BUTTON_POSITIVE, "确定", backListener);
        isExit.setButton(AlertDialog.BUTTON_NEGATIVE, "取消", backListener);
        // 显示对话框
        isExit.show();
    }

    DialogInterface.OnClickListener backListener = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            switch (which)
            {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                    finish();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void sendSuccess(String msg){
        Log.d("log", msg);
        tvReceive.append(msg);
    }

    @Override
    public void sendFailed(String msg){
        Log.d("log", msg);
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getSrcIP(){
        return editSrcIP.getText().toString();
    }

    @Override
    public String getDstIP(){
        return editDstIP.getText().toString();
    }

    @Override
    public String getNetworkInterface(){
        return spinnerNetworks.getSelectedItem().toString();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public String getIMSI() {
        String imsi = null;
        TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        try{
            imsi = tm.getSubscriberId();
        }catch (Exception e){

        }
        if (imsi == null) {
            try {
                Method getSubId = TelephonyManager.class.getMethod("getSubscriberId", int.class);
                SubscriptionManager sm = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
                imsi = (String) getSubId.invoke(tm, sm.getActiveSubscriptionInfoForSimSlotIndex(0).getSubscriptionId()); // Sim slot 1 IMSI
                return imsi;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return imsi;
    }

}
