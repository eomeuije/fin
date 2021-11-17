package com.example.fin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //구글맵참조변수
    GoogleMap mMap;
    Location lastLocation;
    //bus api key
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";

    EditText edtSearch;
    BusStopInfo [] busStopArr;
    BusInfo [] busInfoArr;
    public static void swap(Object [] arr, int i, int j) {
        Object temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        @SuppressLint("MissingPermission") ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted) {
                        Thread.currentThread().run();
                        LocationManager manager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
                        GPSListener gpsListener = new GPSListener();
                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
                        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
                        lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // SupportMapFragment을 통해 레이아웃에 만든 fragment의 ID를 참조하고 구글맵을 호출한다.
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(this); //getMapAsync must be called on the main thread.
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {

                        // Only approximate location access granted.
                    } else {

                        // No location access granted.
                    }
                }
        );

// ...

// Before you perform the actual permission request, check whether your app
// already has the permissions, and whether your app needs to show a permission
// rationale dialog. For more details, see Request permissions.
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
        BusInfoThread busInfothread = new BusInfoThread();
        busInfothread.setDaemon(true);
        busInfothread.start();

        edtSearch = (EditText)findViewById(R.id.edtSearch);
        edtSearch.setOnKeyListener(new View.OnKeyListener() {
            @SuppressLint("ResourceType")
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    Dialog dlg = new Dialog(MainActivity.this, android.R.style.Theme_NoTitleBar);
                    dlg.setContentView(R.layout.bus_stop_list);
                    Button backButton = (Button)dlg.findViewById(R.id.backButton);
                    TextView resultText = (TextView)dlg.findViewById(R.id.busStopText);
                    LinearLayout searchResult = (LinearLayout)dlg.findViewById(R.id.busStopLinearLayout);

                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dlg.dismiss();
                        }
                    });
                    String busNM = String.valueOf(edtSearch.getText());
                    resultText.setText("검색 결과: " + busNM);
                    dlg.show();
                    for(int i = 0; i < busInfoArr.length; i++){System.out.println(busInfoArr[i].STATION_CNT);
                        if(busInfoArr[i].ROUTE_NM.contains(busNM)){
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.bottomMargin = 5;
                            RelativeLayout relativeLayout = new RelativeLayout(dlg.getContext());
                            relativeLayout.setLayoutParams(params);
                            // 버스 번호
                            TextView what = new TextView(dlg.getContext());
                            what.setTextSize(40);
                            what.setId(2);
                            what.setText(busInfoArr[i].ROUTE_NM);
                            switch (busInfoArr[i].ROUTE_COLOR){
                                case 2:{
                                    what.setTextColor(Color.parseColor("#0055ff"));
                                    break;
                                }
                                case 3:
                                case 1: {
                                    what.setTextColor(Color.parseColor("#00DD00"));
                                    break;
                                }
                                case 5:{
                                    what.setTextColor(Color.parseColor("#ff5500"));
                                    break;
                                }
                                case 6:{
                                    what.setTextColor(Color.parseColor("#ff0044"));
                                    break;
                                }
                                default:{
                                    what.setTextColor(Color.parseColor("#030303"));
                                    break;
                                }
                            }
                            // 정류장 수
                            TextView count = new TextView(dlg.getContext());
                            count.setTextSize(17);
                            count.setTextColor(Color.parseColor("#131313"));
                            count.setText(busInfoArr[i].STATION_CNT);

                            // 정류장 수 코멘트
                            TextView com = new TextView(dlg.getContext());
                            com.setTextSize(10);
                            com.setTextColor(Color.parseColor("#D3D3D3"));
                            com.setText("총 정류장 수");

                            // 레이아웃 디자인
                            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            param.addRule(RelativeLayout.ALIGN_PARENT_START, 1);
                            param.leftMargin = 15;
                            what.setLayoutParams(param);
                            param = new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            count.setLayoutParams(param);
                            param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            param = new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT
                            );
                            param.addRule(RelativeLayout.ALIGN_BOTTOM, 2);
                            param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            com.setLayoutParams(param);
                            relativeLayout.addView(what);
                            relativeLayout.addView(count);
                            relativeLayout.addView(com);
                            searchResult.addView(relativeLayout);
                        }
                    }
                }
                return false;
            }
        });
    }


    @Override //구글맵을 띄울준비가 됐으면 자동호출된다.
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //지도타입 - 일반
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        curLocate();
        BusStop thread = new BusStop();
        thread.setDaemon(true);
        thread.start();
    }

    // 내 위치에 마커
    public void curLocate() {
        // 내 위치 설정
        double lat, lon;
        if(lastLocation == null){
            lat = 0.0;
            lon = 0.0;
        }else{
            lat = lastLocation.getLatitude();
            lon = lastLocation.getLongitude();
        }
        LatLng myLocate = new LatLng(lat, lon);

        // 구글 맵에 표시할 마커에 대한 옵션 설정
        MarkerOptions makerOptions = new MarkerOptions();
        makerOptions
                .position(myLocate)
                .title("내 위치")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .alpha(0.5f);
        mMap.addMarker(makerOptions); //.showInfoWindow();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocate, 16));

    }
    class BusInfoThread extends Thread{
        // for loop를 통한 n개의 마커 생성
        public void run(){
            String busStopInfoEndPoint = "http://openapi.changwon.go.kr/rest/bis/Bus/";
            XmlPullParser xpp;
            XmlPullParserFactory factory;
            try {
                URL url = new URL(busStopInfoEndPoint + "?serviceKey=" + key); //문자열로 된 요청 url을 URL객체로 생성
                InputStream is = url.openStream();
                factory = XmlPullParserFactory.newInstance();
                xpp = factory.newPullParser();
                int eventType = xpp.getEventType();
                xpp.setInput(new InputStreamReader(is, "UTF-8"));
                String tagName = null;
                int busStopArrIndex = 0;
                while(eventType != XmlPullParser.END_DOCUMENT){
                    tagName = xpp.getName();
                    if(tagName == null){
                    }
                    else if(tagName.equals("rowCount")){
                        xpp.next();
                        busInfoArr = new BusInfo[Integer.parseInt(xpp.getText())];
                        xpp.next();
                    }
                    else if(tagName.equals("row")){
                        BusInfo bus = new BusInfo();
                        while(true){
                            xpp.next();
                            tagName = xpp.getName();
                            if(tagName != null){
                                if(tagName.equals("row")){
                                    busInfoArr[busStopArrIndex++] = bus;
                                    break;
                                }
                                switch(tagName) {
                                    case "ROUTE_ID":{
                                        xpp.next();
                                        bus.ROUTE_ID = Integer.parseInt(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "ROUTE_NM":{
                                        xpp.next();
                                        bus.ROUTE_NM=xpp.getText();
                                        xpp.next();
                                        break;
                                    }
                                    case "ORGT_STATION_ID":{
                                        xpp.next();
                                        bus.ORGT_STATION_ID= Integer.parseInt(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "DST_STATION_ID":{
                                        xpp.next();
                                        bus.DST_STATION_ID= Integer.parseInt(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "STATION_CNT":{
                                        xpp.next();
                                        bus.STATION_CNT= Short.parseShort(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "ROUTE_COLOR":{
                                        xpp.next();
                                        bus.ROUTE_COLOR= Short.parseShort(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (XmlPullParserException | IOException protocolException) {
                protocolException.printStackTrace();
            }
            for (int i = 1; i < busInfoArr.length; i++)
                for (int j = i; j > 0; j--)
                    if (busInfoArr[j - 1].ROUTE_ID > busInfoArr[j].ROUTE_ID)
                        swap(busInfoArr, j - 1, j);
                    else
                        break;
        }
    }
    // 맵 정류장 마커
    class BusStop extends Thread{
        // for loop를 통한 n개의 마커 생성
        public void run(){
            String busStopInfoEndPoint = "http://openapi.changwon.go.kr/rest/bis/Station/";
            XmlPullParser xpp;
            XmlPullParserFactory factory;
            try {
                URL url = new URL(busStopInfoEndPoint + "?serviceKey=" + key); //문자열로 된 요청 url을 URL객체로 생성
                InputStream is = url.openStream();
                factory = XmlPullParserFactory.newInstance();
                xpp = factory.newPullParser();
                int eventType = xpp.getEventType();
                xpp.setInput(new InputStreamReader(is, "UTF-8"));
                String tagName = null;
                int busStopArrIndex = 0;
                while(eventType != XmlPullParser.END_DOCUMENT){
                    tagName = xpp.getName();
                    if(tagName == null){
                        eventType = xpp.next();
                        continue;
                    }
                    if(tagName.equals("rowCount")){
                        xpp.next();
                        busStopArr = new BusStopInfo[Integer.parseInt(xpp.getText())];
                        xpp.next();
                    }
                    if(tagName.equals("row")){
                        BusStopInfo busStop = new BusStopInfo();
                        while(true){
                            xpp.next();
                            tagName = xpp.getName();
                            if(tagName != null){
                                if(tagName.equals("row")){
                                    Message message = handler.obtainMessage();
                                    message.obj = busStop;
                                    message.what = busStopArrIndex++;
                                    handler.sendMessage(message);
                                    break;
                                }
                                switch(tagName) {
                                    case "STATION_ID":{
                                        xpp.next();
                                        busStop.STATION_ID= Integer.parseInt(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "STATION_NM":{
                                        xpp.next();
                                        busStop.STATION_NM=xpp.getText();
                                        xpp.next();
                                        break;
                                    }
                                    case "STATION_SUB_NM":{
                                        xpp.next();
                                        busStop.STATION_SUB_NM=xpp.getText();
                                        xpp.next();
                                        break;
                                    }
                                    case "LOCAL_X":{
                                        xpp.next();
                                        busStop.LOCAL_X= Double.parseDouble(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                    case "LOCAL_Y":{
                                        xpp.next();
                                        busStop.LOCAL_Y= Double.parseDouble(xpp.getText());
                                        xpp.next();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (XmlPullParserException | IOException protocolException) {
                protocolException.printStackTrace();
            }
            handler.sendEmptyMessage(-1);
        }
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // insertion sort
            if(msg.what == -1){
                for (int i = 1; i < busStopArr.length; i++)
                    for (int j = i; j > 0; j--)
                        if (busStopArr[j - 1].STATION_ID > busStopArr[j].STATION_ID)
                            swap(busStopArr, j - 1, j);
			            else
                            break;
                return;
            }
            MarkerOptions makerOptions = new MarkerOptions();
            int index = msg.what;
            busStopArr[index] = (BusStopInfo) msg.obj;
            if (!(busStopArr[index].STATION_SUB_NM == null) && !busStopArr[index].STATION_SUB_NM.equals("null")) {
                busStopArr[index].STATION_SUB_NM = "(" + busStopArr[index].STATION_SUB_NM + ")";
            }else{
                busStopArr[index].STATION_SUB_NM = "";
            }
            makerOptions
                    .position(new LatLng(busStopArr[index].LOCAL_Y, busStopArr[index].LOCAL_X))
                    .title(busStopArr[index].STATION_NM + busStopArr[index].STATION_SUB_NM) // 타이틀.
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .alpha(0.5f)
                    .snippet(String.valueOf(busStopArr[index].STATION_ID));
            mMap.addMarker(makerOptions);
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(marker.getTitle().equals("내 위치")){
                        return false;
                    }
                    String id = marker.getSnippet();
                    int fIndex = -1;
                    for(int i = 0; i < busStopArr.length; i++){
                        if(busStopArr[i].STATION_ID == Integer.parseInt(id)){
                               fIndex = i;
                        }
                    }
                    Dialog dlg = new Dialog(MainActivity.this, android.R.style.Theme_NoTitleBar);
                    dlg.setContentView(R.layout.bus_stop_list);
                    Button backButton = (Button)dlg.findViewById(R.id.backButton);
                    TextView busStopText = (TextView)dlg.findViewById(R.id.busStopText);
                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dlg.dismiss();
                        }
                    });
                    busStopText.setText(busStopArr[fIndex].STATION_NM + busStopArr[fIndex].STATION_SUB_NM);
                    dlg.show();
                    ReqBusList busListThread = new ReqBusList(busStopArr[fIndex].STATION_ID, dlg, busInfoArr, busStopArr, busStopArr[fIndex].LOCAL_Y, busStopArr[fIndex].LOCAL_X);
                    busListThread.setDaemon(true);
                    busListThread.start();
                    return false;
                }
            });
        }
    };

    public static class GPSListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Double latitude = location.getLatitude();
            Double longitude = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}

class BusStopInfo{
    public int STATION_ID;
    public String STATION_NM;
    public String STATION_SUB_NM = null;
    public double LOCAL_X;
    public double LOCAL_Y;
}
class BusInfo{
    public int ROUTE_ID;         // 노선 ID
    public String ROUTE_NM;         // 버스 번호
    public int ORGT_STATION_ID;  // 출발 정류장
    public int DST_STATION_ID;   // 도착 정류장
    public short STATION_CNT;      // 정류장 수
    public short ROUTE_COLOR;      // 노선 색깔
}