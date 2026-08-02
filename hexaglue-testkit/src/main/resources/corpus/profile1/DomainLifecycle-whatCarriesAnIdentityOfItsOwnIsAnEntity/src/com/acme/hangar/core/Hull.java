package com.acme.hangar.core;

public class Hull {

    private final HullTag tag;
    private final Manifest manifest;

    public Hull(HullTag tag, Manifest manifest) {
        this.tag = tag;
        this.manifest = manifest;
    }

    public HullTag tag() {
        return tag;
    }

    public Manifest manifest() {
        return manifest;
    }
}
