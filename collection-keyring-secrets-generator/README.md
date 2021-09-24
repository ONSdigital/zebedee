## collection-keyring-secrets-generator
The central keyring uses SecretKey encryption to securely store collection keys as files on disk. The Zebedee 
application configuration is now required to provide:
- `SecretKey` 
- `IVParameterSpec`

formatted as Base64 encoded strings. `collection-keyring-secrets-generator` is a simple tool to generate these config 
values in the required format. This tool can be used to generate secrets for developer local setup and secrets 
for dev/prod environments.

### Generating secrets
From the root of the Zebedee project:
```bash
cd collection-keyring-secrets-generator
```
For local dev set up:
```bash
make local
```
Add the output to your `zebedee/run.sh` file and restart Zebedee.

If you are generating config develop/production environments: 
```bash
make env
```
Add the output to the application secrets JSON config (following existing process for app secrets).