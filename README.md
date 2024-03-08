# avalanche-reports-az-func

## Archetype used to generate this project
```bash
mvn archetype:generate -DarchetypeGroupId=com.microsoft.azure -DarchetypeArtifactId=azure-functions-archetype -DjavaVersion=11
```

## Development environment setup

### Azure CLI
How to install Azure CLI https://learn.microsoft.com/en-us/cli/azure/install-azure-cli

### Terraform
#### About Terraform
https://developer.hashicorp.com/terraform/tutorials/azure-get-started
#### Install CLI
https://developer.hashicorp.com/terraform/tutorials/azure-get-started/install-cli  

## Provision Azure environment
### Create tfvars file
Create file with `.auto.tfvars` extension in `src/main/tf` directory, example `src/main/tf/myenv.auto.tfvars`  
Set variable in this file like in this example below
```terraform
azure_subscription_id    = "your azure subscription id"
azure_tenant_id          = "your azure tenant id"
resource_group_name      = "resource group in which resources will be created"
project_name             = "name used for resource that need to have unique name"
function_storage_account = "name used for function app storage account that need to have unique name"
project_unique_id        = 123456 # additional identifier for uniqueness
notification_alert_email = "your email"
```
### Init Terraform
```bash
cd src/main/tf
terraform init
```

### Create environment
```bash
cd src/main/tf
terraform plan
terraform apply
```

## Work with Environment
### Export shell variables
```bash
export FUNC_APP_PLAN=$(cd src/main/tf && terraform state show azurerm_service_plan.this |grep name |grep -v resource_group_name |grep -v sku_name |cut -d "=" -f2 |xargs |tr -d '[:space:]')
export FUNC_APP=$(cd src/main/tf && terraform state show azurerm_windows_function_app.this |grep name |grep -v hostname |grep -v resource_group_name |grep -v storage_account_name |head -n1 |cut -d "=" -f2 |xargs |tr -d '[:space:]')
export RG_NAME=$(cat src/main/tf/myenv.auto.tfvars |grep resource_group_name |cut -d "=" -f2 |xargs |tr -d '[:space:]')
```

## Build functions and run locally
### Install Azure Functions Core Tools
[https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=v4%2Cmacos%2Cjava%2Cportal%2Cbash)
### Build and run
```bash
mvn clean package
```
Run locally  
```bash
mvn azure-functions:run
```
Generate samples  
```bash
 ./generate_samples.sh http://localhost:7071/api/post-observations-v1 "ignore"
```

## Deploy to Azure
```bash
az login
```
```bash
mvn clean package azure-functions:deploy -Dapp.name=$FUNC_APP -Dapp.plan.name=$FUNC_APP_PLAN -Dapp.resource.group=$RG_NAME
```

## Azure CLI
### Manage function app keys
#### List keys
```bash
az functionapp keys list --resource-group ${RG_NAME} --name ${FUNC_APP}
```
#### Set key
```bash
az functionapp keys set -g ${RG_NAME} -n ${FUNC_APP} --key-type functionKeys --key-name MyKeyName --key-value MyKeyValue
```
### Manage function keys
```bash
az functionapp function keys list -g ${RG_NAME} -n ${FUNC_APP} --function-name get-latest-observations-v1
```
```bash
az functionapp function keys list -g ${RG_NAME} -n ${FUNC_APP} --function-name post-observations-v1
```

### Azure function management
See plan  
```bash
az appservice plan show -n $FUNC_APP_PLAN -g $RG_NAME
```

See function  
```bash
az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name get-latest-observations-v1
```
List function endpoints  
```bash
az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name get-latest-observations-v1 |jq -r '.invokeUrlTemplate'
az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name get-latest-observation-v1 |jq -r '.invokeUrlTemplate'
az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name get-extremes-v1 |jq -r '.invokeUrlTemplate'
```
```bash
az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name post-observations-v1
```

See deployment logs  
```bash
az functionapp log deployment list -n $FUNC_APP -g $RG_NAME
```

### Check function logs
```bash
func azure functionapp logstream ${FUNC_APP}
```

### Metrics
```bash
funcAppId=$(az resource show -g $RG_NAME -n $FUNC_APP --resource-type 'Microsoft.Web/sites' |jq -r '.id')
```
Sample metrics  
```bash
az monitor metrics list --resource $funcAppId --metrics "MemoryWorkingSet"
az monitor metrics list --resource $funcAppId --metrics "Requests"
az monitor metrics list --resource $funcAppId --metrics "Http2xx"
```

### Other useful commands
#### Query resource groups
```bash
az group list --query "[?name == '$RG_NAME']"
```

#### Delete resource group together with resources
```bash
az group delete -g $RG_NAME
```

## Run samples
Find API key and POST function endpoint
```bash
apiKey=$(az functionapp keys list --resource-group ${RG_NAME} --name ${FUNC_APP} |jq -r '.functionKeys.default')
postUrl=$(az functionapp function show -g ${RG_NAME} -n ${FUNC_APP} --function-name post-observations-v1 |jq -r '.invokeUrlTemplate')
```
Run generator
```bash
./generate_samples.sh "$postUrl" "$apiKey"
```

## Maven project
### Azure Maven Plugin
https://github.com/microsoft/azure-maven-plugins/wiki/Azure-Functions
### Maven Properties
```bash
mvn help:evaluate
```
