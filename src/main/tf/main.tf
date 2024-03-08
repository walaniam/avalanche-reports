resource "azurerm_resource_group" "this" {
  name     = var.resource_group_name
  location = var.azure_default_location
}

resource "azurerm_cosmosdb_account" "mongo" {
  location             = azurerm_resource_group.this.location
  name                 = "${var.project_name}-${var.project_unique_id}"
  resource_group_name  = azurerm_resource_group.this.name
  offer_type           = "Standard"
  kind                 = "MongoDB"
  mongo_server_version = "4.2"

  enable_automatic_failover     = true
  public_network_access_enabled = true

  backup {
    type                = "Periodic"
    interval_in_minutes = 720
    retention_in_hours  = 24
    storage_redundancy  = "Zone"
  }

  capabilities {
    name = "EnableMongo"
  }
  capabilities {
    name = "EnableServerless"
  }

  consistency_policy {
    consistency_level = "Eventual"
  }

  geo_location {
    failover_priority = 0
    location          = var.azure_default_location
  }

  lifecycle {
    ignore_changes = [
      name
    ]
  }
}

# App Service

resource "azurerm_storage_account" "function_app_storage" {
  account_replication_type = "LRS"
  account_tier             = "Standard"
  location                 = azurerm_resource_group.this.location
  name                     = "${var.function_storage_account}${var.project_unique_id}"
  resource_group_name      = azurerm_resource_group.this.name
}

resource "azurerm_service_plan" "this" {
  location            = azurerm_resource_group.this.location
  name                = "${var.project_name}-service-plan"
  os_type             = "Windows"
  resource_group_name = azurerm_resource_group.this.name
  sku_name            = "Y1"
}

resource "azurerm_log_analytics_workspace" "this" {
  name                 = "${var.project_name}-log-workspace"
  location             = azurerm_resource_group.this.location
  resource_group_name  = azurerm_resource_group.this.name
  sku                  = "PerGB2018"
  retention_in_days    = 30
  cmk_for_query_forced = false
  lifecycle {
    ignore_changes = [
      tags
    ]
  }
}

resource "azurerm_application_insights" "this" {
  application_type    = "java"
  location            = azurerm_resource_group.this.location
  name                = "${var.project_name}-app-insights"
  resource_group_name = azurerm_resource_group.this.name
  workspace_id        = azurerm_log_analytics_workspace.this.id
}

resource "azurerm_windows_function_app" "this" {
  location                   = azurerm_service_plan.this.location
  name                       = "${var.project_name}-${var.project_unique_id}-func-app"
  resource_group_name        = azurerm_service_plan.this.resource_group_name
  service_plan_id            = azurerm_service_plan.this.id
  storage_account_name       = azurerm_storage_account.function_app_storage.name
  storage_account_access_key = azurerm_storage_account.function_app_storage.primary_access_key
  site_config {
    app_scale_limit          = 5
    application_insights_key = azurerm_application_insights.this.instrumentation_key
    application_stack {
      java_version = "17"
    }
  }
  app_settings = {
    "CosmosDBConnectionString" = azurerm_cosmosdb_account.mongo.connection_strings[0]
    "WEBSITE_RUN_FROM_PACKAGE" = "1"
  }
  lifecycle {
    ignore_changes = [
      tags
    ]
  }
}

# Cost alerts
resource "azurerm_consumption_budget_resource_group" "rg_budget" {
  name              = "${var.project_name}-budget"
  resource_group_id = azurerm_resource_group.this.id
  amount            = 20
  time_grain        = "Monthly"
  time_period {
    start_date = formatdate("YYYY-MM-01'T'hh:mm:ssZ", timestamp())
  }
  notification {
    threshold      = 80
    operator       = "GreaterThanOrEqualTo"
    contact_emails = [var.notification_alert_email]
  }
  lifecycle {
    ignore_changes = [
      time_period
    ]
  }
}

output "function_app_hostname" {
  value     = azurerm_windows_function_app.this.default_hostname
  sensitive = false
}