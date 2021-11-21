package com.example.fin;

import static android.content.Context.ALARM_SERVICE;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class CreateAlarmDialog {
    @SuppressLint("MissingPermission")
    public static void create(View view, Bus bus, Double stationLat, Double stationLon){
        View dialogView = (View)View.inflate(view.getContext(), R.layout.dialog_alarm, null);
        AlertDialog.Builder dlg = new AlertDialog.Builder(view.getContext());
        dlg.setView(dialogView);

        TextView title = (TextView)dialogView.findViewById(R.id.title);
        title.setText(bus.BUS_NM + "\n" + bus.busRouteArr[bus.STATION_ORD - 1].STATION_NM);

        TabHost tabHost = (TabHost) dialogView.findViewById(R.id.tabHost);
        tabHost.setup();


        TabHost.TabSpec tabSpecArtist = tabHost.newTabSpec("dayAlarm").setIndicator("주간알람설정");
        tabSpecArtist.setContent(R.id.weekAlarm);
        tabHost.addTab(tabSpecArtist);

        TabHost.TabSpec tabSpecSong = tabHost.newTabSpec("temporalAlarm").setIndicator("일시알람설정");
        tabSpecSong.setContent(R.id.temporalAlarm);
        tabHost.addTab(tabSpecSong);

        tabHost.setCurrentTab(1);

        // 나가야 할 시간 계산
        LocationManager manager=(LocationManager) view.getContext().getSystemService(Context.LOCATION_SERVICE);
        MainActivity.GPSListener gpsListener = new MainActivity.GPSListener();
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsListener);
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsListener);
        Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        LocationDistance ld = new LocationDistance();

        int time = (int) (ld.distance(lastLocation.getLatitude(), lastLocation.getLongitude(), stationLat, stationLon) / 5.0 * 1.8 * 3600);
        time = bus.PREDICT_TRAV_TM - time;

        if(time < 0){       // 0보다 작으면 그 다음 버스나 이미 지난 버스의 위치로 배차간격을 계산해 시간을 예상한다.
            TextView danger = (TextView) dialogView.findViewById(R.id.dangerMessage);

            time += BusRoute.getBusTerm(bus.busRouteArr, bus.STATION_ORD);
            if(time < 0){
                danger.setText("※주의: 알맞은 시간을 찾을 수 없습니다.");
            }else {
                danger.setText("※주의: 시간이 부정확할 수 있습니다.");
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, time);
        TimePicker timePicker = (TimePicker)dialogView.findViewById(R.id.timePicker);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(cal.get(Calendar.AM_PM) == 1) {
                timePicker.setHour(cal.get(Calendar.HOUR) + 12);
            }else{
                timePicker.setHour(cal.get(Calendar.HOUR));
            }
            timePicker.setMinute(cal.get(Calendar.MINUTE));
        }
        // 확인 버튼 클릭시
        dlg.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(tabHost.getCurrentTab() == 0) {
                    TimePicker wTimePicker = (TimePicker) dialogView.findViewById(R.id.wTimePicker);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(wTimePicker.getHour() >= 12) {
                            cal.set(Calendar.HOUR, wTimePicker.getHour() - 12);
                            cal.set(Calendar.AM_PM, 1);
                        }else{
                            cal.set(Calendar.HOUR, wTimePicker.getHour());
                            cal.set(Calendar.AM_PM, 0);
                        }
                        cal.set(Calendar.MINUTE, wTimePicker.getMinute());
                    } else {
                        if(wTimePicker.getCurrentHour() >= 12) {
                            cal.set(Calendar.HOUR, wTimePicker.getCurrentHour() - 12);
                            cal.set(Calendar.AM_PM, 1);
                        }else{
                            cal.set(Calendar.HOUR, wTimePicker.getCurrentHour());
                            cal.set(Calendar.AM_PM, 0);
                        }
                        cal.set(Calendar.MINUTE, wTimePicker.getCurrentMinute());
                    }
                    ToggleButton[] tBtnArr = new ToggleButton[7];
                    for(int j = 0; j < 7; j++){
                        tBtnArr[j] = (ToggleButton) dialogView.findViewById
                                (dialogView.getResources().getIdentifier("toggle_" + (j + 1), "id", "com.example.fin"));
                    }

                    String fName = "AlarmList.csv";
                    String id = null;
                    FileInputStream inFs = null;
                    FileOutputStream outFs;
                    try{
                        inFs = view.getContext().openFileInput(fName);
                        // 파일이 없으면 파일 생성
                    }catch (IOException e){
                        try {
                            outFs = view.getContext().openFileOutput(fName, Context.MODE_PRIVATE);
                            String first = "0\n";
                            outFs.write(first.getBytes());
                            outFs.close();
                            inFs = view.getContext().openFileInput(fName);
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    }
                    try {
                        byte[] txt = new byte[50000];
                        inFs.read(txt);
                        String str = (new String(txt)).trim();
                        String[] alarmList = str.split("\n");
                        id = alarmList[0];
                        int hour = cal.get(Calendar.HOUR);
                        int min = cal.get(Calendar.MINUTE);
                        if(cal.get(Calendar.AM_PM) == 1){
                            hour += 12;
                        }
                        // 0ID 1시 2분 3정류장위도 4정류장경도 5정류장ID 6버스ID 78910111213일월화수목금토 14정류장이름 15버스이름
                        String appStr = id+","+hour+","+min+","+stationLat+","+stationLon
                                +","+bus.busRouteArr[bus.STATION_ORD - 1].STATION_ID+","+bus.ROUTE_ID
                                +","+tBtnArr[0].isChecked()+","+tBtnArr[1].isChecked()
                                +","+tBtnArr[2].isChecked()+","+tBtnArr[3].isChecked()
                                +","+tBtnArr[4].isChecked()+","+tBtnArr[5].isChecked()
                                +","+tBtnArr[6].isChecked()
                                +","+bus.busRouteArr[bus.STATION_ORD - 1].STATION_NM+","+bus.BUS_NM+"\n";
                        alarmList[0] = Integer.toString(Integer.parseInt(id) + 1);

                        String resStr = getFileStr(alarmList) + appStr;
                        outFs = view.getContext().openFileOutput(fName, Context.MODE_PRIVATE);
                        outFs.write(resStr.getBytes());
                        outFs.close();
                        inFs.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                    // 알람 시작
                    wStartAlarm(id, view.getContext(), cal, bus.busRouteArr[bus.STATION_ORD - 1].STATION_NM, bus.BUS_NM);
                    // 토스트 메시지
                    Toast.makeText(dialogView.getContext(), "알람이 설정되었습니다.", Toast.LENGTH_LONG).show();

                }else if(tabHost.getCurrentTab() == 1){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if(timePicker.getHour() >= 12) {
                            cal.set(Calendar.HOUR, timePicker.getHour() - 12);
                            cal.set(Calendar.AM_PM, 1);
                        }else{
                            cal.set(Calendar.HOUR, timePicker.getHour());
                            cal.set(Calendar.AM_PM, 0);
                        }
                        cal.set(Calendar.MINUTE, timePicker.getMinute());
                    } else {
                        if(timePicker.getCurrentHour() >= 12) {
                            cal.set(Calendar.HOUR, timePicker.getCurrentHour() - 12);
                            cal.set(Calendar.AM_PM, 1);
                        }else{
                            cal.set(Calendar.HOUR, timePicker.getCurrentHour());
                            cal.set(Calendar.AM_PM, 0);
                        }
                        cal.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                    }
                    // 알람 설정
                    startAlarm(dlg.getContext(), cal, bus.busRouteArr[bus.STATION_ORD - 1].STATION_NM, bus.BUS_NM);
                    // 토스트 메시지
                    Toast.makeText(dialogView.getContext(), cal.getTime() + "\n알람이 설정되었습니다.", Toast.LENGTH_LONG).show();
                }

            }
        });
        dlg.setNegativeButton("취소", null);
        dlg.show();
    }
    static String getFileStr(String [] arr){
        String result = "";
        for(int i = 0; i < arr.length; i++){
            if(arr[i].equals("")){
                continue;
            }
            result += arr[i] + "\n";
        }
        return result;
    }
    static void cancelAlarm(String  id, Context context){
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, Integer.parseInt(id), intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pIntent);
    }
    static void wStartAlarm(String id, Context context, Calendar cal, String stationNM, String busNM){
        Intent intent = new Intent(context, WeekAlarmReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("stationNM", stationNM);
        intent.putExtra("busNM", busNM);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, Integer.parseInt(id), intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 24 * 60 * 60 * 1000, pIntent);
    }
    static void startAlarm(Context context, Calendar cal, String stationNM, String busNM){
        // Receiver 설정
        Intent newIntent = new Intent(context, AlarmReceiver.class);
        // state 값이 on 이면 알람시작, off 이면 중지
        newIntent.putExtra("state", "on");
        newIntent.putExtra("stationNM", stationNM);
        newIntent.putExtra("busNM", busNM);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 10000, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 알람 설정
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }
    static String[] getLine(String id, String [] alarmList){
        for(int i = 1; i< alarmList.length; i++){
            String[] alarmInfo = alarmList[i].split(",");
            if(alarmInfo[0].equals(id)){
                return alarmInfo;
            }
        }
        return null;
    }
}
