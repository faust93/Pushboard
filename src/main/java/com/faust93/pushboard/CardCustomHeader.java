package com.faust93.pushboard;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.CardHeader;

/**
 * Created by faust93 on 29.04.2014.
 */
public class CardCustomHeader extends CardHeader {

    private String title;

    public CardCustomHeader(Context context, int layout) {
        super(context, layout);
    }

    @Override
    public void setTitle(String title) {
       this.title = title;
    }

    @Override
    public String getTitle()
    {
        return this.title;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {

        if (view != null) {
            TextView t1 = (TextView) view.findViewById(R.id.card_title);
            if (t1 != null)
                t1.setText(title);
        }
    }
}
