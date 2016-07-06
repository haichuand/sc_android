package com.mono.details;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.mono.R;
import com.mono.model.Event;

/**
 * This class is used to handle the notes section located in Event Details.
 *
 * @author Gary Ng
 */
public class NotePanel {

    private EventDetailsActivity activity;
    private EditText notes;
    private TextWatcher textWatcher;

    private Event event;

    public NotePanel(EventDetailsActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        notes = (EditText) activity.findViewById(R.id.notes);
        notes.addTextChangedListener(textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString().trim();
                event.description = !value.isEmpty() ? value : null;
            }
        });
    }

    /**
     * Initialize this panel using the given event.
     *
     * @param event The instance of the event.
     */
    public void setEvent(Event event) {
        this.event = event;
        setText(event.description, true);
    }

    /**
     * Set the string value of the input box.
     *
     * @param text The value of the text.
     * @param disable The value to disable text change listener.
     */
    public void setText(CharSequence text, boolean disable) {
        if (disable) {
            notes.removeTextChangedListener(textWatcher);
        }

        notes.setText(text);

        if (disable) {
            notes.addTextChangedListener(textWatcher);
        }
    }
}
