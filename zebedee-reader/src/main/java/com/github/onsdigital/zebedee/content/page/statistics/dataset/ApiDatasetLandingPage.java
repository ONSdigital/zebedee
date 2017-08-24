package com.github.onsdigital.zebedee.content.page.statistics.dataset;

 import com.github.onsdigital.zebedee.content.page.base.PageType;
 import com.github.onsdigital.zebedee.content.page.statistics.base.Statistics;
 import com.github.onsdigital.zebedee.content.partial.Link;
 import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

 import java.util.List;

 /**
  * Created by
  */
 public class ApiDatasetLandingPage extends Statistics {

     /*Body*/
     private String apiDatasetId;

     @Override
     public PageType getType() {
         return PageType.api_dataset_landing_page;
     }

     public String getapiDatasetId() {
         return apiDatasetId;
     }

     public void setapiDatasetId(String apiDatasetId) {
         this.apiDatasetId = apiDatasetId;
     }

 }
