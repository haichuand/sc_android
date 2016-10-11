package com.mono.parser;

import com.mono.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anu on 10/11/2016.
 */

public class GlobalEventList {
        private static GlobalEventList instance;
        public List<Event> monthList;

        private GlobalEventList() {
            monthList = new ArrayList<Event>();
        }

        public static GlobalEventList getInstance() {
            if (instance == null) instance = new GlobalEventList();
            return instance;
        }

}
