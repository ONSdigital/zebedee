# Zebedee

Simple CMS back-end: a file-based CMS with Json API and no UI.

It provides the ability to create and approve collections of change and preview these, overlaid onto existing content.

Pre-release content is encrypted until published and only shared with permitted users

#### Example environment variables
- zebedee_root    /Users/{username}/Documents/onswebsite
- brian_url   http://localhost:8083/
- use_beta_publisher   false
- DATASET_API_URL http://localhost:22000

Creating a directory and setting it as `zebedee_root` will allow content to persist between launches of zebedee, otherwise a new directory will be created every time. 

#### Service authentication with Zebedee

1) Login to florence using: `curl -X POST -d '{"email":"florence@magicroundabout.ons.gov.uk","password":"<your password>"}' http://localhost:8082/login`
2) Make a note of the `access_token` that gets returned in the headers
3) Create an admin service key: `curl -X POST http://localhost:8082/service -H "X-Florence-Token: <access_token>" -d '{"id":"admin"}'`
4) Make a note of the service token that gets returned in the response body
5) Set the environment variable:
`export SERVICE_AUTH_TOKEN=<YOUR_SERVICE_TOKEN>` replacing the token with that one you got in step 4
6) Restart zebedee and authenticating services
