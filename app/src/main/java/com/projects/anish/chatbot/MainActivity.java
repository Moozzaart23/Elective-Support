package com.projects.anish.chatbot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.IOException;
import android.Manifest;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //private static final String TAG = "ChatActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private ImageView buttonSend;
    private boolean side = true;
    public Bot bot;
    public static Chat chat;
    public int pos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        buttonSend = (ImageView) findViewById(R.id.send);
        listView = (ListView) findViewById(R.id.msgview);
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        chatText = (EditText) findViewById(R.id.msg);

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED | ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, 0x12345);
        }

        if (savedInstanceState != null) {
            ArrayList<ChatMessage> values = savedInstanceState.getParcelableArrayList("key");
            if (values != null) {
                chatArrayAdapter.addAll(values);
                chatArrayAdapter.notifyDataSetChanged();
                Toast.makeText(this,"Retrieved data",Toast.LENGTH_LONG).show();
            }
        }
        listView.setAdapter(chatArrayAdapter);

        AssetManager assets = getResources().getAssets();
        File jayDir = new File(getCacheDir().toString() + "/yash/bots/jarvis");
        boolean b = jayDir.mkdirs();

        if (jayDir.exists()) {
            //Reading the file
            try {
                for (String dir : assets.list("jarvis")) {
                    File subdir = new File(jayDir.getPath() + "/" + dir);
                    boolean subdir_check = subdir.mkdirs();
                    for (String file : assets.list("jarvis/" + dir)) {
                        File f = new File(jayDir.getPath() + "/" + dir + "/" + file);
                        if (f.exists()) {
                            continue;
                        }
                        InputStream in = null;
                        OutputStream out = null;
                        in = assets.open("jarvis/" + dir + "/" + file);
                        out = new FileOutputStream(jayDir.getPath() + "/" + dir + "/" + file);
                        //copy file from assets to the mobile's SD card or any secondary memory
                        copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //get the working directory
        MagicStrings.root_path = getCacheDir().toString() + "/yash";
        System.out.println("Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        //Assign the AIML files to bot for processing
        bot = new Bot("jarvis", MagicStrings.root_path, "chat");
        chat = new Chat(bot);
        //String args = null;
        //String temp = mainFunction(args);

    }

    @Override
    public void onStart(){
        super.onStart();

        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage();
            }
        });

        listView.setAdapter(chatArrayAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                FragmentManager delmanager = getFragmentManager();
                DeleteMessage delDialogFragment = new DeleteMessage();
                delDialogFragment.show(delmanager, "theDialog");
                pos=position;
                return false;
            }
        });
    }

    public void onStop(Bundle saveinstance){
        super.onStop();
        super.onSaveInstanceState(saveinstance);
        //outState.putParcelableArrayList("State",chatArrayAdapter.getList());
        ArrayList<ChatMessage> messages = chatArrayAdapter.getList();
        saveinstance.putParcelableArrayList("key", messages);
    }

    public void delMessageBool(){
        chatArrayAdapter.removeThis(pos);
        chatArrayAdapter.notifyDataSetChanged();
        listView.setAdapter(chatArrayAdapter);
    }

    //initialise menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main1, menu);
        return true;
    }

    //Assigning actions to menu item selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear:
                //Clear data
                FragmentManager clearmanager = getFragmentManager();
                ClearDialogFragment clearDialogFragment = new ClearDialogFragment();
                clearDialogFragment.show(clearmanager, "theDialog");
                return true;
            /*case R.id.settings:
                //open settings activity
                return true;*/
            case R.id.help:
                //open help activity
                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.about:
                //open help activity
                Intent aboutintent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(aboutintent);
                return true;
            case R.id.exit:
                FragmentManager exitmanager = getFragmentManager();
                ExitDialogFragment exitDialogFragment = new ExitDialogFragment();
                exitDialogFragment.show(exitmanager, "theDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    //copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
    //Request and response of user and the bot
    public static String mainFunction (String args) {
        MagicBooleans.trace_mode = false;
        System.out.println("trace mode = " + MagicBooleans.trace_mode);
        Graphmaster.enableShortCuts = true;
        String request = args;
        String response = chat.multisentenceRespond(request);

        return response;
    }

    public boolean sendChatMessage() {
        chatArrayAdapter.add(new ChatMessage(side, chatText.getText().toString()));

        // READ THE CHATTEXT HERE
        String comp = chatText.getText().toString();
        String arr[];
        if(comp.toUpperCase().startsWith("CALL")){
            // MAKE CALL
            arr = comp.split(" ",2);
            chatText.setText("");
            side = !side;
            chatArrayAdapter.add(new ChatMessage(side,"Trying to call "+arr[1]));
            side = !side;
            //makeCall(arr[1]);

        }
        else if(comp.toUpperCase().startsWith("OPEN")){
            // OPEN THE APP
            arr=comp.split(" ",2);
            //comp = getAppName(arr[1]);
            chatText.setText("");
            side = !side;
            chatArrayAdapter.add(new ChatMessage(side,"Sure."));
            side = !side;
            //launchApp(comp);
        }
        else if (comp.toLowerCase().contains("trending jobs"))
        {
            chatText.setText("");
            try {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                String term = comp;
                intent.putExtra(SearchManager.QUERY, term);
                startActivity(intent);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        else {
            //bot reply
            chatText.setText("");
            String temp = mainFunction(comp);
            String FILE_NAME = "example.txt";
            File file = new File(FILE_NAME);
            FileOutputStream fos = null;
            //EditText mEditText=temp;
            try {
                fos = openFileOutput(FILE_NAME, MODE_APPEND);
                String n="\n";
                String q="Question";
                String a="Answer";
                //fos.write(n.getBytes());
                fos.write(q.getBytes());
                fos.write(n.getBytes());
                fos.write(comp.getBytes());
                fos.write(n.getBytes());
                fos.write(a.getBytes());
                fos.write(n.getBytes());
                fos.write(temp.getBytes());
                fos.write(n.getBytes());
                Toast.makeText(this, "Saved to " + getFilesDir() + "/" + FILE_NAME,
                        Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            side = !side;
            chatArrayAdapter.add(new ChatMessage(side, temp));
            side = !side;
        }
        return true;
    }



    public void clearchat()
    {
        chatArrayAdapter.clearData();
        chatArrayAdapter.notifyDataSetChanged();
        listView.setAdapter(chatArrayAdapter);
    }

    /*
    @Override
    protected void onRestoreInstanceState(Bundle state){
        super.onRestoreInstanceState(state);
        ArrayList<ChatMessage> msgs = state.getParcelableArrayList("key");
        chatArrayAdapter.addAll(msgs);
        chatArrayAdapter.notifyDataSetChanged();
        listView.setAdapter(chatArrayAdapter);
        Toast.makeText(this,"Retrieved data",Toast.LENGTH_LONG).show();
    }
    */
    /*

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        //outState.putParcelableArrayList("State",chatArrayAdapter.getList());
        ArrayList<ChatMessage> messages = chatArrayAdapter.getList();
        outState.putParcelableArrayList("key", messages);
        Toast.makeText(this,"DATA set",Toast.LENGTH_LONG).show();
    }
    */

}
