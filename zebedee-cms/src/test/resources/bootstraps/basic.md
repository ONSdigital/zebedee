# Basic artificial setup

home      --themea    --landinga  --producta  --articles   --articlea    2015-01-01 
          |           |           |           |-bulletins  --bulletina   2015-01-01 
          |           |           |           |-timeseries --a4fk 
          |           |           |                        |-a4vr 
          |           |           |-productb 
          |           | 
          |           |-landingb  --productc                       
          |                       |           
          |                       |-productd 
          | 
          |-themeb    --landingc  --producte 
                      |           | 
                      |           --productf 
                      | 
                      |-productg 
                      
### Examples

###### json content
/themea/data.json
/themeb/landingc.json

###### Article content
/themea/landinga/producta/articles/articlesa/2015-01-01/data.json
/themea/landinga/producta/articles/articlesa/2015-01-01/8cde8012.png

###### Bulletin content
/themea/landinga/producta/bulletins/bulletina/2015-01-01/data.json
/themea/landinga/producta/bulletins/bulletina/2015-01-01/6f126267.xls

###### Time series content
/themea/landinga/producta/timeseries/a4fk/data.json