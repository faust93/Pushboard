package com.faust93.pushboard;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardView;

/**
 * Created by faust93 on 23.04.2014.
 */
public class msgViewFragment extends Fragment implements Const {

    private msgViewListener listener;

    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if(rootView == null)
        rootView = inflater.inflate(R.layout.messages_fragment, container, false);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof msgViewFragment.msgViewListener) {
            listener = (msgViewListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet msgViewListener");
        }
    }

    public void insertMsg(Context context, long id, int type, String subj, String msg, int index) {

        LinearLayout parent = (LinearLayout)rootView.findViewById(R.id.ui_cards_view);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout cRow = (LinearLayout) inflater.inflate(R.layout.card_row, parent, false);


        Card card = new Card(context);

        int layout = R.layout.ok_card_header1;

        switch (type) {
            case MSG_TYPE_WARN:
                layout = R.layout.warn_card_header1;
                break;
            case MSG_TYPE_CRITICAL:
                layout = R.layout.critical_card_header1;
                break;
        }

        CardHeader header = new CardCustomHeader(context, layout);
        header.setTitle(subj);
        card.addCardHeader(header);
        card.setId(Long.toString(id));
        card.setTitle(msg);
        card.setSwipeable(true);


        card.setOnSwipeListener(new Card.OnSwipeListener() {
            @Override
            public void onSwipe(Card card) {
                listener.deleteCard(Integer.valueOf(card.getId()),card.getCardHeader().getTitle());
            }
        });

        CardView cardView = (CardView) cRow.findViewById(R.id.card_msg);
        cardView.setCard(card);
        parent.addView(cRow,0);

    }

    public interface msgViewListener {
        public void deleteCard(int cardId, String cardHeader);
    }

}
