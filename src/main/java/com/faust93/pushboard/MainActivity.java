package com.faust93.pushboard;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.PopupMenu;

import android.widget.ListView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity implements Const, msgViewFragment.msgViewListener {

    private mNotifyReceiver mReceiver;
    boolean state = false;

    private ActionBarDrawerToggle mDrawerToggle;
    CharSequence mTitle;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;

    private ArrayList<MenuDrawerItem> drawerItems;
    private MenuDrawerAdapter adapter;

    // Base menu items ID's
    private long settings_id, about_id, allmsg_id;

    private DBhelper db;

    private static SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBhelper(getApplicationContext());
        preferences = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        mTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout,
                R.string.app_name,
                R.string.app_name) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);

            }
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mTitle);

            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerLayout.setDrawerShadow(R.drawable.shadow, GravityCompat.START);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(new MenuClickListener());
        mDrawerList.setOnItemLongClickListener(new MenuLongClickListener());

        drawerItems = new ArrayList<MenuDrawerItem>();
        drawerItems.add(new MenuDrawerItem("MESSAGES"));
        drawerItems.add(new MenuDrawerItem("All", R.drawable.ic_action_email, true, 0));
        drawerItems.add(new MenuDrawerItem("MISC"));
        drawerItems.add(new MenuDrawerItem("Settings",R.drawable.ic_action_settings ));
        drawerItems.add(new MenuDrawerItem("About", R.drawable.ic_action_about));

        allmsg_id = drawerItems.get(1).getId();
        settings_id = drawerItems.get(3).getId();
        about_id = drawerItems.get(4).getId();

        adapter = new MenuDrawerAdapter(getApplicationContext(), drawerItems);
        mDrawerList.setAdapter(adapter);

        if(!PushBoardService.mStarted && preferences.getBoolean("srvEnabled", false)){
            Log.d("pushboard","Service is not running, starting..");
            PushBoardService.actionStart(getApplicationContext());
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        initSubjects(adapter);

        //show all msgs
        int count = showCards(null);

        adapter.findById(allmsg_id).setCount(count);
        adapter.notifyDataSetChanged();

        mDrawerList.setItemChecked(1, true);
        mDrawerList.setSelection(1);

    }

    private int showCards(String subj){

        int count = 0;
        List<pbMessage> AllMsgs;

        msgViewFragment msgFragment = new msgViewFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, msgFragment).commit();
        getFragmentManager().executePendingTransactions();

        if(subj == null) {
            AllMsgs = db.getAllMessages();
        } else {
            AllMsgs = db.getAllMessagesWithSubj(subj);
        }

        for (pbMessage msg : AllMsgs) {
            msgFragment.insertMsg(getApplicationContext(), msg.getID(), msg.getType(), msg.getSubject(), msg.getMessage(), 0);
            count++;
        }
        return count;
    }

    // init subjects menus at app start
    private void initSubjects(MenuDrawerAdapter mAdapter){

        MenuDrawerItem item = null;

        List<pbMessage> allMsgs = db.getAllMessages();

        mAdapter.resetAllCounters();

        for(pbMessage msg : allMsgs){

            String subj = msg.getSubject();

            if(subj != null ) {
                item = mAdapter.findByTitle(subj);
                if ( item != null) {
                    item.incrCount();
                } else {
                    mAdapter.insertMenuItem(subj, R.drawable.ic_action_email, true, 1);
                }
            }
        }
    }

    // swipe card listener
    @Override
    public void deleteCard(int cardId, String cardHeader){

        MenuDrawerItem item = null;

        db.deleteMessage(cardId);

        // decrease All counter
        item = adapter.findById(allmsg_id);
        item.decrCount();

        // decrease message's subject counter & remove menu item if zero
        item = adapter.findByTitle(cardHeader);
        if ( item != null) {
            item.decrCount();

            if (item.getCount() == 0) {     //switch to All if no more msgs under subj
                adapter.removeMenuItem(item);
                mDrawerList.setItemChecked(1, true);
                mDrawerList.setSelection(1);
                showCards(null);
                setTitle("All");
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // long-click menu drawer listener
    private class MenuLongClickListener implements ListView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(final AdapterView<?> adapterView, View view, int position, long id) {

            if (!drawerItems.get(position).isGroupHeader() && id != settings_id && id != about_id ) {

                final MenuDrawerItem mItem = adapter.getItem(position);
                final String title = mItem.getTitle();

                PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_delete:
                            if(title.equals("All")){
                                db.deleteAllMessages();
                                adapter.resetAllCounters();
                                Iterator<MenuDrawerItem> itr = adapter.getMenuDrawerItems().iterator();
                                while(itr.hasNext()) {
                                    MenuDrawerItem _item = itr.next();
                                    if (_item.getIconId() == R.drawable.ic_action_email && !_item.getTitle().equals("All"))
                                        itr.remove();
                                }
                            } else {
                                // delete messages with subj & decrease All items count
                                int affectedItems = db.deleteMessagesWithSubj(title);
                                if (affectedItems != 0) {
                                    MenuDrawerItem all = adapter.findById(allmsg_id);
                                    int curCount = all.getCount();
                                    all.setCount(curCount - affectedItems);
                                }
                                adapter.removeMenuItem(mItem);
                            }
                            mDrawerList.setItemChecked(1, true);
                            mDrawerList.setSelection(1);
                            showCards(null);
                            setTitle("All");
                            return true;
                        default:
                            return false;
                    }
                }
            });
            popup.show();
            }
            return false;
        }
    }

    private class MenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            String title = null;

            if(id == settings_id) {
                title = "Settings";
                Fragment fragment = new SettingsFragment();
                getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
                getFragmentManager().executePendingTransactions();

             } else if(id == about_id) {
                title = "About";
                //fragment = new aboutFragment();
            } else if(id == allmsg_id){
                title = adapter.getItemTitle(position);
                showCards(null);
            } else  if(!drawerItems.get(position).isGroupHeader()){
                title = adapter.getItemTitle(position);
                showCards(title);
            }
            if(title != null){
                setTitle(title);
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                mDrawerLayout.closeDrawer(mDrawerList);
            }

        }
    }

    public class mNotifyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NOTIFY_ACTION)) {

                String selectedTitle = adapter.getItemTitle(mDrawerList.getCheckedItemPosition());
                String subj = intent.getStringExtra("subj");

                if(subj.equals(selectedTitle))
                    showCards(selectedTitle);
                else if(selectedTitle.equals("All"))
                    showCards(null);

                MenuDrawerItem item = adapter.findByTitle(subj);
                if ( item != null) {
                    item.incrCount();
                } else {
                    adapter.insertMenuItem(subj, R.drawable.ic_action_email, true, 1);
                }

                // increase All counter
                item = adapter.findById(allmsg_id);
                item.incrCount();

                adapter.notifyDataSetChanged();
            }
        abortBroadcast();
        }
    }

    @Override
    protected void onResume() {
        IntentFilter mNotifyFilter;
        mNotifyFilter = new IntentFilter(NOTIFY_ACTION);
        mNotifyFilter.setPriority(2);
        mReceiver = new mNotifyReceiver();
        registerReceiver(mReceiver, mNotifyFilter);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static public SharedPreferences getPreferences(){
        return preferences;
    }
}
