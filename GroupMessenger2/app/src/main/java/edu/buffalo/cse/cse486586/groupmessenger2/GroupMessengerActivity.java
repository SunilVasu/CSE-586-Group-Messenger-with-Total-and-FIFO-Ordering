package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    //IMPL
    //static final String TAG = GroupMessengerActivity.class.getSimpleName();
    String TAG = GroupMessengerActivity.class.getSimpleName();

    //Reference PA1
    //ports declarations
    static final int SERVER_PORT = 10000;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final String[] PORTS = {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};

    static int keyNum=0;
    private final Uri mUri  = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
    String myPort;
    private static PriorityQueue<Message> MessageQueue = new PriorityQueue();
    private int maxProposed = 0;
    private double finalMax=0;
    private static HashMap<String, Message> bufferHashMap= new HashMap<String, Message>();
    private int failed = -1;
    public LinkedList<Message> msgList = new LinkedList<Message>();
    private final HashMap<Integer,String> portHashMap= new HashMap<Integer, String>();





    final ContentValues mContentValues = new ContentValues();

    //Reference - OnPTestClickListener
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    //IMPL-ends
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        portHashMap.put(0,"11108");
        portHashMap.put(1,"11112");
        portHashMap.put(2,"11116");
        portHashMap.put(3,"11120");
        portHashMap.put(4,"11124");
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        Log.v(TAG, "*** Inside onCreate ****");

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        //Reference PA1 - hack for the ports to communicate on 10000
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        TAG = TAG+myPort;

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        //Reference PA1
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            Log.v(TAG, "**** ServerSocket Created ***");
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        //Button for sent
        final Button button;
        button =(Button) findViewById(R.id.button4);


        //Sent mesg when enter pressed
        //Reference PA 1
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // This is one way to reset the input box.
                    TextView tv = (TextView) findViewById(R.id.textView1);
                    tv.append("\t" + msg +"\n"); // This is one way to display a string.

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });

        //Sent msg when button 4 is pressed. Activity on this button press
        //Reference - http://stackoverflow.com/questions/20156733/how-to-add-button-click-event-in-android-studio
        //(1) - http://stackoverflow.com/questions/16636752/android-button-onclicklistener
        button.setOnClickListener(new View.OnClickListener() {
            //onClick
            public void onClick(View v)
            {
                //Reference - PA1
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView tv = (TextView) findViewById(R.id.textView1);
                tv.setMovementMethod(new ScrollingMovementMethod());
                tv.append("\t" + msg + "\n"); // This is one way to display a string.

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    //Reference PA1
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            boolean check = true;

            try {
                while(check) {
                    //REFERENCES:-
                    //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                    //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                    //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                    Socket socket = serverSocket.accept();
                    DataInputStream dataIn = null;
                    DataOutputStream dataOut = null;
                    //String failedProc=null;


                    if(!socket.isInputShutdown()) {
                        //Read the message over the socket
                        dataIn = new DataInputStream(socket.getInputStream());
                        String msg = (String) dataIn.readUTF();
                        Log.e(TAG,"Server Side Msg Received: "+msg);
                        String uniqIdentifer;
                        //Log.e(TAG,"Server msg : "+msg);


                        if(msg.startsWith("failPr#")){
                            //Sent Ack

                            failed =Integer.parseInt(msg.substring(7));
                            Log.e(TAG,"Checking failedProc Server : "+failed);
                            dataOut = new DataOutputStream(socket.getOutputStream());
                            dataOut.writeUTF("PA-1_OK");
                        }

                        String logger="";
                        for(Message msgQueueObj:MessageQueue){
                            logger = logger+msgQueueObj.getMsg()+"."+msgQueueObj.getStatus()+"--Priority--"+msgQueueObj.getPriority()+"--Sender--"+msgQueueObj.getSender() +" $$$ ";
                        }
                        Log.e(TAG,"Queue: "+logger);


                        if(failed!=-1){
                            //Log.e(TAG,"inside if :");
                            for(Message msgQueueObj:MessageQueue){
                                //Log.e(TAG,"inside if sender :" +msgQueueObj.getSender() + " failed: "+ failed);
                                //logger = logger+msgQueueObj.getMsg()+"."+msgQueueObj.getStatus()+"."+msgQueueObj.getPriority()+"."+msgQueueObj.getSender() +" $$$ ";
                                if(msgQueueObj.getSender()==failed){
                                    msgList.add(msgQueueObj);
                                }
                            }

                            for(Message msgListElem: msgList){
                                //Log.e(TAG,"Queue inside loop : "+msgListElem.getPriority());
                                if(MessageQueue.contains(msgListElem)){
                                    MessageQueue.remove(msgListElem);
                                    Log.e(TAG,"Queue -- msg removed: "+msgListElem.getPriority());
                                }
                            }


                        }


                        if(msg.startsWith("initial#")){
                            String msgInitial =msg.substring(8);
                            String[] output = msgInitial.split("##");
                            msgInitial = output[1];
                            Log.e(TAG,"Checking message at initial#: "+msgInitial);
                            String senderPort = output[0];
                            int senderProcessor=0;

                            Log.e(TAG,"initial# Checking Port: "+senderPort);
                            for (int i=0; i<5; i++){
                                if(failed!=-1){
                                    if(i==failed){
                                        continue;
                                    }
                                }
                                int flag=0;
                                if(PORTS[i].equals(senderPort))
                                {
                                    senderProcessor=i;
                                    flag=1;
                                }
                                if(flag==1) {
                                    break;
                                }
                            }

                            Message msgObj = new Message(msgInitial,false,maxProposed,senderProcessor);
                            uniqIdentifer = senderProcessor+msgInitial;
                            MessageQueue.add(msgObj);
                            bufferHashMap.put(msgInitial,msgObj);

                            //Sent Ack

                            dataOut = new DataOutputStream(socket.getOutputStream());
                            dataOut.writeUTF("maxPro#"+maxProposed);

                            Log.e(TAG,"Initial Proposed#---"+maxProposed+"."+senderProcessor+"**"+msgObj.getMsg());


                            maxProposed++;
                            //Using Thread Sleep as this solved the intermittent failure
                            //Thread.sleep(500);



                        }
                        else if (msg.startsWith("forPro#")){
                            String msgPro =msg.substring(7);
                            String[] output = msgPro.split("###");
                            uniqIdentifer = output[1];
                            int agreed_num = Integer.parseInt(output[0]);

                           // Log.e(TAG,"******Final Agreed#***"+agreed_num+"__"+uniqIdentifer);
                            Message msgForPro = bufferHashMap.get(uniqIdentifer);
                            MessageQueue.remove(msgForPro);
                            int senderProcessor = msgForPro.getSender();




                            finalMax = agreed_num + (senderProcessor/10.0);
                            maxProposed = Math.max(agreed_num+1,maxProposed);

                            Log.e(TAG,"Final Agreed#---"+finalMax+"---"+msgForPro.getMsg());
                            msgForPro.setPriority(finalMax);
                            msgForPro.setStatus(true);

                            MessageQueue.add(msgForPro);

                            if(!msg.equals(null)){
                                dataOut = new DataOutputStream(socket.getOutputStream());
                                dataOut.writeUTF("PA-1_OK");
                                //publishProgress(msg);
                                //Using Thread Sleep as this solved the intermittent failure
                                //Thread.sleep(500);
                               // Log.e(TAG,"***Inside Ack forProposal***");
                            }
                        }


                        while (MessageQueue.size()>0 && MessageQueue.peek().getStatus()) {
                                    Message msgObject = MessageQueue.remove();
                                    String msgToPublish = msgObject.getMsg();

                                    publishProgress(msgToPublish);
                                }



                        //Sent Ack


                    }
                   // dataIn.close();
                    //dataOut.close();
                    socket.close();
                  /*if(!socket.isConnected())
                {check = false;}*/
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"ServerTask Exception - IOException");
            }


            return null;
            // here
        }

        protected void onProgressUpdate(String...strings) {
            //REFERENECE - PA1
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");
            tv.append("\n");


            try {

                //REFERENCE - PA1
                mContentValues.put("key",Integer.toString(keyNum));
                mContentValues.put("value", strReceived);
                keyNum++;
                //Log.v(TAG,"*********\n");
                //Log.v(TAG, "key: "+mContentValues.get("key")+"\t value: "+mContentValues.get("value"));

                getContentResolver().insert(mUri, mContentValues);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            return;
        }


    }

    //Reference PA1
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                    int maxCounter=-1;
                    String msgToSend = msgs[0];

                msgToSend = "initial#" +myPort+"##"+ msgToSend;
                int count3=0;
                for (String remotePort : PORTS) {

                    try{

                        if(failed!=-1){
                            if(count3==failed){
                                count3++;
                                continue;
                            }

                        }

                    Log.e(TAG,"Sending: "+msgToSend);
                    count3++;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));


                    boolean check = true;
                    DataOutputStream dataOut = null;
                    DataInputStream dataIn = null;
                    if (!msgToSend.equals("") && socket.isConnected()) {
                        //REFERENCES:-
                        //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                        //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                        //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                        //Send a message over the socket
                        dataOut = new DataOutputStream(socket.getOutputStream());
                        dataOut.writeUTF(msgToSend);
                        dataOut.flush();
                        //Using Thread Sleep as this solved the intermittent failure
                        //Thread.sleep(500);

                        //Wait for an acknowledgement

                        dataIn = new DataInputStream(socket.getInputStream());
                        String maxPro = (String) dataIn.readUTF();
                        if (maxPro.startsWith("maxPro#")) {
                            String maxProposed = maxPro.substring(7);
                            if (maxCounter < Integer.parseInt(maxProposed)) {
                                maxCounter = Integer.parseInt(maxProposed);
                                //Log.e(TAG, "Inside Client maxPro#---" + msgToSend + maxCounter);
                            }
                        }


                        //Closing Resources
                        //dataOut.close();
                        //dataIn.close();

                        socket.close();

                        //count3++;
                    }


                }
                  catch(Exception e){
                        if(failed==-1){
                        //failed=Integer.parseInt(remotePort);
                        failed = count3-1;
                        }
                      Log.e(TAG, "Exception in Client Initial: failed : " + failed);
                    }


                }


                int count2=0;
                for (String remotePort : PORTS) {

                    try{
                        if(failed!=-1){
                        if(count2==failed){
                            count2++;
                            continue;
                        }}


                    count2++;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    boolean check = true;
                    DataOutputStream dataOut = null;
                    DataInputStream dataIn = null;

                    int senderPort = socket.getPort();
                    int senderProcessor = 0;
                    for (int i = 0; i < 5; i++) {
                        if (Integer.parseInt(PORTS[i]) == senderPort) {
                            senderProcessor = i;
                            break;
                        }
                    }

                    String msgToSend2 = "forPro#" + maxCounter + "###" + msgs[0];

                        Log.e(TAG,"Sending Agreed: "+ msgToSend2);
                        if (!msgToSend2.equals("") && socket.isConnected()) {
                        //REFERENCES:-
                        //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                        //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                        //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                        //Send a message over the socket
                        dataOut = new DataOutputStream(socket.getOutputStream());
                        dataOut.writeUTF(msgToSend2);
                        dataOut.flush();
                        //Using Thread Sleep as this solved the intermittent failure
                        //Thread.sleep(500);
                        do {
                            //Wait for an acknowledgement
                            dataIn = new DataInputStream(socket.getInputStream());
                            String msg = (String) dataIn.readUTF();
                            if (msg.equals("PA-1_OK")) {
                                check = false;
                            }
                        } while (check);


                    }


                    //Closing Resources
                    //dataOut.close();
                    //dataIn.close();

                    socket.close();
                        //count2++;
                }
                catch(Exception e){
                    if(failed==-1){
                    //failed = Integer.parseInt(remotePort);
                        failed = count2-1; }
                    Log.e(TAG, "Exception in Client Proposal: failed : " + failed);

                }

                }





                //Sending the failed node to other processors
                //if(failed==11108 || failed==11112|| failed==11116|| failed==11120||failed==11124){
                if(failed!=-1){
                    int count=0;
                    int i=0;
                    String failedRemotePort="0";
                  /*  for (String remotePort : PORTS){
                        if(failed==Integer.parseInt(remotePort)){
                            failed=i;
                            failedRemotePort = remotePort;
                            break;
                        }
                        i++;
                    }*/
                        for (String remotePort : PORTS) {

                        try {
                            //failedRemotePort = portHashMap.get(failed);
                           // if (!failedRemotePort.equals(remotePort))
                            {
                                if (count == failed) {
                                    count++;
                                    continue;
                                }
                                count++;

                                //Log.e(TAG, "Checking failedProc Client failedRemotePort: "+failedRemotePort+"remotePort: "+remotePort);

                            String rp;


                                Log.e(TAG,"Notifying the failure to port: "+remotePort);
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                            boolean check = true;
                            DataOutputStream dataOut = null;
                            DataInputStream dataIn = null;


                            String msgToSend2 = "failPr#" + failed;
                            //Log.e(TAG, "Checking failedProc Client : " + msgToSend2 +"  Socket :"+Integer.parseInt(remotePort)/2);
                            if (!msgToSend2.equals("") && socket.isConnected()) {
                                //REFERENCES:-
                                //https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html
                                //http://stackoverflow.com/questions/15479697/printwriter-or-any-other-output-stream-in-java-do-not-know-r-n
                                //http://java2s.com/Tutorials/Java/Socket/How_to_read_data_from_Socket_connectin_using_Java.htm

                                //Send a message over the socket
                                dataOut = new DataOutputStream(socket.getOutputStream());
                                dataOut.writeUTF(msgToSend2);
                                dataOut.flush();
                                //Using Thread Sleep as this solved the intermittent failure
                                //Thread.sleep(500);
                                do {
                                    //Wait for an acknowledgement
                                    dataIn = new DataInputStream(socket.getInputStream());
                                    String msg = (String) dataIn.readUTF();
                                    if (msg.equals("PA-1_OK")) {
                                        check = false;
                                    }
                                } while (check);

                            }


                            //Closing Resources
                            //dataOut.close();
                            //dataIn.close();

                            socket.close();
                        }
                        }
                        catch(Exception e){
                            Log.e(TAG,"Checking failedProc Client Exception in  failPr#--"+failed+"---"+e);

                        }

                    }

                }


                } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class Message implements Comparable<Message> {
        public String msg;
        public boolean status;
        public double priorty;
        public int sender;

        public Message(String msg, boolean status, double priorty, int sender){
            this.msg = msg;
            this.priorty = priorty;
            this.sender = sender;
            this.status = status;
        }
        public void setPriority(double priorty){
            this.priorty = priorty;
        }
        public boolean getStatus(){
            return status;
        }
        public void setStatus(boolean val){
            this.status=val;
        }


        public String getMsg(){
            return this.msg;
        }

        public int getSender(){
            return this.sender;
        }
        public double getPriority() {
            return this.priorty;
        }
        @Override
        public int compareTo(Message another) {
            if(this.priorty < another.priorty){
                return -1;
            }
            else if(this.priorty == another.priorty){
                return 0;
            }
            else{
                return 1;
            }

        }



    }



}
