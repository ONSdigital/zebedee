package com.github.onsdigital.zebedee.content.base;

import com.github.onsdigital.zebedee.content.util.ContentUtil;

/**
 * Created by bren on 03/08/15.
 */
public class Content implements Cloneable {

    @Override
    public Object clone() throws CloneNotSupportedException {
        return ContentUtil.clone(this);
    }
}
