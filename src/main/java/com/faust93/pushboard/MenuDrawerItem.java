package com.faust93.pushboard;

/**
 * Created by faust93 on 21.04.2014.
 */
public class MenuDrawerItem {

    private long _id;
    private String title;
    private int icon;
    private int count;
    private boolean isCounterVisible = false;
    private boolean isGroupHeader = false;

    public MenuDrawerItem(){}

    public MenuDrawerItem(String title) {
        this(title, 0, false, 0);
        isGroupHeader = true;
    }

    public MenuDrawerItem(String title, int icon){
        this(title, icon, false, 0);
    }

    public MenuDrawerItem(String title, int icon, boolean isCounterVisible, int count){
        this.title = title;
        this.icon = icon;
        this.isCounterVisible = isCounterVisible;
        this.count = count;
        this._id = Utils.createID();
    }

    public long getId() { return this._id; }

    public long getIconId() { return this.icon; }

    public String getTitle(){
        return this.title;
    }

    public int getIcon(){
        return this.icon;
    }

    public int getCount(){
        return this.count;
    }

    public void incrCount(){
        this.count++;
    }

    public void decrCount(){
        this.count--;
    }

    public boolean getCounterVisibility(){
        return this.isCounterVisible;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIcon(int icon){
        this.icon = icon;
    }

    public void setCount(int count){
        this.count = count;
    }

    public void setCounterVisibility(boolean isCounterVisible){
        this.isCounterVisible = isCounterVisible;
    }

    public boolean isGroupHeader() {
        return isGroupHeader;
    }
    public void setGroupHeader(boolean isGroupHeader) {
        this.isGroupHeader = isGroupHeader;
    }


}
