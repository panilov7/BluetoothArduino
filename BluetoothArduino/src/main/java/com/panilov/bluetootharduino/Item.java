package com.panilov.bluetootharduino;

/**
 * Created by peter on 11/3/13.
 */
public class Item {
    private String title, description;

    public Item(String title, String desc){
        this.title = title;
        description = desc;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
