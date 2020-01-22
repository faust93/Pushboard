package com.faust93.pushboard;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by faust93 on 21.04.2014.
 */
public class MenuDrawerAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<MenuDrawerItem> menuDrawerItems;

    public MenuDrawerAdapter(Context context, ArrayList<MenuDrawerItem> Items){

       // super(context, R.layout.menu_item, Items);
        this.context = context;
        this.menuDrawerItems = Items;
    }

    public ArrayList<MenuDrawerItem> getMenuDrawerItems(){
        return this.menuDrawerItems;
    }

    public void addMenuItem(String title, int icon) {
        this.menuDrawerItems.add( new MenuDrawerItem(title, icon) );
    }

    public void insertMenuItem(String title, int icon, boolean isCounterVisible, int count) {
        this.menuDrawerItems.add( 2, new MenuDrawerItem(title, icon, isCounterVisible, count) );

    }

    public void removeMenuItem(MenuDrawerItem item) {
        this.menuDrawerItems.remove(item);
    }

    public void addMenuItem(String title, int icon, boolean isCounterVisible, int count) {
        this.menuDrawerItems.add( new MenuDrawerItem(title, icon, isCounterVisible, count) );
    }

    public void addMenuItem(String title) {
        this.menuDrawerItems.add( new MenuDrawerItem(title) );
    }

    public MenuDrawerItem findById(long id){
        for(MenuDrawerItem item : menuDrawerItems){
            if(item.getId() == id)
                return item;
        }
        return null;
    }

    public MenuDrawerItem findByTitle(String title){
        for(MenuDrawerItem item : menuDrawerItems){
        //    Log.d("pushboard", "findbysubject: " + title + " " + item.getTitle());
            if(item.getTitle().equals(title))
                return item;
        }
        return null;
    }

    @Override
    public int getCount() {
        return menuDrawerItems.size();
    }

    @Override
    public MenuDrawerItem getItem(int position) {
        return menuDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return menuDrawerItems.get(position).getId();
    }

    public String getItemTitle(int position) {
        return menuDrawerItems.get(position).getTitle();
    }

    public void resetAllCounters(){
        for(MenuDrawerItem item : menuDrawerItems){
            if(item.getCounterVisibility())
                item.setCount(0);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
      //      convertView = mInflater.inflate(R.layout.menu_item, null);

        View rowView = null;
        if(!menuDrawerItems.get(position).isGroupHeader()){
            rowView =  mInflater.inflate(R.layout.menu_item, parent, false);

            ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
            TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
            TextView counterView = (TextView) rowView.findViewById(R.id.item_counter);

            imgView.setImageResource(menuDrawerItems.get(position).getIcon());
            titleView.setText(menuDrawerItems.get(position).getTitle());

            if(menuDrawerItems.get(position).getCounterVisibility()) {
                counterView.setText(String.valueOf(menuDrawerItems.get(position).getCount()));
            } else {
                counterView.setVisibility(View.GONE);
            }

        } else{
            rowView = mInflater.inflate(R.layout.group_header, parent, false);
            TextView titleView = (TextView) rowView.findViewById(R.id.header);
            titleView.setText(menuDrawerItems.get(position).getTitle());
        }

        return rowView;
    }
}
