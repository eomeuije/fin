package com.example.fin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    //구글맵참조변수
    GoogleMap mMap;
    Location lastLocation;
    String fName = "AlarmList.csv";
    //bus api key
    String key = "0%2BXvCseXelCWRB66ZSWmKJLmed%2BENq9on4sYgzJQm6o2P1uhkiaFr8x58WcbTPEaDtktKzQCtIszeA0ndXQaBg%3D%3D";

    DrawerLayout drawerLayout;
    View drawerView;
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

        @SuppressLint("MissingPermission")
        ActivityResultLauncher<String[]> backPermissionReq = registerForActivityResult(new ActivityResultContracts
                        // 응답 후 익명 함수 실행
                        .RequestMultiplePermissions(), result -> {
                    Boolean background = result.getOrDefault(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);
                    // 응답 결과에 따른 if else
                    if (background) {
                        // 현재 위치를 가져옴
                        LocationManager manager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
                        GPSListener gpsListener = new GPSListener();
                        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
                        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
                        lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        // 구글 fragment의 ID를 참조하고 구글맵을 호출
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                        mapFragment.getMapAsync(this); // 반드시 메인 스레드에서 실행
                    } else{
                        // 주간 알람을 위한 앱 설정 요청 (사용자가 직접 해주어야 함.)
                        Toast.makeText(MainActivity.this, "주간 알람을 위해선 앱 설정에서\n위치 권한 항상 허용을 선택해 주십시오.", Toast.LENGTH_LONG).show();
                    }
                }
        );
                    // 사용자에게 권한 요청
        ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                                                    // 응답 후 익명 함수 실행
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    // 응답 결과에 따른 if else
                    if (fineLocationGranted != null && fineLocationGranted) {
                        backPermissionReq.launch(new String[]{
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        });
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {    // 대략적인 위치 정보만 허용되었을 때
                        Toast.makeText(MainActivity.this, "주간 알람을 위해선 앱 설정에서\n위치 권한 항상 허용을 선택해 주십시오.", Toast.LENGTH_LONG).show();
                        backPermissionReq.launch(new String[]{
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        });
                    } else {    // 권한이 없을 때
                        Toast.makeText(MainActivity.this, "주간 알람을 위해선 앱 설정에서\n위치 권한 항상 허용을 선택해 주십시오.", Toast.LENGTH_LONG).show();
                        backPermissionReq.launch(new String[]{
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        });
                    }
                }
        );

        // 권한 요청 실행
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
        // 버스 검색을 위한 버스 정보 저장
        BusInfoThread busInfothread = new BusInfoThread();
        busInfothread.setDaemon(true);
        busInfothread.start();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        drawerView = (View) findViewById(R.id.drawer);

        Button btnOpenDrawer = (Button) findViewById(R.id.dayAlarmMenu);
        btnOpenDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wKeyfunc();
            }
        });

        edtSearch = (EditText)findViewById(R.id.edtSearch);
        edtSearch.setOnKeyListener(new View.OnKeyListener() {
            @SuppressLint("ResourceType")
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    editKeyFunc();
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
                is.close();
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
                // 문자열로 된 요청 url을 URL객체로 생성
                URL url = new URL(busStopInfoEndPoint + "?serviceKey=" + key);
                // url, 스트림 연결
                InputStream is = url.openStream();
                factory = XmlPullParserFactory.newInstance();
                xpp = factory.newPullParser();
                // xml의 현재 위치 저장
                int eventType = xpp.getEventType();
                // xml parser와 url내용을 연결
                xpp.setInput(new InputStreamReader(is, "UTF-8"));
                String tagName = null;
                int busStopArrIndex = 0;
                // 파싱 시작
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
                is.close();
            } catch (XmlPullParserException | IOException protocolException) {
                protocolException.printStackTrace();
            }
            handler.sendEmptyMessage(-1);
        }
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            // 마커가 모두 찍힌 후 클릭이벤트, 정렬
            if(msg.what == -1){
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if(marker.getTitle().equals("내 위치")){
                            return false;
                        }
                        int id = Integer.parseInt(marker.getSnippet());
                        int fIndex = -1;
                        // 이진 탐색
                        int low = 0;
                        int high = busStopArr.length - 1;
                        int mid;
                        while(low <= high) {
                            mid = (low + high) / 2;
                            if (busStopArr[mid].STATION_ID == id) {
                                fIndex = mid;
                                break;
                            }
                            else if (busStopArr[mid].STATION_ID > id) {
                                high = mid - 1;
                            }
                            else {
                                low = mid + 1;
                            }
                        }
                        // 다이얼로그 선언, 초기화
                        Dialog dlg = new Dialog(MainActivity.this, android.R.style.Theme_NoTitleBar);
                        dlg.setContentView(R.layout.bus_stop_list);
                        Button backButton = (Button)dlg.findViewById(R.id.backButton);
                        TextView busStopText = (TextView)dlg.findViewById(R.id.busStopText);
                        // 뒤로가기 버튼 구현
                        backButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dlg.dismiss();
                            }
                        });
                        // 정류장 이름
                        busStopText.setText(busStopArr[fIndex].STATION_NM + busStopArr[fIndex].STATION_SUB_NM);
                        // 다이얼로그를 띄움
                        dlg.show();
                        ReqBusThread busListThread = new ReqBusThread(busStopArr[fIndex].STATION_ID, dlg, busInfoArr, busStopArr, busStopArr[fIndex].LOCAL_Y, busStopArr[fIndex].LOCAL_X);
                        busListThread.setDaemon(true);
                        busListThread.start();
                        return false;
                    }
                });
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
    @SuppressLint("ResourceType")
    void wKeyfunc(){
        drawerLayout.openDrawer(drawerView);
        byte[] txt = new byte[50000];
        try {
            FileInputStream inFs = openFileInput(fName);
            inFs.read(txt);
            inFs.close();
        } catch (IOException e) {       // 파일이 없을 때 종료
            e.printStackTrace();
            return;
        }
        String str = (new String(txt)).trim();
        String [] alarmList = str.split("\n");
        if(alarmList[0].equals("")){    // 파일 내용이 없을 때 종료
            return;
        }
        LinearLayout alarmLinear = (LinearLayout) findViewById(R.id.weekAlarmLinear);
        alarmLinear.removeAllViews();
        for(int i = 1; i < alarmList.length; i++){
            String [] alarmInfo = alarmList[i].split(",");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            RelativeLayout relativeLayout = new RelativeLayout(MainActivity.this);
            relativeLayout.setLayoutParams(params);
            params.bottomMargin = 1;
            relativeLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.botbodclr));
            //버스 이름
            TextView whatR = new TextView(MainActivity.this);
            whatR.setText(alarmInfo[15]);
            whatR.setTextSize(30);
            whatR.setTextColor(Color.parseColor("#131313"));
            //정류장 이름
            TextView whatS = new TextView(MainActivity.this);
            whatS.setText(alarmInfo[14]);
            whatS.setTextSize(25);
            whatS.setTextColor(Color.parseColor("#131313"));
            //시간
            TextView when = new TextView(MainActivity.this);
            when.setText(alarmInfo[1] + " : " + alarmInfo[2]);
            when.setTextSize(25);
            when.setTextColor(Color.parseColor("#333333"));
            //요일
            TextView week = new TextView(MainActivity.this);
            week.setTextSize(20);
            week.setTextColor(Color.parseColor("#535353"));
            String result = "";
            for(int j = 7; j < 14; j++){
                if(alarmInfo[j].equals("true")){
                    switch (j){
                        case 7:{
                            result += " 일";
                            break;
                        }
                        case 8:{
                            result += " 월";
                            break;
                        }
                        case 9:{
                            result += " 화";
                            break;
                        }
                        case 10:{
                            result += " 수";
                            break;
                        }
                        case 11:{
                            result += " 목";
                            break;
                        }
                        case 12:{
                            result += " 금";
                            break;
                        }
                        case 13:{
                            result += " 토";
                            break;
                        }
                    }
                }
            }
            week.setText(result);
            //자리배치
            RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            param.addRule(RelativeLayout.ALIGN_PARENT_START);
            whatR.setLayoutParams(param);
            whatR.setId(2);

            param = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            param.addRule(RelativeLayout.BELOW, 2);
            whatS.setLayoutParams(param);
            whatS.setId(3);

            param = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            when.setLayoutParams(param);

            param = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            param.addRule(RelativeLayout.BELOW, 3);
            week.setLayoutParams(param);

            relativeLayout.addView(whatR);
            relativeLayout.addView(whatS);
            relativeLayout.addView(when);
            relativeLayout.addView(week);
            alarmLinear.addView(relativeLayout);

            relativeLayout.setTag(alarmInfo[0]);
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String  id = (String) view.getTag();
                    View dialogView = (View)View.inflate(view.getContext(), R.layout.week_dlg, null);
                    AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                    dlg.setView(dialogView);
                    dlg.setTitle("알람 편집");

                    String[] alarmList = null;
                    try {
                        FileInputStream inFs = view.getContext().openFileInput(fName);
                        byte[] txt = new byte[50000];
                        inFs.read(txt);
                        alarmList = (new String(txt)).trim().split("\n");
                        inFs.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String[] aInfo = CreateAlarmDialog.getLine(id, alarmList);
                    ToggleButton[] tBtnArr = new ToggleButton[7];
                    for(int i = 0; i < 7; i++){
                        tBtnArr[i] = (ToggleButton) dialogView.findViewById
                                (getResources().getIdentifier("toggle_" + (i + 1), "id", "com.example.fin"));
                        if (aInfo != null && aInfo[i + 7].equals("true")) {
                            tBtnArr[i].setChecked(true);
                        }
                    }


                    String[] finalAlarmList = alarmList;
                    dlg.setPositiveButton("수정", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            TimePicker timePicker = (TimePicker)dialogView.findViewById(R.id.wTimePicker);
                            int hour;
                            int min;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                hour = timePicker.getHour();
                                min = timePicker.getMinute();
                            }else{
                                hour = timePicker.getCurrentHour();
                                min = timePicker.getCurrentMinute();
                            }
                            aInfo[1] = Integer.toString(hour);
                            aInfo[2] = Integer.toString(min);
                            for(int j = 0; j < 7; j++){
                                if(tBtnArr[j].isChecked()){ // 요일이 체크 되어 있으면
                                    aInfo[j + 7] = "true";  // true
                                }else{                      // 아니면
                                    aInfo[j + 7] = "false"; // false
                                }
                            }
                            String resStr = aInfo[0]+","+aInfo[1]+","+aInfo[2]+","+aInfo[3]+","+aInfo[4]
                                    +","+aInfo[5]+","+aInfo[6]
                                    +","+aInfo[7]+","+aInfo[8]
                                    +","+aInfo[9]+","+aInfo[10]
                                    +","+aInfo[11]+","+aInfo[12]
                                    +","+aInfo[13]
                                    +","+aInfo[14]+","+aInfo[15];
                            for(int j = 1; j < finalAlarmList.length; j++){
                                if(finalAlarmList[j].split(",")[0].equals(id)){
                                    finalAlarmList[j] = resStr;
                                    break;
                                }
                            }
                            try {
                                FileOutputStream outFs = view.getContext().openFileOutput(fName, Context.MODE_PRIVATE);
                                outFs.write(CreateAlarmDialog.getFileStr(finalAlarmList).getBytes());
                                outFs.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //알람 재설정
                            Calendar cal = Calendar.getInstance();
                            if(hour >= 12){
                                hour -= 12;
                                cal.set(Calendar.AM_PM, Calendar.PM);
                            }else{
                                cal.set(Calendar.AM_PM, Calendar.AM);
                            }
                            cal.set(Calendar.HOUR, hour);
                            cal.set(Calendar.MINUTE, min);
                            CreateAlarmDialog.cancelAlarm(id, view.getContext());
                            CreateAlarmDialog.wStartAlarm(id, view.getContext(), cal, aInfo[14], aInfo[15]);
                            wKeyfunc();
                        }
                    });
                    dlg.setNegativeButton("삭제", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for(int j = 1; j < finalAlarmList.length; j++){
                                if(finalAlarmList[j].split(",")[0].equals(id)){
                                    finalAlarmList[j] = "";
                                    break;
                                }
                            }
                            try {
                                FileOutputStream outFs = view.getContext().openFileOutput(fName, Context.MODE_PRIVATE);
                                outFs.write(CreateAlarmDialog.getFileStr(finalAlarmList).getBytes());
                                outFs.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            CreateAlarmDialog.cancelAlarm(id, view.getContext());
                            wKeyfunc();
                        }
                    });
                    dlg.show();
                }
            });
        }
    }
    @SuppressLint("ResourceType")
    void editKeyFunc(){
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
        for(int i = 0; i < busInfoArr.length; i++){
            if(busInfoArr[i].ROUTE_NM.contains(busNM)){
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.bottomMargin = 5;
                RelativeLayout relativeLayout = new RelativeLayout(dlg.getContext());
                relativeLayout.setLayoutParams(params);
                params.bottomMargin = 1;
                relativeLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.botbodclr));
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
                count.setText(Short.toString(busInfoArr[i].STATION_CNT));

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
                relativeLayout.setTag(busInfoArr[i]);
                relativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BusInfo busInfo = (BusInfo) view.getTag();
                        Dialog dlgBus = new Dialog(dlg.getContext(), android.R.style.Theme_NoTitleBar);
                        dlgBus.setContentView(R.layout.bus_stop_list);
                        Button backButton = (Button)dlgBus.findViewById(R.id.backButton);
                        TextView resultText = (TextView)dlgBus.findViewById(R.id.busStopText);
                        LinearLayout searchResult = (LinearLayout)dlgBus.findViewById(R.id.busStopLinearLayout);

                        backButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dlgBus.dismiss();
                            }
                        });
                        resultText.setText(busInfo.ROUTE_NM);
                        dlgBus.show();
                        BusSearchThread busSearchThread = new BusSearchThread(searchResult, busInfo.ROUTE_ID, busStopArr, busInfoArr);
                        busSearchThread.setDaemon(true);
                        busSearchThread.start();
                    }
                });
            }
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