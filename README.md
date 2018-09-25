# Zebedee

Zebedee is the CMS used by the ONS website and the internal website publishing system. It is a JSON API and does not 
have a user interface. It comes in 2 flavours:

## zebedee-reader
Zebedee-reader is read-only. It's used by [Babbage][1] (the public facing web frontend of the ONS website) to retrived 
the published site content as JSON.

## zebedee-cms
Zebedee-cms is an extension of zebedee-reader. It's used by [Florence][2] and provides API endpoints for managing 
content, users, teams and publishing collecions. Zebedee-CMS is not public facing and requires authentication for the 
majority functionality. Pre-release content is encrypted and requries the appropriate permissions to be able to 
access it.

## Prerequisites 
- git
- Java 8
- Maven

Zebedee is an API and does not have a UI. The easiest and quickest way to use it is is via Florence. Clone and set up:
- [Florence][2] - the UI for the publishing application.
- [Babbage][1] - ONS website the fontend.
- [Sixteens][5] 

### Getting started

```
git clone git@github.com:ONSdigital/zebedee.git
```

_NOTE_: The following set guide will set up Zebedee in **"CMS"** mode as this is typically how the devlopers will run 
the stack locally. 

### Database... 
Zebedee isn't backed by a database instead it uses a file system to store json files on disk ***. As a result it 
requires a specific directory structure in order to function correctly.

To save yourself some pain you can use the [dp-zebedee-utils/content][3] tool to create the required directory 
structure and populate the CMS with some "default content" - follow the steps in the [README][3] before going any further.

*** _We know this is a terrible idea - but in our defence this is a legacy hangover and we are actively working 
towards deprecating it._
 
Once the script has run successfully copy the generated script from `dp-zebedee-utils/content/generated/run-cms.sh` into
the root dir of your Zebedee project. This bash script will run Zebedee using typical dev defaults configuration and 
use the content generated by dp-zebedee-utils.



### _Awakening the beast_

You may be required to make the bash script an executable before you can run it. If so run:

````bash
sudo chmod +x run-cms.sh
````  
_Enter you password when prompted_.

Otherwise run: 
 
```
./run-cms.sh
```

Assuming its started without error head to [Florence login][4] and login with the default account:
```
email: florence@magicroundabout.ons.gov.uk
password: Doug4l
```

[1]: https://github.com/ONSdigital/babbage
[2]: https://github.com/ONSdigital/florence
[3]: https://github.com/ONSdigital/dp-zebedee-utils/tree/master/content
[4]: http://localhost:8081/florence/login
[5]: https://github.com/ONSdigital/sixteens