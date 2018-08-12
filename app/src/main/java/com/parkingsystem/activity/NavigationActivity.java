package com.parkingsystem.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.parkingsystem.R;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    public LocationClient mLocationClient;

    private MapView mMapView = null;

    private BaiduMap mBaiduMap = null;

    private BDLocationListener mListener = new MyLocationListener();

    private List<String> permissionList = new ArrayList<>();

    private Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_navigation);

        mMapView = (MapView) findViewById(R.id.map_view);

        //得到BaiduMap的实例
        mBaiduMap = mMapView.getMap();
        //设置Baidu的地图类型，具有MAP_TYPE_NONE、MAP_TYPE_NORMAL和MAP_TYPE_SATELLITE
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);

        //必须主动开启定位功能
        mBaiduMap.setMyLocationEnabled(true);

        //创建LocationClient
        mLocationClient = new LocationClient(getApplicationContext());

        //注册一个回调用的Listener
        mLocationClient.registerLocationListener(mListener);

        //配置一些参数
        initLocationClient();
        //检查权限
        checkPermissionState();
    }

    /**
     * 判断是否有定位所需权限，没有的话，需要运行时申请
     */
    private void checkPermissionState() {

        if (ContextCompat.checkSelfPermission(NavigationActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(NavigationActivity.this,
                Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(NavigationActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(NavigationActivity.this, permissions, 1);
        } else {
            //调用LocationClient的start接口，接下来SDK将完成发送定位请求的工作
            mLocationClient.start();
        }
    }

    /**
     * 设置地图基本配置
      */
    private void initLocationClient() {
        //创建option实例
        //option有很多默认设置，可以按需变更
        LocationClientOption option = new LocationClientOption();

        //设置定位模式，默认高精度
        //有高精度，低功耗，仅设备等模式
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        //设置返回的定位结果坐标系
        //默认gcj02
        option.setCoorType("bd09ll");

        //设置定位间隔，默认为0，即只定位1次
        //此处设置定位请求的间隔大于等于10000ms
        option.setScanSpan(30000);

        //设置是否需要地址信息，默认不需要
        option.setIsNeedAddress(true);

        //设置是否使用gps，默认false,
        option.setOpenGps(true);

        //设置是否当GPS有效时，是否按照1次/s的频率输出GPS结果，默认false
        option.setLocationNotify(true);

        //设置是否需要位置语义化结果，默认false
        //设置为true后，可以在BDLocation.getLocationDescribe里得到类似于“在北京天安门附近”的结果
        option.setIsNeedLocationDescribe(true);

        //设置是否需要POI(Point of Interest，信息点)，默认false，同样可以在BDLocation.getPoiList里得到
        option.setIsNeedLocationPoiList(true);

        //定位SDK内部是一个SERVICE，并放到了独立进程
        //此处设置是否在stop的时候杀死这个进程，默认不杀死
        option.setIgnoreKillProcess(false);

        //设置是否收集CRASH信息，默认收集
        option.SetIgnoreCacheException(false);

        //设置是否需要过滤GPS仿真结果，默认需要
        option.setEnableSimulateGps(false);

        //为LocationClient配置option
        mLocationClient.setLocOption(option);
    }

    //MapView与Activity的生命周期匹配
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);

        mMapView.onDestroy();
    }

    private class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            //BDLocation中含有大量的信息，由之前配置的option决定
            //LocType定义了结果码，例如161表示网络定位成功
            //具体参考sdk文档
            Log.d("ZJTest", "onReceiveLocation: " + bdLocation.getLocType());

            //我在这里利用经度、纬度信息构建坐标点
            LatLng point = new LatLng(bdLocation.getLatitude(),
                    bdLocation.getLongitude());

            //创建一个新的MapStatus
            MapStatus mapStatus = new MapStatus.Builder()
                    //定位到定位点
                    .target(point)
                    //决定缩放的尺寸
                    .zoom(16)
                    .build();
            //利用MapStatus构建一个MapStatusUpdate对象
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
            //更新BaiduMap，此时BaiduMap的界面就会从初始位置（北京），移动到定位点
            mBaiduMap.setMapStatus(mapStatusUpdate);

            //得到定位使用的图标
            Bitmap origin = BitmapFactory.decodeResource(
                    getResources(), R.drawable.before);

            //我自己的图标图片太大了，因此这里做了一下缩放
            Matrix matrix = new Matrix();
            matrix.postScale((float) 0.3, (float) 0.3);
            Bitmap resize = Bitmap.createBitmap(origin, 0, 0,
                    origin.getWidth(), origin.getHeight(),
                    matrix, true);

            //重新构建定位图标
            BitmapDescriptor bitmap = BitmapDescriptorFactory
                    .fromBitmap(resize);

            //利用定位点信息和图标，构建MarkerOption，用于在地图上添加Marker
            MarkerOptions option = new MarkerOptions()
                    .position(point)
                    .icon(bitmap);

            //设置Marker的动画效果，我选的是“生长”
            option.animateType(MarkerOptions.MarkerAnimateType.grow);

            //因为我选择的是不断更新位置，
            //因此，如果之前已经叠加过图标，先移除
            if (mMarker != null) {
                mMarker.remove();
            }

            //在地图上添加Marker，并显示
            mMarker = (Marker)(mBaiduMap.addOverlay(option));
        }

        //这个接口主要返回连接网络的类型
        public void onConnectHotSpotMessage(String s, int i) {
        }
    }
}