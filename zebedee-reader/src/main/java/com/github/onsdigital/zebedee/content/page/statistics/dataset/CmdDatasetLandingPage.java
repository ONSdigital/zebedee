package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.base.Statistics;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.List;

/**
 * Created by
 */
public class CmdDatasetLandingPage extends Statistics {

    /*Body*/
    private String cmdDatasetId;

    @Override
    public PageType getType() {
        return PageType.cmd_dataset_landing_page;
    }

    public String getcmdDatasetId() {
        return cmdDatasetId;
    }

    public void setcmdDatasetId(String cmdDatasetId) {
        this.cmdDatasetId = cmdDatasetId;
    }

}
