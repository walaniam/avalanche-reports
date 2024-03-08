# IaC Comparative way
## Azure CLI
### App Service example
#### Create
```shell
export RESOURCE_GROUP=web-site
export AZURE_REGION=westeurope
export AZURE_APP_PLAN=popupappplan-$RANDOM
export AZURE_WEB_APP=popupwebapp-$RANDOM

az group list --output table --query "[?name == '$RESOURCE_GROUP']"
az group create --location $AZURE_REGION --name $RESOURCE_GROUP

az appservice plan create --name $AZURE_APP_PLAN --resource-group $RESOURCE_GROUP --location $AZURE_REGION --sku FREE
az appservice plan list --query "[?name == '$AZURE_APP_PLAN']"

az webapp create --name $AZURE_WEB_APP --resource-group $RESOURCE_GROUP --plan $AZURE_APP_PLAN
az webapp list --output table

az webapp deployment source config --name $AZURE_WEB_APP --resource-group $RESOURCE_GROUP --repo-url "https://github.com/Azure-Samples/php-docs-hello-world" --branch master --manual-integration
```

#### Destroy
```shell
az group delete -g $RESOURCE_GROUP
```