package com.example.fin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;

public class RebootReceiver extends BroadcastReceiver {
    String fName = "AlarmList.csv";
    @Override
    public void onReceive(Context context, Intent intent) {
        String[] alarmList = null;
        try {
            FileInputStream inFs = context.openFileInput(fName);
            byte[] txt = new byte[50000];
            inFs.read(txt);
            alarmList = (new String(txt)).trim().split("\n");
            inFs.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String[] alarmInfo;
        Calendar cal = Calendar.getInstance();
        int hour, min;
        for (int i = 1; i < alarmList.length; i++){
            alarmInfo = alarmList[i].split(",");
            hour = Integer.parseInt(alarmInfo[1]);
            min = Integer.parseInt(alarmInfo[2]);
            if(hour >= 12){
                hour -= 12;
                cal.set(Calendar.AM_PM, Calendar.PM);
            }else {
                cal.set(Calendar.AM_PM, Calendar.AM);
            }
            cal.set(Calendar.HOUR, hour);
            cal.set(Calendar.MINUTE, min);
            CreateAlarmDialog.wStartAlarm(alarmInfo[0], context, cal, alarmInfo[14], alarmInfo[15]);
        }
    }
}
