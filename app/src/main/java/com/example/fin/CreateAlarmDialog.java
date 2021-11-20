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
    public static void create(View view, Double stationLat, Double stationLon){
        Bus bus = (Bus)view.getTag();
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
            danger.setText("※주의 시간이 부정확할 수 있습니다.");

            time += BusRoute.getBusTerm(bus.busRouteArr, bus.STATION_ORD);
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
                    ToggleButton toggleSun = (ToggleButton) dialogView.findViewById(R.id.toggle_sun);
                    ToggleButton toggleMon = (ToggleButton) dialogView.findViewById(R.id.toggle_mon);
                    ToggleButton toggleTue = (ToggleButton) dialogView.findViewById(R.id.toggle_tue);
                    ToggleButton toggleWed = (ToggleButton) dialogView.findViewById(R.id.toggle_wed);
                    ToggleButton toggleThu = (ToggleButton) dialogView.findViewById(R.id.toggle_thu);
                    ToggleButton toggleFri = (ToggleButton) dialogView.findViewById(R.id.toggle_fri);
                    ToggleButton toggleSat = (ToggleButton) dialogView.findViewById(R.id.toggle_sat);

                    String fName = "AlarmList.csv";
                    String id = null;
                    FileInputStream inFs = null;
                    FileOutputStream outFs;
                    // 파일이 없으면 파일 생성
                    try{
                        inFs = view.getContext().openFileInput(fName);
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
                        str = id+","+hour+","+min+","+stationLat+","+stationLon
                                +","+bus.busRouteArr[bus.STATION_ORD - 1].STATION_ID+","+bus.ROUTE_ID
                                +","+toggleSun.isChecked()+","+toggleMon.isChecked()
                                +","+toggleTue.isChecked()+","+toggleWed.isChecked()
                                +","+toggleThu.isChecked()+","+toggleFri.isChecked()
                                +","+toggleSat.isChecked()
                                +","+bus.busRouteArr[bus.STATION_ORD - 1].STATION_NM+","+bus.BUS_NM+"\n";
                        alarmList[0] = Integer.toString(Integer.parseInt(id) + 1);

                        String resStr = getFileStr(alarmList) + str;
                        outFs = view.getContext().openFileOutput(fName, Context.MODE_PRIVATE);
                        outFs.write(resStr.getBytes());
                        outFs.close();
                        inFs.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                    // 알람 시작
                    Intent intent = new Intent(view.getContext(), WeekAlarmReceiver.class);
                    intent.putExtra("id", id);
                    PendingIntent pIntent = PendingIntent.getBroadcast(view.getContext(), 1, intent, 0);
                    AlarmManager alarmManager = (AlarmManager) view.getContext().getSystemService(Context.ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);

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
                    // Receiver 설정
                    Intent intent = new Intent(dlg.getContext(), AlarmReceiver.class);
                    // state 값이 on 이면 알람시작, off 이면 중지
                    intent.putExtra("state", "on");

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(dlg.getContext(), 10000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // 알람 설정
                    AlarmManager alarmManager = (AlarmManager) dlg.getContext().getSystemService(ALARM_SERVICE);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
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
            result += arr[i] + "\n";
        }
        return result;
    }
}
