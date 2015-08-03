package com.github.onsdigital.zebedee.content.adhoc;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.staticpage.base.BaseStaticPage;

/**
 * Created by bren on 04/06/15.
 */
public class AdHoc extends BaseStaticPage {

    @Override
    public ContentType getType() {
        return ContentType.static_adhoc;
    }

}
