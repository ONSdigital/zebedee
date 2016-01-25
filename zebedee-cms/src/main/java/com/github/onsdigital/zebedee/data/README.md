# DataPublisher Reloaded
Componentised version of the original DataPublisher mega-class

#### Purpose
Single function .processCollection() processes all timeseries_dataset upload files in a collection


### Processing
#### Timeseries
The new publisher is hierarchical

DataPublisher <- collection level
DataPublication <- single dataset level
DataProcessor <- single timeseries level

Supporting objects are
DataPublicationDetails <- stores the triplet of files needed for publication - landing page, dataset page, and upload file
DataFinder <- searches the collection for files that need processing
DataLink <- connects data processor to external services
DataMerge <- merges two sets of timeseries values
DataWriter <- writes out single timeseries taking care of versioning if necessary

#### DataFiles

DataGenerator <- converts a list of timeseries to xlsx and csv files

Supported by
DataGrid <- converts a list of timeseries to a table format
DataTimeRange <- extracts the time range required to represent diverse timeseries in a datagrid


Tom

