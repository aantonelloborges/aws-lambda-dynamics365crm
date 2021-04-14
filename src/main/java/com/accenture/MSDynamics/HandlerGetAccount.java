package com.accenture.msdynamics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.accenture.MSDynamics.entities.AuthorityEntity;
import com.accenture.MSDynamics.entities.GetAccountResponseEntity;
import com.accenture.MSDynamics.entities.GetAccountRequestEntity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HandlerGetAccount implements RequestHandler<Map<String, String>, String> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  LambdaLogger logger = null;

  @Override
  public String handleRequest(Map<String, String> event, Context context) {
    logger = context.getLogger();

    logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
    logger.log("CONTEXT: " + gson.toJson(context));
    logger.log("EVENT: " + gson.toJson(event));
    logger.log("EVENT TYPE: " + event.getClass());

    String DYNAMICS_ORG_URI = System.getenv().get("DYNAMICS_ORG_URI");
    String AZURE_TENANT_ID = System.getenv().get("AZURE_TENANT_ID");
    String CLIENT_ID = System.getenv().get("CLIENT_ID");
    String CLIENT_SECRET = System.getenv().get("CLIENT_SECRET");

    GetAccountRequestEntity request = gson.fromJson(gson.toJson(event), GetAccountRequestEntity.class);

    logger.log("REQUEST.phone: " + request.phone);

    try {

      AuthorityEntity accessToken = authenticate(DYNAMICS_ORG_URI, AZURE_TENANT_ID, CLIENT_ID, CLIENT_SECRET, context);
     
      GetAccountResponseEntity account_data = getAccountData(DYNAMICS_ORG_URI, accessToken.access_token, request.phone, context);
      String response = gson.toJson(account_data);

      logger.log("handleRequest.response: " + response);

      return response;
            
    } catch (Exception ex) {
      return ex.getMessage();
    }
  }

  public AuthorityEntity authenticate(String organizationURI, String tenantId, String clientId, String clientSecret,
      Context context) throws Exception {

    logger = context.getLogger();

    HttpURLConnection connection = null;
    try {
      URL url = new URL("https://login.microsoft.com/" + tenantId + "/oauth2/token");
      connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);

      String body = "grant_type=client_credentials" + "&client_id=" + clientId + "&client_secret=" + clientSecret
          + "&resource=" + organizationURI;
      OutputStream outputStream = connection.getOutputStream();
      outputStream.write(body.toString().getBytes());
      outputStream.close();

      StringBuilder response = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        response.append(line);
      }
      bufferedReader.close();

      Gson gson = new Gson();
      AuthorityEntity json = gson.fromJson(response.toString(), AuthorityEntity.class);

      return json;

    } finally {
      if (connection != null)
        connection.disconnect();
    }
  }

  public GetAccountResponseEntity getAccountData(String OrganizationURI, String accessToken, String phone_number, Context context)
      throws Exception {

    logger = context.getLogger();

    HttpURLConnection connection = null;
    try {

      URL url = new URL(OrganizationURI + "api/data/v9.2/contacts?$select=contactid,fullname,telephone1&$filter="
          + URLEncoder.encode("telephone1 eq '" + phone_number + "'", StandardCharsets.UTF_8.name()));

      connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "*/*");
      connection.setRequestProperty("Connection", "keep-alive");
      connection.addRequestProperty("Authorization", "Bearer " + accessToken);

      connection.setRequestMethod("GET");
      connection.setDoOutput(true);

      StringBuilder response = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {  
        response.append(line);
      }
      bufferedReader.close();


      String prepeared_json = response.toString().replace(
        "\"@odata.context\":\"https://angeloborgesdynamics0.crm2.dynamics.com/api/data/v9.2/$metadata#contacts(contactid,fullname,telephone1)\",",
        "").replace("\"value\":[{\"@odata.etag\":\"W/\\\"4750197\\\"\",", "\"data\":{").replace("}]}", "}}");
      //logger.log("getAccountData.prepeared_json: "+ prepeared_json);
      GetAccountResponseEntity contactEntity = gson.fromJson(prepeared_json, GetAccountResponseEntity.class);
      //logger.log("getAccountData.json: "+ contactEntity.toString());
  
      return contactEntity;

    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

}