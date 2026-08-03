package com.acme.armada.log;

public interface Bookkeeping {

    void write(String line);

    String read(int position);
}
