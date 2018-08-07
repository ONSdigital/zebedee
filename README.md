# Zebedee

Simple CMS back-end: a file-based CMS with Json API and no UI.

It provides the ability to create and approve collections of change and preview these, overlaid onto existing content.

Pre-release content is encrypted until published and only shared with permitted users


David

### Enable store delete content
To enable storing / retirval of deleted content you need to have the audit dbAdd the following to the `run.sh`:

```bash
export audit_db_enabled=true
export store_deleted_content=true
...
java $JAVA_OPTS \

 ...
 -Daudit_db_enabled=$audit_db_enabled \
 -Dstore_deleted_content=$store_deleted_content \
 ...

 com.github.davidcarboni.restolino.Main
```






#### Example environment variables
zebedee_root    /Users/thomasridd/Documents/onswebsite
brian_url   http://localhost:8083/
use_beta_publisher   false
