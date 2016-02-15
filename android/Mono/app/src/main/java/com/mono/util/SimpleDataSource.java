package com.mono.util;

public interface SimpleDataSource<T> {

    T getItem(int position);

    int getCount();
}
