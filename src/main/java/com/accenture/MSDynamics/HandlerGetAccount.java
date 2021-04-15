package com.accenture.msdynamics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.accenture.msdynamics.entities.AuthorityEntity;
import com.accenture.msdynamics.entities.GetAccountRequestEntity;
import com.accenture.msdynamics.entities.GetAccountResponseEntity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HandlerGetAccount implements RequestHandler<Object, String> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  LambdaLogger logger = null;
  String response = null;

  @Override
  public String handleRequest(Object event, Context context) {
    logger = context.getLogger();

    /// logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
    // logger.log("CONTEXT: " + gson.toJson(context));
    // logger.log("EVENT: " + gson.toJson(event));
    logger.log("EVENT: " + event);
    // logger.log("EVENT TYPE: " + event.getClass());

    String DYNAMICS_ORG_URI = System.getenv().get("DYNAMICS_ORG_URI");
    String AZURE_TENANT_ID = System.getenv().get("AZURE_TENANT_ID");
    String CLIENT_ID = System.getenv().get("CLIENT_ID");
    String CLIENT_SECRET = System.getenv().get("CLIENT_SECRET");

    Integer inicio = event.toString().indexOf("{phone=");
    Integer fim = event.toString().length() - 25;
    String event_json = event.toString().substring(inicio, fim);
    logger.log("event_json: " + event_json);
    GetAccountRequestEntity request = gson.fromJson(event_json, GetAccountRequestEntity.class);
    logger.log("request.phone: " + request.phone);

    try {

      AuthorityEntity accessToken = authenticate(DYNAMICS_ORG_URI, AZURE_TENANT_ID, CLIENT_ID, CLIENT_SECRET, context);

      response = getAccountData(DYNAMICS_ORG_URI, accessToken.access_token, request.phone, context).replace(":", "=").replace("\"", "");
      //String response = gson.toJson(account_data);
      logger.log("response: " + response);

    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return response;
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
      AuthorityEntity auth = gson.fromJson(response.toString(), AuthorityEntity.class);
      logger.log("access_token: " + auth.access_token);

      return auth;

    } finally {
      if (connection != null)
        connection.disconnect();
    }
  }

  public String getAccountData(String OrganizationURI, String accessToken, String phone_number,
      Context context) throws Exception {

    logger = context.getLogger();

    HttpURLConnection connection = null;
    GetAccountResponseEntity contactEntity = null;
    String prepeared_json = null;
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

      //String prepeared_json = response.toString().replace("\"@odata.context\":\"https://angeloborgesdynamics0.crm2.dynamics.com/api/data/v9.2/$metadata#contacts(contactid,fullname,telephone1)\",","").replace("\"value\":[{\"@odata.etag\":\"W/\\\"4750197\\\"\",", "\"data\":{").replace("}]}", "}}");
      Integer inicio = response.toString().indexOf("\"contactid\"");
      Integer fim = response.toString().length() - 2;
      prepeared_json = response.toString().substring(inicio, fim);
      prepeared_json = "{\"data\":{" + prepeared_json + "}";
      logger.log("getAccountData.prepeared_json: " + prepeared_json);
      //contactEntity = gson.fromJson(gson.toJson(prepeared_json), GetAccountResponseEntity.class);
      //logger.log("contactEntity.data.fullname: " + contactEntity.data.fullname);

    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return prepeared_json;
  }

}